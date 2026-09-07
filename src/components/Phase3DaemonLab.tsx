import React, { useState, useEffect, useRef } from 'react';
import {
  Terminal,
  Cpu,
  Zap,
  Play,
  Square,
  AlertTriangle,
  RotateCcw,
  ShieldCheck,
  Activity,
  HardDrive,
  Clock,
  ArrowRightLeft,
  CheckCircle2,
  FileCode,
  Download,
  Trash2,
  Flame,
  Radio
} from 'lucide-react';

interface DaemonSimulationState {
  core: 'xray' | 'hysteria2' | 'naive' | 'amneziawg' | 'tuic';
  status: 'IDLE' | 'EXTRACTING' | 'STARTING' | 'RUNNING' | 'STOPPING_SIGTERM' | 'STOPPING_SIGKILL' | 'CRASHED' | 'TERMINATED';
  pid: number | null;
  socks5Port: number;
  uptimeSeconds: number;
  cpuPercent: number;
  memoryMb: number;
  restartCount: number;
  exitCode: number | null;
  lastError: string | null;
}

interface BinaryFileMeta {
  name: string;
  coreKey: 'xray' | 'hysteria2' | 'naive' | 'amneziawg' | 'tuic';
  version: string;
  sizeBytes: number;
  abi: string;
  extracted: boolean;
  permissions: string;
  protocols: string[];
}

const INITIAL_BINARIES: BinaryFileMeta[] = [
  {
    name: 'xray',
    coreKey: 'xray',
    version: 'v1.8.24 (XTLS-Reality)',
    sizeBytes: 24890112, // ~24MB
    abi: 'arm64-v8a',
    extracted: true,
    permissions: '-rwxr-xr-x (0755)',
    protocols: ['VLESS-Reality', 'VMess-AEAD', 'Trojan', 'Shadowsocks-2022']
  },
  {
    name: 'hysteria',
    coreKey: 'hysteria2',
    version: 'v2.5.2 (Salamander)',
    sizeBytes: 18456200, // ~18MB
    abi: 'arm64-v8a',
    extracted: true,
    permissions: '-rwxr-xr-x (0755)',
    protocols: ['Hysteria 2 / QUIC', 'Salamander Obfuscation']
  },
  {
    name: 'naive',
    coreKey: 'naive',
    version: 'v124.0.6367.60 (Cronet)',
    sizeBytes: 15204352, // ~15MB
    abi: 'arm64-v8a',
    extracted: true,
    permissions: '-rwxr-xr-x (0755)',
    protocols: ['NaïveProxy HTTP/2', 'NaïveProxy HTTP/3 QUIC']
  },
  {
    name: 'amneziawg-go',
    coreKey: 'amneziawg',
    version: 'v0.2.14 (Junk/InitPacket)',
    sizeBytes: 8912896, // ~8.5MB
    abi: 'arm64-v8a',
    extracted: false,
    permissions: '-rwxr-xr-x (0755)',
    protocols: ['AmneziaWG Obfuscated WireGuard']
  },
  {
    name: 'tuic-client',
    coreKey: 'tuic',
    version: 'v5.0.0 (BBR-0RTT)',
    sizeBytes: 6710886, // ~6.4MB
    abi: 'arm64-v8a',
    extracted: false,
    permissions: '-rwxr-xr-x (0755)',
    protocols: ['TUIC v5 / QUIC']
  }
];

