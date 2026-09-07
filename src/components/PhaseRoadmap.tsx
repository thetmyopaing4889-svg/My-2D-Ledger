import React from 'react';
import { CheckCircle2, CircleDot, ArrowRight, Shield, Terminal, Cpu, FileCode2, Smartphone } from 'lucide-react';

export const PhaseRoadmap: React.FC = () => {
  const phases = [
    {
      number: 'PHASE 1',
      title: 'Architecture, Gradle KTS & Manifest',
      status: 'DELIVERED',
      icon: FileCode2,
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      items: [
        'Multi-arch Gradle KTS (arm64-v8a, armeabi-v7a, x86_64) with ABI splits',
        'CMakeLists.txt with Clang C11, -O3, GNU extensions & system log links',
        'AndroidManifest.xml with isolated :vpn_core process & Android 14 foreground permissions',
        'JNI Bridge C skeleton and Kotlin HevTunnel / AegisVpnService class contracts'
      ]
    },
    {
      number: 'PHASE 2',
      title: 'JNI / C hev-socks5-tunnel Integration',
      status: 'DELIVERED',
      icon: Cpu,
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      items: [
        'hev-socks5-tunnel C11 coroutine epoll event loop implementation',
        'Zero-copy TUN fd handover and thread synchronization',
        'Live stats polling (TX/RX throughput in HevTunnel.kt StateFlow)',
        'YAML config file writer & validation model (HevTunnelConfig.kt)'
      ]
    },
    {
      number: 'PHASE 3',
      title: 'ProcessManager.kt & DaemonService.kt',
      status: 'DELIVERED',
      icon: Terminal,
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      items: [
        'BinaryManager: APK asset extraction with ABI matching and chmod 0755 permissions',
        'ProcessManager: Subprocess lifecycle (ProcessBuilder, log piping, SIGTERM -> SIGKILL)',
        'ProcessHealthMonitor: SOCKS5 127.0.0.1:10808 handshake verification and latency telemetry',
        'DaemonService: Background ForegroundService coordinating cores, port allocation & auto-recovery'
      ]
    },
    {
      number: 'PHASE 4',
      title: 'Config Parsers & URI Decoders',
      status: 'DELIVERED',
      icon: Shield,
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      items: [
        'UriParser.kt: share link parsing for vless, hysteria2, naive, tuic, amneziawg & conf',
        'XrayConfigGenerator.kt: Reality, XHTTP, gRPC, WS JSON builder with socks 10808 inbound',
        'HysteriaConfigGenerator.kt: YAML builder with Salamander obfs & brutal congestion',
        'Naive, Tuic & Amnezia generators preserving junk params (Jc, Jmin, S1, H1..H4)'
      ]
    },
    {
      number: 'PHASE 5',
      title: 'Jetpack Compose UI & Latency Engine',
      status: 'DELIVERED',
      icon: Smartphone,
      badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
      items: [
        'PingLatencyTester.kt: Direct TCP handshake RTT & HTTP 204 connectivity tester',
        'ServerViewModel.kt: StateFlow management for connection lifecycle & speeds',
        'MainScreen.kt: Pulsing glow connect button, speed meters & throughput graph',
        'ServerListScreen.kt & LogViewerScreen.kt: Server cards, batch ping & terminal',
        'MainActivity.kt: VpnService.prepare() launcher & system notification channel'
      ]
    }
  ];

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Lead Engineer Delivery Roadmap</h2>
          <p className="text-xs text-slate-400">
            Step-by-step modular implementation plan for the complete production Android VPN client.
          </p>
        </div>
        <span className="text-xs font-mono text-emerald-400 bg-emerald-950/60 px-3 py-1.5 rounded-lg border border-emerald-800/60">
          All 5 Phases Delivered • Full Android Client Complete
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3 pt-2">
        {phases.map((phase, idx) => {
          const Icon = phase.icon;
          const isDelivered = phase.status === 'DELIVERED';
          const isNext = phase.status === 'NEXT';

          return (
            <div
              key={phase.number}
              className={`p-4 rounded-xl border flex flex-col justify-between transition-all ${
                isDelivered
                  ? 'bg-emerald-950/15 border-emerald-500/50 shadow-md ring-1 ring-emerald-500/20'
                  : isNext
                  ? 'bg-cyan-950/15 border-cyan-500/50 ring-1 ring-cyan-500/20'
                  : 'bg-slate-950/40 border-slate-800/80 text-slate-400'
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] font-mono font-bold text-slate-400">{phase.number}</span>
                  <span className={`text-[9px] font-mono uppercase px-2 py-0.5 rounded-full border font-semibold ${phase.badgeColor}`}>
                    {phase.status}
                  </span>
                </div>
                <div className="flex items-center gap-2 mb-2">
                  <Icon className={`w-4 h-4 ${isDelivered ? 'text-emerald-400' : isNext ? 'text-cyan-400' : 'text-slate-500'}`} />
                  <h3 className="text-xs font-bold text-white leading-snug">{phase.title}</h3>
                </div>
                <ul className="space-y-1.5 text-[11px] text-slate-400 mt-2">
                  {phase.items.map((it, i) => (
                    <li key={i} className="flex items-start gap-1.5">
                      <span className="text-slate-600 mt-0.5">•</span>
                      <span>{it}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {idx < phases.length - 1 && (
                <div className="hidden lg:flex items-center justify-end text-slate-700 pt-3">
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
