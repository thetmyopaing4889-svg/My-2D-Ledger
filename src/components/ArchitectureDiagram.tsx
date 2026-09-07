import React, { useState } from 'react';
import { Shield, Cpu, Network, ArrowRight, Layers, Lock, Smartphone, Terminal, Server } from 'lucide-react';

interface StageNode {
  id: string;
  title: string;
  subtitle: string;
  icon: React.ComponentType<{ className?: string }>;
  tag: string;
  color: string;
  details: {
    layer: string;
    throughput: string;
    ipcMethod: string;
    notes: string;
    codeSnippet: string;
  };
}

const STAGES: StageNode[] = [
  {
    id: 'device',
    title: 'Android Userland',
    subtitle: 'Apps & Browser Traffic',
    icon: Smartphone,
    tag: 'OS Layer',
    color: 'border-cyan-500/40 text-cyan-400 bg-cyan-950/20',
    details: {
      layer: 'Android OS Application Space (UIDs)',
      throughput: 'Line rate (~1 Gbps theoretical)',
      ipcMethod: 'Kernel IP stack routing table into tun0',
      notes: 'All device apps are intercepted except AegisVPN itself (disallowed self-package to avoid packet loop).',
      codeSnippet: `builder.addRoute("0.0.0.0", 0)\nbuilder.addDisallowedApplication(packageName)`
    }
  },
  {
    id: 'tun',
    title: 'VpnService TUN',
    subtitle: 'Virtual Interface tun0',
    icon: Network,
    tag: 'Kernel TUN',
    color: 'border-emerald-500/40 text-emerald-400 bg-emerald-950/20',
    details: {
      layer: 'Linux TUN Driver (tun.ko)',
      throughput: 'Zero-copy raw IP packets via ParcelFileDescriptor',
      ipcMethod: 'Raw file descriptor int tunFd passed directly via JNI',
      notes: 'Allocates MTU 1500, IPv4 (172.19.0.1/30) and IPv6 (fdfe:dcba:9876::1/126) with DNS redirection.',
      codeSnippet: `ParcelFileDescriptor tun = builder.establish();\nint fd = tun.getFd(); // e.g., 42`
    }
  },
  {
    id: 'hev',
    title: 'hev-socks5-tunnel',
    subtitle: 'Native C / JNI Engine',
    icon: Cpu,
    tag: 'C11 / epoll',
    color: 'border-amber-500/40 text-amber-400 bg-amber-950/20',
    details: {
      layer: 'Native NDK Shared Library (libhev-socks5-tunnel.so)',
      throughput: 'Ultra-low latency, coroutine-based epoll event loop',
      ipcMethod: 'TCP/UDP SOCKS5 client over 127.0.0.1:10808',
      notes: 'Translates raw Layer 3 IP packets (TCP SYN, UDP datagrams, ICMP) into Layer 5 SOCKS5 commands. Minimal CPU and battery consumption.',
      codeSnippet: `HevTunnel.init(yamlPath, fd);\nHevTunnel.start(); // Spawns POSIX pthread`
    }
  },
  {
    id: 'socks',
    title: 'Loopback Bridge',
    subtitle: '127.0.0.1:10808',
    icon: Lock,
    tag: 'Localhost IPC',
    color: 'border-blue-500/40 text-blue-400 bg-blue-950/20',
    details: {
      layer: 'Linux Loopback (lo)',
      throughput: 'In-memory socket buffers',
      ipcMethod: 'SOCKS5 protocol with UDP ASSOCIATE support',
      notes: 'Zero external network exposure. Bound strictly to 127.0.0.1. Network security config allows cleartext exclusively on loopback.',
      codeSnippet: `socks:\n  port: 10808\n  address: 127.0.0.1\n  udp: 'udp'`
    }
  },
  {
    id: 'daemon',
    title: 'Active Core Daemon',
    subtitle: 'Spawned Subprocess',
    icon: Server,
    tag: 'Process Manager',
    color: 'border-purple-500/40 text-purple-400 bg-purple-950/20',
    details: {
      layer: 'Isolated Native Executable (Xray / Hysteria2 / AmneziaWG / TUIC / Naive)',
      throughput: 'Protocol-dependent (QUIC / TLS-in-TLS / WireGuard)',
      ipcMethod: 'Runtime fork/execve with isolated unix pipes',
      notes: 'Subprocess is dynamically extracted to app nativeLibraryDir, executed with generated config JSON/YAML, and terminated smoothly when protocol switches.',
      codeSnippet: `ProcessBuilder(coreBinPath, "-config", configPath).start()`
    }
  },
  {
    id: 'remote',
    title: 'Target Server',
    subtitle: 'Censorship-Bypassed',
    icon: Shield,
    tag: 'Anti-DPI Node',
    color: 'border-rose-500/40 text-rose-400 bg-rose-950/20',
    details: {
      layer: 'Remote Server (VPS / Cloudflare / Relay)',
      throughput: 'Uncensored Wide-Area Connection',
      ipcMethod: 'Encrypted & Camouflaged (uTLS, REALITY, Salamander, Cronet)',
      notes: 'Deep Packet Inspection (DPI) sees only legitimate HTTPS, Apple/Microsoft SNI, or random UDP noise.',
      codeSnippet: `REALITY SNI: gateway.icloud.com\nObfs: Salamander (AES-128-CTR pseudo-random)`
    }
  }
];

