package org.anticensor.vpn.core.process

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages the lifecycle of standalone anti-censorship daemons (Xray, Hysteria 2, NaiveProxy, etc.)
 * running as child processes within the Android :vpn_core process.
 *
 * Features:
 * - Single-active daemon enforcement (clean teardown before switching)
 * - Two-phase shutdown: Graceful SIGTERM with timeout escalation to SIGKILL
 * - Asynchronous stdout & stderr log piping with ring buffering
 * - Crash detection and exponential backoff restart policy
 */
class ProcessManager(private val context: Context) {

    companion object {
        private const val TAG = "ProcessManager"
        private const val GRACEFUL_STOP_TIMEOUT_MS = 2500L
        private const val MAX_CRASH_RESTARTS = 3
        private const val CRASH_WINDOW_MS = 30000L
        private const val LOG_BUFFER_CAPACITY = 250
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State & Logs
    private val _daemonState = MutableStateFlow<DaemonState>(DaemonState.Idle)
    val daemonState: StateFlow<DaemonState> = _daemonState.asStateFlow()

    private val _logFlow = MutableSharedFlow<ProcessLogEntry>(replay = 50, extraBufferCapacity = 100)
    val logFlow: SharedFlow<ProcessLogEntry> = _logFlow.asSharedFlow()

    private val logHistory = ArrayDeque<ProcessLogEntry>(LOG_BUFFER_CAPACITY)

    // Running process handles
    private var currentProcess: Process? = null
    private var currentCore: CoreBinary? = null
    private var currentPid: Long = -1
    private var currentConfigPath: String? = null
    private var launchTimestamp: Long = 0

    // Restart & Recovery tracking
    private val restartCount = AtomicInteger(0)
    private var firstCrashTimestamp: Long = 0
    private val isExplicitShutdown = AtomicBoolean(false)

    // Coroutine log jobs
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null
    private var monitorJob: Job? = null

    /**
     * Starts a core daemon with the given configuration file.
     * Enforces single-active-daemon policy: cleanly shuts down any existing process first.
     */
    @Synchronized
    suspend fun startDaemon(
        core: CoreBinary,
        executableFile: File,
        configFile: File,
        extraArgs: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        // Enforce single-active daemon policy: terminate any running daemon
        if (currentProcess != null && isProcessAlive(currentProcess)) {
            Log.w(TAG, "Another daemon is already active (${currentCore?.executableName}). Initiating teardown...")
            stopDaemon()
        }

        isExplicitShutdown.set(false)
        _daemonState.value = DaemonState.Starting(core.executableName, configFile.absolutePath)
        currentCore = core
        currentConfigPath = configFile.absolutePath

        if (!executableFile.canExecute()) {
            val err = "Executable ${executableFile.absolutePath} lacks execution permission"
            Log.e(TAG, err)
            _daemonState.value = DaemonState.Crashed(core.executableName, -1, err, restartCount.get())
            return@withContext false
        }

        try {
            val commandList = buildCommandLine(core, executableFile, configFile, extraArgs)
            Log.i(TAG, "Launching daemon: ${commandList.joinToString(" ")}")

            val processBuilder = ProcessBuilder(commandList).apply {
                directory(context.filesDir)

                // Configure environment variables
                val env = environment()
                env["HOME"] = context.filesDir.absolutePath
                env["TMPDIR"] = context.cacheDir.absolutePath
                env["XRAY_LOCATION_ASSET"] = context.filesDir.absolutePath
                env["LOG_LEVEL"] = "warn"
            }

            val process = processBuilder.start()
            currentProcess = process
            currentPid = resolveProcessPid(process)
            launchTimestamp = System.currentTimeMillis()

            Log.i(TAG, "Daemon ${core.executableName} started successfully with PID: $currentPid")
            _daemonState.value = DaemonState.Running(
                binaryName = core.executableName,
                pid = currentPid,
                socks5Port = core.defaultSocks5Port,
                startTimeMs = launchTimestamp
            )

            // Start stdout / stderr draining coroutines
            startLogDrainers(process)

            // Start health & crash monitor
            startExitMonitor(core, process)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start daemon ${core.executableName}", e)
            _daemonState.value = DaemonState.Crashed(
                binaryName = core.executableName,
                exitCode = -1,
                errorMessage = e.message ?: "Process spawn error",
                restartCount = restartCount.get()
            )
            false
        }
    }

    /**
     * Builds the specific CLI invocation arguments for each protocol engine.
     */
    private fun buildCommandLine(
        core: CoreBinary,
        executable: File,
        configFile: File,
        extraArgs: List<String>
    ): List<String> {
        val args = mutableListOf(executable.absolutePath)
        when (core) {
            CoreBinary.XRAY -> {
                args.add("run")
                args.add("-c")
                args.add(configFile.absolutePath)
            }
            CoreBinary.HYSTERIA2 -> {
                args.add("client")
                args.add("-c")
                args.add(configFile.absolutePath)
            }
            CoreBinary.NAIVE -> {
                args.add(configFile.absolutePath)
            }
            CoreBinary.AMNEZIA_WG -> {
                args.add("-c")
                args.add(configFile.absolutePath)
            }
            CoreBinary.TUIC -> {
                args.add("-c")
                args.add(configFile.absolutePath)
            }
        }
        args.addAll(extraArgs)
        return args
    }

    /**
     * Gracefully stops the running daemon using two-phase shutdown:
     * Phase 1: SIGTERM via process.destroy()
     * Phase 2: If still alive after timeout, escalate to SIGKILL via process.destroyForcibly()
     */
    @Synchronized
    suspend fun stopDaemon(): Boolean = withContext(Dispatchers.IO) {
        val process = currentProcess ?: return@withContext true
        val core = currentCore ?: CoreBinary.XRAY
        val pid = currentPid

        isExplicitShutdown.set(true)
        Log.i(TAG, "Initiating graceful shutdown for daemon ${core.executableName} (PID $pid)...")
        _daemonState.value = DaemonState.Stopping(core.executableName, pid, isEscalatedToKill = false)

        try {
            // Cancel log readers and exit monitors
            monitorJob?.cancel()

            // Phase 1: Send SIGTERM
            process.destroy()

            // Wait up to GRACEFUL_STOP_TIMEOUT_MS
            val terminatedInTime = withTimeoutOrNull(GRACEFUL_STOP_TIMEOUT_MS) {
                while (isProcessAlive(process)) {
                    delay(50)
                }
                true
            } ?: false

            // Phase 2: Escalate to SIGKILL if still alive
            if (!terminatedInTime && isProcessAlive(process)) {
                Log.w(TAG, "Daemon did not terminate within timeout; escalating to SIGKILL (destroyForcibly)")
                _daemonState.value = DaemonState.Stopping(core.executableName, pid, isEscalatedToKill = true)
                process.destroyForcibly()

                // Native kill fallback if supported
                if (pid > 0) {
                    try {
                        Runtime.getRuntime().exec(arrayOf("kill", "-9", pid.toString())).waitFor()
                    } catch (e: Exception) {
                        Log.w(TAG, "Native kill -9 execution fallback: ${e.message}")
                    }
                }

                // Final brief wait
                withTimeoutOrNull(1000) {
                    while (isProcessAlive(process)) {
                        delay(50)
                    }
                }
            }

            val exitCode = try { process.exitValue() } catch (_: Exception) { 0 }
            val duration = System.currentTimeMillis() - launchTimestamp
            Log.i(TAG, "Daemon ${core.executableName} terminated with exit code: $exitCode (ran for ${duration}ms)")

            _daemonState.value = DaemonState.Terminated(core.executableName, exitCode, duration)
            currentProcess = null
            currentPid = -1
            restartCount.set(0)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping daemon process", e)
            _daemonState.value = DaemonState.Idle
            currentProcess = null
            currentPid = -1
            false
        }
    }

    /**
     * Starts asynchronous coroutine readers for stdout and stderr streams.
     */
    private fun startLogDrainers(process: Process) {
        stdoutJob?.cancel()
        stderrJob?.cancel()

        stdoutJob = scope.launch {
            drainStream(process.inputStream, isStderr = false)
        }

        stderrJob = scope.launch {
            drainStream(process.errorStream, isStderr = true)
        }
    }

    private suspend fun drainStream(inputStream: InputStream, isStderr: Boolean) = withContext(Dispatchers.IO) {
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val msg = line ?: continue
                    val entry = ProcessLogEntry(isStderr = isStderr, message = msg)
                    synchronized(logHistory) {
                        if (logHistory.size >= LOG_BUFFER_CAPACITY) {
                            logHistory.removeFirst()
                        }
                        logHistory.addLast(entry)
                    }
                    _logFlow.emit(entry)

                    if (isStderr) {
                        Log.w(TAG, "[${currentCore?.executableName} STDERR] $msg")
                    } else {
                        Log.d(TAG, "[${currentCore?.executableName} STDOUT] $msg")
                    }
                }
            }
        } catch (_: Exception) {
            // Stream closed upon process termination
        }
    }

    /**
     * Monitors process exit. If the process dies unexpectedly, triggers crash recovery.
     */
    private fun startExitMonitor(core: CoreBinary, process: Process) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            try {
                // Wait for process to exit
                val exitCode = withContext(Dispatchers.IO) {
                    process.waitFor()
                }

                if (!isExplicitShutdown.get()) {
                    Log.e(TAG, "Daemon ${core.executableName} died unexpectedly with exit code: $exitCode")
                    handleUnexpectedCrash(core, exitCode)
                }
            } catch (_: CancellationException) {
                // Normal cancellation on intentional stop
            }
        }
    }

    /**
     * Handles unexpected crash with crash-loop throttling.
     */
    private suspend fun handleUnexpectedCrash(core: CoreBinary, exitCode: Int) {
        val now = System.currentTimeMillis()
        if (now - firstCrashTimestamp > CRASH_WINDOW_MS) {
            // Reset window
            firstCrashTimestamp = now
            restartCount.set(1)
        } else {
            restartCount.incrementAndGet()
        }

        val crashes = restartCount.get()
        if (crashes > MAX_CRASH_RESTARTS) {
            Log.e(TAG, "Crash loop detected! Exceeded $MAX_CRASH_RESTARTS crashes in ${CRASH_WINDOW_MS / 1000}s. Halting auto-restart.")
            _daemonState.value = DaemonState.Crashed(
                binaryName = core.executableName,
                exitCode = exitCode,
                errorMessage = "Exceeded max restart attempts ($MAX_CRASH_RESTARTS) due to recurring crashes",
                restartCount = crashes
            )
            return
        }

        _daemonState.value = DaemonState.Crashed(
            binaryName = core.executableName,
            exitCode = exitCode,
            errorMessage = "Unexpected exit with code $exitCode. Attempting restart ($crashes/$MAX_CRASH_RESTARTS)...",
            restartCount = crashes
        )

        // Exponential backoff before restart: 1s, 2s, 4s...
        val backoffMs = (1000L * (1 shl (crashes - 1))).coerceAtMost(6000L)
        Log.i(TAG, "Backing off for ${backoffMs}ms before auto-restarting ${core.executableName}...")
        delay(backoffMs)

        val configFile = currentConfigPath?.let { File(it) }
        val binaryManager = BinaryManager(context)
        val executable = binaryManager.getExecutableFile(core)

        if (configFile != null && configFile.exists() && executable.exists()) {
            Log.i(TAG, "Auto-recovering daemon ${core.executableName} (attempt $crashes)...")
            startDaemon(core, executable, configFile)
        }
    }

    /**
     * Resolves the Linux PID of the child process.
     */
    private fun resolveProcessPid(process: Process): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return process.pid()
            } catch (_: Exception) {}
        }
        // Fallback reflection for UNIXProcess / ProcessImpl
        try {
            val field: Field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            return field.getLong(process)
        } catch (_: Exception) {}
        return -1
    }

    private fun isProcessAlive(process: Process?): Boolean {
        if (process == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            process.isAlive
        } else {
            try {
                process.exitValue()
                false
            } catch (_: IllegalThreadStateException) {
                true
            }
        }
    }

    /**
     * Returns a snapshot copy of recent log entries.
     */
    fun getLogHistory(): List<ProcessLogEntry> {
        synchronized(logHistory) {
            return logHistory.toList()
        }
    }

    fun release() {
        scope.launch {
            stopDaemon()
            scope.cancel()
        }
    }
}