export const Phase3DaemonLab: React.FC = () => {
  const [binaries, setBinaries] = useState<BinaryFileMeta[]>(INITIAL_BINARIES);
  const [extractingKey, setExtractingKey] = useState<string | null>(null);
  const [extractProgress, setExtractProgress] = useState(0);

  const [daemon, setDaemon] = useState<DaemonSimulationState>({
    core: 'xray',
    status: 'RUNNING',
    pid: 14892,
    socks5Port: 10808,
    uptimeSeconds: 142,
    cpuPercent: 1.4,
    memoryMb: 18.6,
    restartCount: 0,
    exitCode: null,
    lastError: null
  });

  const [healthStatus, setHealthStatus] = useState({
    isHealthy: true,
    latencyMs: 2.1,
    consecutiveFailures: 0,
    lastProbeTime: 'Just now'
  });

  const [logs, setLogs] = useState<{ isStderr: boolean; text: string; time: string }[]>([
    { isStderr: false, text: '[BinaryManager] Resolved target ABI: arm64-v8a', time: '10:24:01' },
    { isStderr: false, text: '[BinaryManager] Binary xray verified at /data/user/0/org.anticensor.vpn/app_bin/xray (chmod 755 OK)', time: '10:24:02' },
    { isStderr: false, text: '[ProcessManager] ProcessBuilder launching: xray run -c config_xray.json', time: '10:24:02' },
    { isStderr: false, text: '[ProcessManager] Child process spawned with PID 14892 in :vpn_core', time: '10:24:02' },
    { isStderr: false, text: '[xray STDOUT] Xray 1.8.24 (Xray, Penetrates Everything.) Custom Build', time: '10:24:03' },
    { isStderr: false, text: '[xray STDOUT] Inbound socks: 127.0.0.1:10808 (no-auth) started', time: '10:24:03' },
    { isStderr: false, text: '[ProcessHealthMonitor] SOCKS5 handshake probe OK (VER=0x05, METHOD=0x00, 2.1ms)', time: '10:24:03' },
    { isStderr: false, text: '[DaemonService] Daemon Xray Core is healthy. Ready for HevTunnel connection', time: '10:24:04' }
  ]);

  const logEndRef = useRef<HTMLDivElement>(null);

  const addLog = (text: string, isStderr = false) => {
    const time = new Date().toTimeString().split(' ')[0];
    setLogs(prev => [...prev.slice(-40), { isStderr, text, time }]);
  };

  // Live timer tick for daemon simulation
  useEffect(() => {
    const interval = setInterval(() => {
      if (daemon.status === 'RUNNING') {
        setDaemon(prev => ({
          ...prev,
          uptimeSeconds: prev.uptimeSeconds + 1,
          cpuPercent: parseFloat((0.8 + Math.random() * 1.5).toFixed(1)),
          memoryMb: parseFloat((18.2 + Math.sin(prev.uptimeSeconds / 10) * 1.2).toFixed(1))
        }));

        setHealthStatus({
          isHealthy: true,
          latencyMs: parseFloat((1.5 + Math.random() * 1.8).toFixed(1)),
          consecutiveFailures: 0,
          lastProbeTime: 'Just now'
        });

        // Random heartbeat logs
        if (Math.random() > 0.8) {
          const coreTag = daemon.core === 'xray' ? 'xray' : daemon.core === 'hysteria2' ? 'hy2' : daemon.core;
          addLog(`[${coreTag} STDOUT] Routing session to 104.244.42.1:443 via XTLS-Reality (SNI: dl.google.com)`);
        }
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [daemon.status, daemon.core]);

  // Extract Binary Simulation
  const handleExtract = (key: 'xray' | 'hysteria2' | 'naive' | 'amneziawg' | 'tuic') => {
    setExtractingKey(key);
    setExtractProgress(10);
    addLog(`[BinaryManager] Extracting APK asset bin/arm64-v8a/${key}.gz into app_bin/${key}...`);

    let cur = 10;
    const progressTimer = setInterval(() => {
      cur += 25;
      setExtractProgress(cur);
      if (cur >= 100) {
        clearInterval(progressTimer);
        setBinaries(prev =>
          prev.map(b => (b.coreKey === key ? { ...b, extracted: true } : b))
        );
        setExtractingKey(null);
        addLog(`[BinaryManager] Applied chmod 0755 to app_bin/${key}. Binary is now executable!`);
      }
    }, 250);
  };

  // Start Daemon Simulation
  const handleStartDaemon = (targetCore: 'xray' | 'hysteria2' | 'naive' | 'amneziawg' | 'tuic') => {
    const bin = binaries.find(b => b.coreKey === targetCore);
    if (!bin?.extracted) {
      addLog(`[ProcessManager] Cannot start ${targetCore}: Binary not yet extracted. Extracting first...`, true);
      handleExtract(targetCore);
      return;
    }

    addLog(`[DaemonService] Preparing launch workflow for ${bin.name}...`);
    setDaemon(prev => ({ ...prev, status: 'STARTING', core: targetCore }));

    setTimeout(() => {
      const newPid = Math.floor(10000 + Math.random() * 20000);
      setDaemon({
        core: targetCore,
        status: 'RUNNING',
        pid: newPid,
        socks5Port: 10808,
        uptimeSeconds: 0,
        cpuPercent: 1.2,
        memoryMb: targetCore === 'xray' ? 18.5 : targetCore === 'hysteria2' ? 24.2 : 16.0,
        restartCount: 0,
        exitCode: null,
        lastError: null
      });

      addLog(`[ProcessManager] ${bin.name} daemon running (PID ${newPid}) bound to 127.0.0.1:10808`);
      addLog(`[ProcessHealthMonitor] Verified SOCKS5 handshake response in 1.8ms. Health: UP`);
    }, 600);
  };

  // Graceful SIGTERM Shutdown
  const handleGracefulStop = () => {
    if (daemon.status !== 'RUNNING') return;
    const pid = daemon.pid;
    addLog(`[ProcessManager] Sending SIGTERM (process.destroy()) to PID ${pid}...`);
    setDaemon(prev => ({ ...prev, status: 'STOPPING_SIGTERM' }));

    setTimeout(() => {
      addLog(`[ProcessManager] Process ${pid} responded to SIGTERM and terminated cleanly (exitCode 0) in 320ms.`);
      setDaemon(prev => ({
        ...prev,
        status: 'TERMINATED',
        pid: null,
        uptimeSeconds: 0,
        exitCode: 0
      }));
      setHealthStatus(prev => ({ ...prev, isHealthy: false }));
    }, 450);
  };

  // Uncooperative Daemon: Escalation to SIGKILL (SIGKILL escalation test)
  const handleKillEscalation = () => {
    if (daemon.status !== 'RUNNING') return;
    const pid = daemon.pid;
    addLog(`[ProcessManager] Phase 1: Sending SIGTERM to PID ${pid} (simulating unresponsive/hung process)...`, true);
    setDaemon(prev => ({ ...prev, status: 'STOPPING_SIGTERM' }));

    // Simulate 2500ms timeout expiring without exit
    setTimeout(() => {
      addLog(`[ProcessManager] WARNING: Graceful stop timeout (2500ms) expired. Process ${pid} still alive!`, true);
      addLog(`[ProcessManager] Phase 2: ESCALATING TO SIGKILL (process.destroyForcibly() / kill -9 ${pid})`, true);
      setDaemon(prev => ({ ...prev, status: 'STOPPING_SIGKILL' }));

      setTimeout(() => {
        addLog(`[ProcessManager] Process ${pid} was forcibly terminated by SIGKILL (exitCode 137 / SIGKILL).`);
        setDaemon(prev => ({
          ...prev,
          status: 'TERMINATED',
          pid: null,
          uptimeSeconds: 0,
          exitCode: 137
        }));
        setHealthStatus(prev => ({ ...prev, isHealthy: false }));
      }, 500);
    }, 1500);
  };

  // Simulate Crash & Auto-Recovery
  const handleSimulateCrash = () => {
    if (daemon.status !== 'RUNNING') return;
    const pid = daemon.pid;
    const crashes = daemon.restartCount + 1;
    addLog(`[ProcessManager] CRITICAL: Child process ${pid} died unexpectedly with SIGSEGV (exitCode 139)!`, true);

    setDaemon(prev => ({
      ...prev,
      status: 'CRASHED',
      pid: null,
      exitCode: 139,
      lastError: 'Segmentation fault (signal 11)',
      restartCount: crashes
    }));
    setHealthStatus(prev => ({ ...prev, isHealthy: false }));

    if (crashes <= 3) {
      const backoffMs = Math.min(6000, 1000 * Math.pow(2, crashes - 1));
      addLog(`[ProcessManager] Auto-recovery policy active. Backing off ${backoffMs}ms before restart (attempt ${crashes}/3)...`);

      setTimeout(() => {
        const newPid = Math.floor(10000 + Math.random() * 20000);
        addLog(`[ProcessManager] Auto-restart SUCCESS: Spawned new ${daemon.core} instance with PID ${newPid}`);
        setDaemon(prev => ({
          ...prev,
          status: 'RUNNING',
          pid: newPid,
          uptimeSeconds: 0,
          exitCode: null,
          lastError: null
        }));
        setHealthStatus(prev => ({ ...prev, isHealthy: true }));
      }, backoffMs / 2);
    } else {
      addLog(`[ProcessManager] Circuit breaker tripped! Exceeded 3 crashes within 30s. Halting auto-restart to prevent battery drain.`, true);
    }
  };

  // Dynamic Protocol Switching Simulation
  const handleSwitchProtocol = (targetCore: 'xray' | 'hysteria2' | 'naive' | 'amneziawg' | 'tuic') => {
    if (daemon.core === targetCore && daemon.status === 'RUNNING') return;

    addLog(`[DaemonService] === INITIATING PROTOCOL SWITCH: ${daemon.core} -> ${targetCore} ===`);
    addLog(`[DaemonService] Step 1: Stopping current daemon (${daemon.core}) to liberate port 10808...`);
    setDaemon(prev => ({ ...prev, status: 'STOPPING_SIGTERM' }));

    setTimeout(() => {
      addLog(`[ProcessManager] Previous daemon PID ${daemon.pid || 14892} terminated. Port 10808 now free.`);
      addLog(`[DaemonService] Step 2: Extracting/verifying new binary (${targetCore})...`);

      setBinaries(prev =>
        prev.map(b => (b.coreKey === targetCore ? { ...b, extracted: true } : b))
      );

      setTimeout(() => {
        const newPid = Math.floor(20000 + Math.random() * 10000);
        addLog(`[DaemonService] Step 3: Launching ${targetCore} with fresh config...`);
        addLog(`[ProcessManager] Spawned ${targetCore} (PID ${newPid}) in :vpn_core`);
        addLog(`[ProcessHealthMonitor] Probing SOCKS5 127.0.0.1:10808... Handshake OK!`);

        setDaemon({
          core: targetCore,
          status: 'RUNNING',
          pid: newPid,
          socks5Port: 10808,
          uptimeSeconds: 0,
          cpuPercent: 1.1,
          memoryMb: targetCore === 'hysteria2' ? 24.5 : targetCore === 'naive' ? 17.2 : 18.0,
          restartCount: 0,
          exitCode: null,
          lastError: null
        });
        setHealthStatus({ isHealthy: true, latencyMs: 1.9, consecutiveFailures: 0, lastProbeTime: 'Just now' });
      }, 700);
    }, 500);
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border border-indigo-500/30 rounded-2xl p-6 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold uppercase bg-indigo-500/15 text-indigo-400 border border-indigo-500/30">
                Phase 3 Delivered
              </span>
              <span className="text-slate-500">•</span>
              <span className="text-xs font-mono text-slate-400">ProcessManager &amp; DaemonService</span>
            </div>
            <h2 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
              <Terminal className="w-5 h-5 text-indigo-400" />
              Subprocess Daemon &amp; Binary Extractor Lab
            </h2>
            <p className="text-xs text-slate-300 mt-1 max-w-3xl leading-relaxed">
              Standalone executable extraction (chmod 0755), single-active daemon lifecycle enforcement,
              graceful SIGTERM/SIGKILL escalation, auto-restart crash recovery, and SOCKS5 loopback health probing.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-slate-400 hidden sm:inline">Isolated Process:</span>
            <span className="px-3 py-1 bg-slate-950 rounded-lg border border-slate-800 text-xs font-mono font-bold text-cyan-400">
              :vpn_core
            </span>
          </div>
        </div>
      </div>

      {/* Active Daemon Telemetry Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {/* Status */}
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>Daemon State</span>
            <span
              className={`w-2 h-2 rounded-full ${
                daemon.status === 'RUNNING'
                  ? 'bg-emerald-400 animate-ping'
                  : daemon.status.startsWith('STOPPING')
                  ? 'bg-amber-400 animate-pulse'
                  : daemon.status === 'CRASHED'
                  ? 'bg-rose-500 animate-bounce'
                  : 'bg-slate-600'
              }`}
            />
          </div>
          <div className="text-base font-bold text-white font-mono truncate">
            {daemon.status}
          </div>
          <div className="text-[11px] text-slate-500 font-mono uppercase">
            {daemon.core} Core
          </div>
        </div>

        {/* PID & SOCKS5 */}
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>PID / SOCKS5</span>
            <Zap className="w-3.5 h-3.5 text-cyan-400" />
          </div>
          <div className="text-xl font-bold text-white font-mono">
            {daemon.pid ? `PID ${daemon.pid}` : 'None'}
          </div>
          <div className="text-[11px] text-slate-500 font-mono">
            127.0.0.1:{daemon.socks5Port}
          </div>
        </div>

        {/* Uptime & Restarts */}
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>Uptime / Restarts</span>
            <Clock className="w-3.5 h-3.5 text-indigo-400" />
          </div>
          <div className="text-xl font-bold text-white font-mono">
            {Math.floor(daemon.uptimeSeconds / 60)}m {daemon.uptimeSeconds % 60}s
          </div>
          <div className="text-[11px] text-slate-500 font-mono">
            Restarts: {daemon.restartCount}/3 Max
          </div>
        </div>

        {/* CPU & Memory */}
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>Resource Usage</span>
            <Activity className="w-3.5 h-3.5 text-purple-400" />
          </div>
          <div className="text-xl font-bold text-white font-mono">
            {daemon.status === 'RUNNING' ? `${daemon.memoryMb} MB` : '0 MB'}
          </div>
          <div className="text-[11px] text-slate-500 font-mono">
            CPU: {daemon.status === 'RUNNING' ? `${daemon.cpuPercent}%` : '0%'}
          </div>
        </div>

        {/* Health Monitor Probe */}
        <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span>Health Monitor</span>
            <ShieldCheck className={`w-3.5 h-3.5 ${healthStatus.isHealthy ? 'text-emerald-400' : 'text-slate-600'}`} />
          </div>
          <div className="text-base font-bold font-mono">
            {healthStatus.isHealthy ? (
              <span className="text-emerald-400">HANDSHAKE OK</span>
            ) : (
              <span className="text-slate-500">PROBE OFFLINE</span>
            )}
          </div>
          <div className="text-[11px] text-slate-500 font-mono">
            Latency: {healthStatus.latencyMs} ms
          </div>
        </div>
      </div>

      {/* Main Controls Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Col: Binary Manager & Protocol Switcher */}
        <div className="lg:col-span-6 space-y-6">
          {/* Binary Manager Extraction Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <HardDrive className="w-4 h-4 text-cyan-400" />
                <h3 className="text-sm font-bold text-white">BinaryManager Asset Extractor</h3>
              </div>
              <span className="text-[10px] font-mono bg-slate-800 px-2 py-0.5 rounded text-slate-400">
                app_bin/ directory
              </span>
            </div>

            <p className="text-xs text-slate-400 leading-relaxed">
              Extracts pre-compiled architecture-specific binaries from APK assets into the private app sandbox and grants execution permissions via <code className="text-cyan-300">chmod 0755</code>.
            </p>

            <div className="space-y-2.5">
              {binaries.map(bin => (
                <div
                  key={bin.coreKey}
                  className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 flex items-center justify-between gap-3 text-xs"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-white font-mono">{bin.name}</span>
                      <span className="text-[10px] font-mono text-slate-400">{bin.version}</span>
                    </div>
                    <div className="flex items-center gap-2 text-[11px] font-mono text-slate-500">
                      <span>{(bin.sizeBytes / 1048576).toFixed(1)} MB</span>
                      <span>•</span>
                      <span>{bin.abi}</span>
                      <span>•</span>
                      <span className="text-emerald-400">{bin.permissions}</span>
                    </div>
                  </div>

                  <div>
                    {bin.extracted ? (
                      <span className="flex items-center gap-1 text-[11px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded border border-emerald-500/20">
                        <CheckCircle2 className="w-3 h-3" /> Ready
                      </span>
                    ) : extractingKey === bin.coreKey ? (
                      <span className="text-[11px] font-mono text-cyan-400 bg-cyan-500/10 px-2 py-1 rounded border border-cyan-500/20">
                        Extracting {extractProgress}%
                      </span>
                    ) : (
                      <button
                        onClick={() => handleExtract(bin.coreKey)}
                        className="flex items-center gap-1 text-[11px] font-mono text-slate-200 bg-slate-800 hover:bg-slate-700 px-2.5 py-1 rounded transition-colors"
                      >
                        <Download className="w-3 h-3 text-cyan-400" /> Extract
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Dynamic Protocol Switcher & Single-Active Policy */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <ArrowRightLeft className="w-4 h-4 text-purple-400" />
                <h3 className="text-sm font-bold text-white">Dynamic Protocol Switching</h3>
              </div>
              <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                Single Active Policy
              </span>
            </div>

            <p className="text-xs text-slate-400 leading-relaxed">
              Switching between anti-censorship protocols dynamically tears down the active daemon process, liberates port 10808, and initializes the target core without leaving zombie sockets.
            </p>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <button
                onClick={() => handleSwitchProtocol('xray')}
                className={`p-2.5 rounded-lg border text-left transition-all text-xs font-mono flex flex-col gap-1 ${
                  daemon.core === 'xray' && daemon.status === 'RUNNING'
                    ? 'bg-cyan-950/60 border-cyan-500 text-cyan-300 shadow-md'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                }`}
              >
                <span className="font-bold text-white">Xray Core</span>
                <span className="text-[10px] text-slate-500">VLESS Reality</span>
              </button>

              <button
                onClick={() => handleSwitchProtocol('hysteria2')}
                className={`p-2.5 rounded-lg border text-left transition-all text-xs font-mono flex flex-col gap-1 ${
                  daemon.core === 'hysteria2' && daemon.status === 'RUNNING'
                    ? 'bg-purple-950/60 border-purple-500 text-purple-300 shadow-md'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                }`}
              >
                <span className="font-bold text-white">Hysteria 2</span>
                <span className="text-[10px] text-slate-500">Salamander QUIC</span>
              </button>

              <button
                onClick={() => handleSwitchProtocol('naive')}
                className={`p-2.5 rounded-lg border text-left transition-all text-xs font-mono flex flex-col gap-1 ${
                  daemon.core === 'naive' && daemon.status === 'RUNNING'
                    ? 'bg-blue-950/60 border-blue-500 text-blue-300 shadow-md'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                }`}
              >
                <span className="font-bold text-white">NaïveProxy</span>
                <span className="text-[10px] text-slate-500">Cronet Camouflage</span>
              </button>

              <button
                onClick={() => handleSwitchProtocol('amneziawg')}
                className={`p-2.5 rounded-lg border text-left transition-all text-xs font-mono flex flex-col gap-1 ${
                  daemon.core === 'amneziawg' && daemon.status === 'RUNNING'
                    ? 'bg-emerald-950/60 border-emerald-500 text-emerald-300 shadow-md'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                }`}
              >
                <span className="font-bold text-white">AmneziaWG</span>
                <span className="text-[10px] text-slate-500">Junk Packet Obf</span>
              </button>

              <button
                onClick={() => handleSwitchProtocol('tuic')}
                className={`p-2.5 rounded-lg border text-left transition-all text-xs font-mono flex flex-col gap-1 ${
                  daemon.core === 'tuic' && daemon.status === 'RUNNING'
                    ? 'bg-amber-950/60 border-amber-500 text-amber-300 shadow-md'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                }`}
              >
                <span className="font-bold text-white">TUIC v5</span>
                <span className="text-[10px] text-slate-500">0-RTT QUIC</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right Col: Process Lifecycle Actions & Live Terminal */}
        <div className="lg:col-span-6 space-y-6">
          {/* Lifecycle Actions Bar */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-amber-400" />
                <h3 className="text-sm font-bold text-white">ProcessManager Stress &amp; Control Tests</h3>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              {daemon.status === 'RUNNING' ? (
                <button
                  onClick={handleGracefulStop}
                  className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-lg bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/30 font-mono font-bold transition-all"
                >
                  <Square className="w-3.5 h-3.5" />
                  Graceful SIGTERM
                </button>
              ) : (
                <button
                  onClick={() => handleStartDaemon(daemon.core)}
                  className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-mono font-bold transition-all shadow-md"
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  Spawn {daemon.core}
                </button>
              )}

              <button
                onClick={handleKillEscalation}
                disabled={daemon.status !== 'RUNNING'}
                className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-lg bg-rose-600/20 hover:bg-rose-600/30 text-rose-300 border border-rose-500/30 font-mono font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <Flame className="w-3.5 h-3.5 text-rose-400" />
                Force SIGKILL Escalation
              </button>

              <button
                onClick={handleSimulateCrash}
                disabled={daemon.status !== 'RUNNING'}
                className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-lg bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 border border-purple-500/30 font-mono font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed col-span-2"
              >
                <AlertTriangle className="w-3.5 h-3.5 text-purple-400" />
                Simulate Unexpected Crash (SIGSEGV / Backoff Auto-Restart)
              </button>
            </div>
          </div>

          {/* Live Terminal Log Stream */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden shadow-xl flex flex-col">
            <div className="px-4 py-2.5 bg-slate-900 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-mono text-slate-300">
                <Terminal className="w-3.5 h-3.5 text-indigo-400" />
                <span className="font-semibold text-white">:vpn_core ProcessBuilder Stream</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                  RingBuffer: {logs.length}/250
                </span>
                <button
                  onClick={() => setLogs([])}
                  className="text-slate-500 hover:text-slate-300 text-xs transition-colors"
                  title="Clear console"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className="p-3 font-mono text-[11px] text-slate-300 space-y-1 max-h-[320px] overflow-y-auto bg-slate-950 select-text leading-relaxed">
              {logs.map((log, i) => (
                <div key={i} className="flex items-start gap-2">
                  <span className="text-slate-600 select-none text-[10px] font-mono">{log.time}</span>
                  <span className={log.isStderr ? 'text-rose-400' : 'text-slate-300'}>
                    {log.text}
                  </span>
                </div>
              ))}
              <div ref={logEndRef} />
            </div>
          </div>
        </div>
      </div>

      {/* Architecture Execution Flow Card */}
      <div className="p-5 rounded-xl bg-slate-900/60 border border-slate-800 space-y-3">
        <h3 className="text-sm font-bold text-white flex items-center gap-2">
          <Terminal className="w-4 h-4 text-indigo-400" />
          Subprocess Orchestration Architecture in :vpn_core
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 space-y-1">
            <div className="text-[10px] font-mono text-cyan-400 font-bold uppercase">1. BinaryManager</div>
            <p className="text-slate-400 text-[11px]">
              Extracts architecture-specific ELF binaries to <code className="text-cyan-300">context.getDir("bin")</code>, issues <code className="text-slate-300">chmod 0755</code>, and performs size/checksum verification.
            </p>
          </div>

          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 space-y-1">
            <div className="text-[10px] font-mono text-purple-400 font-bold uppercase">2. ProcessManager</div>
            <p className="text-slate-400 text-[11px]">
              Spawns standalone executable via <code className="text-purple-300">ProcessBuilder</code> with custom environment flags. Non-blocking coroutines drain stdout/stderr into ring buffer.
            </p>
          </div>

          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 space-y-1">
            <div className="text-[10px] font-mono text-emerald-400 font-bold uppercase">3. Health &amp; SOCKS5 Probe</div>
            <p className="text-slate-400 text-[11px]">
              <code className="text-emerald-300">ProcessHealthMonitor</code> probes <code className="text-slate-300">127.0.0.1:10808</code> with SOCKS5 handshake (0x05 0x01 0x00) ensuring listener is responding before tunnel connect.
            </p>
          </div>

          <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80 space-y-1">
            <div className="text-[10px] font-mono text-rose-400 font-bold uppercase">4. Two-Phase Teardown</div>
            <p className="text-slate-400 text-[11px]">
              Enforces single-active daemon. When stopping or switching, sends <code className="text-amber-300">SIGTERM</code>; if alive after 2500ms, escalates to <code className="text-rose-400">SIGKILL</code> (destroyForcibly).
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