export const ArchitectureDiagram: React.FC = () => {
  const [selectedStage, setSelectedStage] = useState<StageNode>(STAGES[2]);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-2">
            <Layers className="w-3.5 h-3.5" />
            <span>Modular Daemon + tun2socks Architecture</span>
          </div>
          <h2 className="text-xl font-bold text-white tracking-tight">Zero-Collision Network Pipeline</h2>
          <p className="text-sm text-slate-400">
            Click any layer in the data pipeline to inspect its system calls, IPC bridge, and kernel parameters.
          </p>
        </div>
        <div className="text-xs text-slate-400 bg-slate-800/80 px-3 py-2 rounded-lg border border-slate-700/50">
          Target ABI: <span className="text-cyan-400 font-mono font-medium">arm64-v8a</span> / <span className="text-cyan-400 font-mono font-medium">armeabi-v7a</span> / <span className="text-cyan-400 font-mono font-medium">x86_64</span>
        </div>
      </div>

      {/* Pipeline Nodes Flow */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        {STAGES.map((stage, idx) => {
          const Icon = stage.icon;
          const isSelected = selectedStage.id === stage.id;
          return (
            <button
              key={stage.id}
              onClick={() => setSelectedStage(stage)}
              className={`text-left p-3.5 rounded-xl border transition-all relative group flex flex-col justify-between ${
                isSelected
                  ? `${stage.color} ring-2 ring-cyan-500/30 shadow-lg`
                  : 'bg-slate-800/60 border-slate-700/60 hover:border-slate-600 hover:bg-slate-800 text-slate-300'
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className={`p-2 rounded-lg ${isSelected ? 'bg-slate-900/60' : 'bg-slate-700/40'}`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <span className="text-[10px] font-mono uppercase tracking-wider text-slate-400 px-1.5 py-0.5 rounded bg-slate-800/90 border border-slate-700/50">
                    {stage.tag}
                  </span>
                </div>
                <div className="text-xs font-bold text-white leading-tight">{stage.title}</div>
                <div className="text-[11px] text-slate-400 truncate mt-0.5">{stage.subtitle}</div>
              </div>

              {idx < STAGES.length - 1 && (
                <div className="hidden lg:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-slate-500 group-hover:text-slate-300">
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              )}
            </button>
          );
        })}
      </div>

      {/* Stage Detail Drawer */}
      <div className="bg-slate-950/80 rounded-lg p-5 border border-slate-800 grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-cyan-400 uppercase tracking-wide bg-cyan-950/40 border border-cyan-800/40 px-2 py-0.5 rounded">
              Selected Stage: {selectedStage.id.toUpperCase()}
            </span>
            <h3 className="text-base font-semibold text-white">{selectedStage.title} ({selectedStage.subtitle})</h3>
          </div>

          <p className="text-sm text-slate-300 leading-relaxed">
            {selectedStage.details.notes}
          </p>

          <div className="grid grid-cols-2 gap-3 pt-2">
            <div className="bg-slate-900/90 p-2.5 rounded border border-slate-800">
              <span className="text-[11px] uppercase tracking-wider font-mono text-slate-500 block">Subsystem Layer</span>
              <span className="text-xs font-medium text-slate-200">{selectedStage.details.layer}</span>
            </div>
            <div className="bg-slate-900/90 p-2.5 rounded border border-slate-800">
              <span className="text-[11px] uppercase tracking-wider font-mono text-slate-500 block">Throughput / Latency</span>
              <span className="text-xs font-medium text-emerald-400">{selectedStage.details.throughput}</span>
            </div>
            <div className="bg-slate-900/90 p-2.5 rounded border border-slate-800 col-span-2">
              <span className="text-[11px] uppercase tracking-wider font-mono text-slate-500 block">Bridge IPC Mechanism</span>
              <span className="text-xs font-mono text-cyan-300">{selectedStage.details.ipcMethod}</span>
            </div>
          </div>
        </div>

        <div className="lg:col-span-5 flex flex-col justify-between bg-slate-900 p-3.5 rounded-lg border border-slate-800 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800 text-slate-400">
            <span className="flex items-center gap-1.5 text-xs text-slate-300 font-semibold">
              <Terminal className="w-3.5 h-3.5 text-cyan-400" />
              Native Invocation
            </span>
            <span className="text-[10px] text-slate-500">JNI / Kernel Binding</span>
          </div>
          <pre className="text-[12px] text-cyan-200/90 overflow-x-auto py-3 leading-relaxed whitespace-pre-wrap">
            {selectedStage.details.codeSnippet}
          </pre>
          <div className="text-[11px] text-slate-400 bg-slate-950/60 p-2 rounded border border-slate-800/80">
            ✓ Complete thread-safe lifecycle control via <code className="text-white">pthread_mutex</code> & <code className="text-white">ParcelFileDescriptor</code>
          </div>
        </div>
      </div>
    </div>
  );
};
