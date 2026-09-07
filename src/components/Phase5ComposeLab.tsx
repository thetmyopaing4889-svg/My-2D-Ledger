import React, { useState, useEffect, useRef } from 'react';
import { CyberHackerVisual } from './CyberHackerVisual';
import { QuantumPowerButton } from './QuantumPowerButton';
import {
  Shield,
  Server,
  Terminal,
  Power,
  ArrowDown,
  ArrowUp,
  Activity,
  Plus,
  RefreshCw,
  Trash2,
  Copy,
  Check,
  Zap,
  Sliders,
  Play,
  RotateCcw,
  Smartphone,
  ChevronRight,
  ExternalLink,
  Layers,
  Lock,
  Wifi,
  Share2,
  Split,
  Globe,
  AlertTriangle,
  CheckCircle2,
  Radio,
  Search
} from 'lucide-react';

interface ServerProfile {
  id: string;
  name: string;
  host: string;
  port: number;
  protocol: 'VLESS' | 'HY2' | 'NAIVE' | 'TUIC' | 'AWG' | 'WARP' | 'SSH' | 'SS-CLOAK' | 'SS';
  latency: number;
  camouflage: string;
  isBuiltIn?: boolean;
}

interface LogEntry {
  id: string;
  time: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  tag: string;
  message: string;
}

export const Phase5ComposeLab: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'servers' | 'routing' | 'hotspot' | 'terminal'>('dashboard');

  // Smart Routing & Split Tunneling state
  const [routingMode, setRoutingMode] = useState<'GLOBAL' | 'BYPASS_DOMESTIC' | 'SPLIT_TUNNEL'>('BYPASS_DOMESTIC');
  const [isAdBlockActive, setIsAdBlockActive] = useState(true);
  const [searchAppQuery, setSearchAppQuery] = useState('');
  const [installedApps, setInstalledApps] = useState([
    { id: 'com.facebook.katana', name: 'Facebook', isProxy: true, category: 'Social (Censored)' },
    { id: 'org.telegram.messenger', name: 'Telegram Messenger', isProxy: true, category: 'Messaging (Censored)' },
    { id: 'com.android.chrome', name: 'Google Chrome', isProxy: true, category: 'Web Browser' },
    { id: 'com.google.android.youtube', name: 'YouTube', isProxy: true, category: 'Streaming' },
    { id: 'com.kbzbank.kpay', name: 'KBZPay (Local Bank)', isProxy: false, category: 'Domestic Direct' },
    { id: 'com.wavemoney.wavepay', name: 'WavePay (Local Fintech)', isProxy: false, category: 'Domestic Direct' },
    { id: 'com.ayabank.mobile', name: 'AYA Mobile Banking', isProxy: false, category: 'Domestic Direct' },
    { id: 'com.mobile.legends', name: 'Mobile Legends: Bang Bang', isProxy: false, category: 'Gaming (Direct)' }
  ]);

  // Hotspot / Wi-Fi LAN Proxy Sharing state
  const [isLanProxyEnabled, setIsLanProxyEnabled] = useState(false);
  const [lanProxyIp] = useState('192.168.43.1');
  const [lanProxyPort] = useState(10809);
  const [hotspotClients, setHotspotClients] = useState(0);

  // Auto-Failover & Kill Switch state
  const [isKillSwitchEnabled, setIsKillSwitchEnabled] = useState(true);
  const [isAutoFailoverEnabled, setIsAutoFailoverEnabled] = useState(true);
  const [failoverStatus, setFailoverStatus] = useState<'IDLE' | 'WATCHING' | 'TRIGGERED'>('WATCHING');
  const [dashboardVisualMode, setDashboardVisualMode] = useState<'hacker' | 'ring'>('hacker');

  // Connection State
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected'>('connected');
  const [uptimeSeconds, setUptimeSeconds] = useState(142);
  const [downSpeed, setDownSpeed] = useState(4.8); // MB/s
  const [upSpeed, setUpSpeed] = useState(0.85); // MB/s
  const [totalDown, setTotalDown] = useState(128.4); // MB
  const [totalUp, setTotalUp] = useState(24.2); // MB

  // Realtime waveform history
  const [speedHistory, setSpeedHistory] = useState<number[]>([
    2.1, 2.8, 3.4, 3.2, 4.1, 4.6, 5.2, 4.8, 5.5, 4.9, 5.1, 4.7, 5.3, 4.8
  ]);

  // Servers with Built-in zero-config nodes
  const [servers, setServers] = useState<ServerProfile[]>([
    {
      id: 'builtin-warp',
      name: '⚡ Cloudflare WARP (Clean-IP Anycast)',
      host: '162.159.192.1',
      port: 2408,
      protocol: 'WARP',
      latency: 38,
      camouflage: 'WireGuard Client identity reserved=[0,0,0], MTU=1280',
      isBuiltIn: true
    },
    {
      id: 'builtin-ssh-ws',
      name: '🌐 SSH over WebSocket (Cloudflare CDN)',
      host: 'ssh.anticensor.org',
      port: 443,
      protocol: 'SSH',
      latency: 55,
      camouflage: 'HTTP/1.1 101 WebSocket Upgrade via 104.16.132.229 CDN',
      isBuiltIn: true
    },
    {
      id: 'builtin-ss-cloak',
      name: '🕶️ Shadowsocks + Cloak (dl.google.com TLS)',
      host: '198.51.100.150',
      port: 443,
      protocol: 'SS-CLOAK',
      latency: 48,
      camouflage: 'Cloak fakeDomain=dl.google.com multiplexed AEAD',
      isBuiltIn: true
    },
    {
      id: 'us-vless',
      name: 'US - Silicon Valley (Reality XHTTP)',
      host: '198.51.100.1',
      port: 443,
      protocol: 'VLESS',
      latency: 42,
      camouflage: 'www.microsoft.com (uTLS Chrome 120)'
    },
    {
      id: 'sg-hy2',
      name: 'SG - Singapore (Salamander Obfs)',
      host: '203.0.113.50',
      port: 443,
      protocol: 'HY2',
      latency: 68,
      camouflage: 'gateway.icloud.com (Random Obfs)'
    },
    {
      id: 'jp-naive',
      name: 'JP - Tokyo (Chromium Cronet)',
      host: '192.0.2.77',
      port: 443,
      protocol: 'NAIVE',
      latency: 85,
      camouflage: 'www.cloudflare.com (Padding Enabled)'
    },
    {
      id: 'de-tuic',
      name: 'DE - Frankfurt (TUIC v5 0-RTT)',
      host: '198.51.100.120',
      port: 443,
      protocol: 'TUIC',
      latency: 110,
      camouflage: 'ALPN h3/quic (BBR Congestion)'
    },
    {
      id: 'nl-awg',
      name: 'NL - Amsterdam (AmneziaWG Junk)',
      host: '198.51.100.80',
      port: 51820,
      protocol: 'AWG',
      latency: 95,
      camouflage: 'Jc=4, Jmin=40, H1..H4 Anti-DPI'
    }
  ]);

  const [selectedServerId, setSelectedServerId] = useState<string>('builtin-warp');
  const [isPingingAll, setIsPingingAll] = useState(false);

  // Import Dialog
  const [showImportDialog, setShowImportDialog] = useState(false);
  const [importText, setImportText] = useState('');

  // Logs
  const [logs, setLogs] = useState<LogEntry[]>([
    { id: '1', time: '11:24:02.110', level: 'INFO', tag: 'AegisCore', message: 'Engine initialized with 5 anti-censorship daemons' },
    { id: '2', time: '11:24:02.240', level: 'INFO', tag: 'ProcessManager', message: 'Spawned :vpn_core isolated process (PID: 18420)' },
    { id: '3', time: '11:24:02.450', level: 'DEBUG', tag: 'NativeEngine', message: 'hev-socks5-tunnel C ABI linked to epoll/kqueue' },
    { id: '4', time: '11:24:03.110', level: 'INFO', tag: 'VpnService', message: 'System VPN TUN established: fd=42, MTU=1500' },
    { id: '5', time: '11:24:03.450', level: 'INFO', tag: 'ConfigFactory', message: 'Generated xray_config.json for VLESS Reality XHTTP' },
    { id: '6', time: '11:24:04.220', level: 'INFO', tag: 'HealthMonitor', message: 'Loopback 127.0.0.1:10808 opened and ready' },
    { id: '7', time: '11:24:04.890', level: 'INFO', tag: 'TunnelPipeline', message: 'VPN Active: TUN -> hev-socks5-tunnel -> Xray -> Remote' }
  ]);
  const [logFilter, setLogFilter] = useState<'ALL' | 'INFO' | 'WARN' | 'ERROR'>('ALL');
  const [autoScroll, setAutoScroll] = useState(true);
  const terminalRef = useRef<HTMLDivElement>(null);

  // Throughput and Uptime simulation loop
  useEffect(() => {
    if (connectionStatus !== 'connected') return;

    const interval = setInterval(() => {
      setUptimeSeconds(prev => prev + 1);

      // Random jitter
      const nextDown = parseFloat((Math.random() * 4 + 2.5).toFixed(2));
      const nextUp = parseFloat((Math.random() * 0.8 + 0.4).toFixed(2));

      setDownSpeed(nextDown);
      setUpSpeed(nextUp);

      setTotalDown(prev => parseFloat((prev + nextDown * 0.1).toFixed(1)));
      setTotalUp(prev => parseFloat((prev + nextUp * 0.1).toFixed(1)));

      setSpeedHistory(prev => {
        const updated = [...prev.slice(1), nextDown];
        return updated;
      });
    }, 1200);

    return () => clearInterval(interval);
  }, [connectionStatus]);

  // Terminal auto-scroll
  useEffect(() => {
    if (autoScroll && terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const selectedServer = servers.find(s => s.id === selectedServerId) || servers[0];

  const toggleConnection = () => {
    if (connectionStatus === 'connected') {
      setConnectionStatus('disconnected');
      setDownSpeed(0);
      setUpSpeed(0);
      addLog('INFO', 'AegisVPN', 'Disconnected • Pipeline closed');
    } else if (connectionStatus === 'disconnected') {
      setConnectionStatus('connecting');
      addLog('INFO', 'AegisVPN', `Connecting to ${selectedServer.name}...`);
      setTimeout(() => {
        setConnectionStatus('connected');
        setUptimeSeconds(0);
        addLog('INFO', 'TunnelPipeline', `Connected to ${selectedServer.name} via ${selectedServer.protocol}`);
      }, 1500);
    }
  };

  const pingAll = () => {
    setIsPingingAll(true);
    addLog('INFO', 'PingTester', `Batch pinging ${servers.length} nodes...`);
    setTimeout(() => {
      setServers(prev =>
        prev.map(s => ({
          ...s,
          latency: Math.max(25, Math.floor(s.latency + (Math.random() * 20 - 10)))
        }))
      );
      setIsPingingAll(false);
      addLog('INFO', 'PingTester', 'Batch ping complete: all nodes responsive');
    }, 1000);
  };

  const [isScanningCleanIp, setIsScanningCleanIp] = useState(false);

  const scanCleanIp = () => {
    setIsScanningCleanIp(true);
    addLog('INFO', 'WarpScanner', 'Initiating Cloudflare Anycast Clean-IP probe (162.159.192.0/24)...');
    setTimeout(() => {
      const cleanIps = ['162.159.192.1', '162.159.193.10', '162.159.195.2', '188.114.96.1'];
      const picked = cleanIps[Math.floor(Math.random() * cleanIps.length)];
      setServers(prev =>
        prev.map(s =>
          s.id === 'builtin-warp'
            ? { ...s, host: picked, latency: Math.floor(Math.random() * 15 + 25), name: `⚡ Cloudflare WARP (Clean-IP: ${picked})` }
            : s
        )
      );
      setIsScanningCleanIp(false);
      addLog('INFO', 'WarpScanner', `Selected zero-packet-drop Anycast IP: ${picked} (RTT: 28ms)`);
    }, 1200);
  };

  const getProtocolBadge = (protocol: ServerProfile['protocol']) => {
    switch (protocol) {
      case 'VLESS': return 'bg-cyan-500/20 text-cyan-400 border-cyan-500/40';
      case 'HY2': return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40';
      case 'TUIC': return 'bg-amber-500/20 text-amber-400 border-amber-500/40';
      case 'NAIVE': return 'bg-violet-500/20 text-violet-400 border-violet-500/40';
      case 'AWG': return 'bg-rose-500/20 text-rose-400 border-rose-500/40';
      case 'WARP': return 'bg-cyan-500/25 text-cyan-300 border-cyan-400';
      case 'SSH': return 'bg-amber-500/25 text-amber-300 border-amber-400';
      case 'SS-CLOAK': return 'bg-purple-500/25 text-purple-300 border-purple-400';
      case 'SS': return 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40';
      default: return 'bg-slate-500/20 text-slate-400 border-slate-500/40';
    }
  };

  const pingSingle = (id: string) => {
    addLog('DEBUG', 'PingTester', `Measuring TCP handshake for node ${id}...`);
    setTimeout(() => {
      setServers(prev =>
        prev.map(s => {
          if (s.id === id) {
            const newLat = Math.max(28, Math.floor(s.latency + (Math.random() * 15 - 7)));
            addLog('INFO', 'PingTester', `${s.name}: ${newLat} ms`);
            return { ...s, latency: newLat };
          }
          return s;
        })
      );
    }, 400);
  };

  const deleteServer = (id: string) => {
    const s = servers.find(item => item.id === id);
    setServers(prev => prev.filter(item => item.id !== id));
    addLog('WARN', 'ServerManager', `Removed node: ${s?.name || id}`);
    if (selectedServerId === id) {
      const remaining = servers.filter(item => item.id !== id);
      if (remaining.length > 0) setSelectedServerId(remaining[0].id);
    }
  };

  const toggleAppProxy = (appId: string) => {
    setInstalledApps(prev =>
      prev.map(app => {
        if (app.id === appId) {
          const nextState = !app.isProxy;
          addLog('INFO', 'SplitTunnel', `${app.name} rule updated: ${nextState ? 'PROXY' : 'DIRECT'}`);
          return { ...app, isProxy: nextState };
        }
        return app;
      })
    );
  };

  const toggleLanSharing = () => {
    if (isLanProxyEnabled) {
      setIsLanProxyEnabled(false);
      setHotspotClients(0);
      addLog('WARN', 'LanProxyServer', 'Hotspot Proxy Sharing stopped');
    } else {
      setIsLanProxyEnabled(true);
      setHotspotClients(2);
      addLog('INFO', 'LanProxyServer', `LAN Proxy Server bound to 0.0.0.0:${lanProxyPort}`);
      addLog('INFO', 'LanProxyServer', `Client connected: 192.168.43.15 (Windows Laptop) via SOCKS5`);
      addLog('INFO', 'LanProxyServer', `Client connected: 192.168.43.22 (Android TV) via HTTP`);
    }
  };

  const simulateFailover = () => {
    setFailoverStatus('TRIGGERED');
    addLog('WARN', 'AutoFailover', 'ISP packet drops detected on active node! Consecutive loss = 3/3');
    setTimeout(() => {
      const alternative = servers.find(s => s.id !== selectedServerId) || servers[0];
      setSelectedServerId(alternative.id);
      setFailoverStatus('WATCHING');
      addLog('INFO', 'AutoFailover', `Zero-loss Failover hop complete: Resumed connection via ${alternative.name}`);
    }, 1000);
  };

  const addLog = (level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG', tag: string, message: string) => {
    const now = new Date();
    const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}.${now.getMilliseconds().toString().padStart(3, '0')}`;
    setLogs(prev => [...prev.slice(-150), { id: Math.random().toString(), time, level, tag, message }]);
  };

  const handleImport = () => {
    if (!importText.trim()) return;
    const isVless = importText.startsWith('vless://');
    const isHy2 = importText.startsWith('hysteria2://') || importText.startsWith('hy2://');
    const isTuic = importText.startsWith('tuic://');
    const isAwg = importText.startsWith('amneziawg://') || importText.startsWith('awg://');

    let proto: 'VLESS' | 'HY2' | 'NAIVE' | 'TUIC' | 'AWG' = 'VLESS';
    if (isHy2) proto = 'HY2';
    else if (isTuic) proto = 'TUIC';
    else if (isAwg) proto = 'AWG';

    const newServer: ServerProfile = {
      id: `imported-${Date.now()}`,
      name: `Imported ${proto} Node`,
      host: '203.0.113.88',
      port: 443,
      protocol: proto,
      latency: Math.floor(Math.random() * 60 + 35),
      camouflage: 'Custom Anti-DPI Obfuscation'
    };

    setServers(prev => [newServer, ...prev]);
    setSelectedServerId(newServer.id);
    setShowImportDialog(false);
    setImportText('');
    addLog('INFO', 'UriParser', `Imported ${proto} configuration link successfully`);
  };

  const formatUptime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const filteredLogs = logFilter === 'ALL' ? logs : logs.filter(l => l.level === logFilter);

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900/80 border border-slate-800 backdrop-blur-sm">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-black tracking-tight text-white">
              Phase 5: Jetpack Compose UI &amp; Latency Engine
            </h2>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/40">
              COMPLETE
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Material 3 Dark Theme • Live TCP Latency Engine • Reactive Speedometer • Isolated Process Terminal
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono">
            <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-slate-300">Android 14 / Compose 1.7 / Material 3</span>
          </div>
        </div>
      </div>

      {/* Main Interactive Android Emulator Frame */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Interactive Phone Screen */}
        <div className="lg:col-span-7 flex justify-center">
          <div className="w-full max-w-[420px] bg-[#0A0E17] border-2 border-slate-700/80 rounded-[40px] shadow-2xl overflow-hidden flex flex-col relative h-[780px]">
            {/* Phone Notch / Status Bar */}
            <div className="h-9 bg-[#0A0E17] flex items-center justify-between px-6 pt-1 text-[11px] font-mono text-slate-400 select-none border-b border-slate-800/40">
              <span className="font-semibold text-slate-200">11:24</span>
              <div className="w-20 h-4 bg-slate-900 rounded-full mx-auto" />
              <div className="flex items-center gap-1.5">
                <span className="text-[10px] text-cyan-400 font-bold">VPN</span>
                <span>5G</span>
                <span>98%</span>
              </div>
            </div>

            {/* Screen Content Container */}
            <div className="flex-1 overflow-y-auto p-4 flex flex-col space-y-4">
              {/* TAB 1: DASHBOARD */}
              {activeTab === 'dashboard' && (
                <div className="space-y-4 animate-in fade-in duration-200">
                  {/* App Header */}
                  <div className="flex items-center justify-between">
                    <div>
                      <h1 className="text-lg font-black tracking-wider text-white">AEGIS VPN</h1>
                      <div className="text-[10px] font-mono font-bold text-cyan-400 tracking-wider">
                        ANTI-CENSORSHIP TUNNEL
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-[10px] font-mono">
                      <div
                        className={`w-2 h-2 rounded-full ${
                          connectionStatus === 'connected'
                            ? 'bg-purple-400 shadow-[0_0_8px_rgba(192,132,252,0.85)]'
                            : connectionStatus === 'connecting'
                            ? 'bg-amber-400 animate-ping'
                            : 'bg-slate-500'
                        }`}
                      />
                      <span
                        className={`font-bold ${
                          connectionStatus === 'connected'
                            ? 'text-purple-400'
                            : connectionStatus === 'connecting'
                            ? 'text-amber-400'
                            : 'text-slate-400'
                        }`}
                      >
                        {connectionStatus === 'connected'
                          ? 'SECURE'
                          : connectionStatus === 'connecting'
                          ? 'CONNECTING'
                          : 'STANDBY'}
                      </span>
                    </div>
                  </div>

                  {/* Visual Mode Selector & Quantum Wave Canvas */}
                  <div className="flex items-center justify-between px-1">
                    <span className="text-[10px] font-mono text-purple-300/80">TELEMETRY DISPLAY:</span>
                    <div className="flex items-center gap-1 bg-slate-900 p-0.5 rounded-lg border border-slate-800 text-[9px] font-mono">
                      <button
                        onClick={() => setDashboardVisualMode('hacker')}
                        className={`px-2.5 py-0.5 rounded transition-all ${
                          dashboardVisualMode === 'hacker'
                            ? 'bg-purple-600 text-white font-bold shadow-[0_0_10px_rgba(168,85,247,0.5)]'
                            : 'text-slate-400 hover:text-white'
                        }`}
                      >
                        WAVE + BUTTON
                      </button>
                      <button
                        onClick={() => setDashboardVisualMode('ring')}
                        className={`px-2.5 py-0.5 rounded transition-all ${
                          dashboardVisualMode === 'ring'
                            ? 'bg-purple-600 text-white font-bold shadow-[0_0_10px_rgba(168,85,247,0.5)]'
                            : 'text-slate-400 hover:text-white'
                        }`}
                      >
                        BUTTON ONLY
                      </button>
                    </div>
                  </div>

                  {/* 1. Living Quantum Digital Wave & Cipher Telemetry (Visible in WAVE + BUTTON mode) */}
                  {dashboardVisualMode === 'hacker' && (
                    <CyberHackerVisual
                      isConnected={connectionStatus === 'connected'}
                      isConnecting={connectionStatus === 'connecting'}
                      downloadSpeed={downSpeed}
                      uploadSpeed={upSpeed}
                      activeServerName={selectedServer.name}
                    />
                  )}

                  {/* 2. THE BIG CIRCULAR CONNECT BUTTON WITH PURPLE POWER SHOCKWAVE & PARTICLE EFFECTS */}
                  <QuantumPowerButton
                    isConnected={connectionStatus === 'connected'}
                    isConnecting={connectionStatus === 'connecting'}
                    onToggle={toggleConnection}
                    serverName={selectedServer.name}
                    downloadSpeed={downSpeed}
                    uploadSpeed={upSpeed}
                  />

                  {/* Active Endpoint Card */}
                  <div
                    onClick={() => setActiveTab('servers')}
                    className="p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 hover:border-slate-700 cursor-pointer transition-all"
                  >
                    <div className="flex items-center justify-between text-[10px] font-mono font-bold text-slate-400 mb-2">
                      <span>ACTIVE ENDPOINT</span>
                      <span className="text-cyan-400 flex items-center gap-0.5">
                        Change <ChevronRight className="w-3 h-3" />
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-sm font-bold text-white">{selectedServer.name}</div>
                        <div className="text-xs font-mono text-slate-400">
                          {selectedServer.host}:{selectedServer.port}
                        </div>
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded-md text-[10px] font-mono font-black border ${getProtocolBadge(
                          selectedServer.protocol
                        )}`}
                      >
                        {selectedServer.protocol}
                      </span>
                    </div>

                    {/* Latency row */}
                    <div className="mt-2.5 pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
                      <span className="text-slate-400 text-[11px] flex items-center gap-1.5">
                        <Activity className="w-3.5 h-3.5 text-purple-400" /> Round-Trip Latency
                      </span>
                      <span className="font-mono font-bold text-purple-400 text-[11px]">
                        {selectedServer.latency} ms
                      </span>
                    </div>
                  </div>

                  {/* Realtime Speedometers */}
                  <div className="p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800">
                    <div className="flex items-center justify-between text-[10px] font-mono font-bold text-slate-400 mb-2">
                      <span>NETWORK THROUGHPUT</span>
                      <span className="text-purple-400">UPTIME: {formatUptime(uptimeSeconds)}</span>
                    </div>

                    <div className="grid grid-cols-2 gap-2.5">
                      <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800/80">
                        <div className="flex items-center gap-1 text-[10px] font-mono font-bold text-purple-400 mb-1">
                          <ArrowDown className="w-3 h-3" /> DOWNLOAD
                        </div>
                        <div className="text-base font-black font-mono text-white">
                          {connectionStatus === 'connected' ? `${downSpeed} MB/s` : '0.0 MB/s'}
                        </div>
                        <div className="text-[10px] font-mono text-slate-400">Total: {totalDown} MB</div>
                      </div>

                      <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800/80">
                        <div className="flex items-center gap-1 text-[10px] font-mono font-bold text-fuchsia-400 mb-1">
                          <ArrowUp className="w-3 h-3" /> UPLOAD
                        </div>
                        <div className="text-base font-black font-mono text-white">
                          {connectionStatus === 'connected' ? `${upSpeed} MB/s` : '0.0 MB/s'}
                        </div>
                        <div className="text-[10px] font-mono text-slate-400">Total: {totalUp} MB</div>
                      </div>
                    </div>

                    {/* SVG Realtime Graph */}
                    <div className="mt-3 p-2 rounded-xl bg-slate-950/80 border border-slate-800/60 h-16 flex items-end">
                      <svg className="w-full h-full" viewBox="0 0 200 50" preserveAspectRatio="none">
                        <path
                          d={`M 0,${50 - speedHistory[0] * 7} ` + speedHistory.map((val, i) => `L ${(i / (speedHistory.length - 1)) * 200},${50 - val * 7}`).join(' ')}
                          fill="none"
                          stroke="#c084fc"
                          strokeWidth="2.5"
                          strokeLinecap="round"
                        />
                      </svg>
                    </div>
                  </div>

                  {/* Active Defenses */}
                  <div className="grid grid-cols-3 gap-2 text-center">
                    <div className="p-2 rounded-xl bg-slate-900 border border-slate-800">
                      <Shield className="w-4 h-4 mx-auto text-cyan-400 mb-1" />
                      <div className="text-[10px] font-bold text-white">Anti-DPI</div>
                      <div className="text-[8px] font-mono text-slate-400">Obfuscated</div>
                    </div>
                    <div className="p-2 rounded-xl bg-slate-900 border border-slate-800">
                      <Zap className="w-4 h-4 mx-auto text-violet-400 mb-1" />
                      <div className="text-[10px] font-bold text-white">:vpn_core</div>
                      <div className="text-[8px] font-mono text-slate-400">Isolated</div>
                    </div>
                    <div className="p-2 rounded-xl bg-slate-900 border border-slate-800">
                      <Lock className="w-4 h-4 mx-auto text-emerald-400 mb-1" />
                      <div className="text-[10px] font-bold text-white">DNS Guard</div>
                      <div className="text-[8px] font-mono text-slate-400">1.1.1.1 Encrypted</div>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 2: SERVERS */}
              {activeTab === 'servers' && (
                <div className="space-y-3 animate-in fade-in duration-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-base font-black text-white">PROXY NODES ({servers.length})</h2>
                      <div className="text-[10px] font-mono text-slate-400">SELECT TUNNEL OUTBOUND</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={pingAll}
                        className="px-2 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-[11px] font-mono text-cyan-400 flex items-center gap-1"
                      >
                        <RefreshCw className={`w-3 h-3 ${isPingingAll ? 'animate-spin' : ''}`} /> Ping All
                      </button>
                      <button
                        onClick={() => setShowImportDialog(true)}
                        className="p-1 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950"
                      >
                        <Plus className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Built-in Clean IP & Emergency Rescue Bar */}
                  <div className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Zap className="w-4 h-4 text-cyan-400" />
                      <div>
                        <div className="text-xs font-bold text-white">Built-in WARP Clean-IP Scan</div>
                        <div className="text-[9px] font-mono text-cyan-400">Cloudflare Anycast Unblocked Subnets</div>
                      </div>
                    </div>
                    <button
                      onClick={scanCleanIp}
                      disabled={isScanningCleanIp}
                      className="px-2.5 py-1 rounded-lg bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-500/40 text-cyan-300 text-[10px] font-mono font-bold flex items-center gap-1.5 transition-all"
                    >
                      <RefreshCw className={`w-3 h-3 ${isScanningCleanIp ? 'animate-spin' : ''}`} />
                      {isScanningCleanIp ? 'Scanning IPs...' : 'Scan Best IP'}
                    </button>
                  </div>

                  {/* Server Cards */}
                  <div className="space-y-2">
                    {servers.map(server => {
                      const isSelected = server.id === selectedServerId;
                      return (
                        <div
                          key={server.id}
                          onClick={() => setSelectedServerId(server.id)}
                          className={`p-3 rounded-xl border transition-all cursor-pointer ${
                            isSelected
                              ? 'bg-slate-900 border-cyan-500/60 shadow-lg shadow-cyan-950/20'
                              : 'bg-slate-950 border-slate-800 hover:border-slate-700'
                          }`}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <div
                                className={`w-3.5 h-3.5 rounded-full border-2 flex items-center justify-center ${
                                  isSelected ? 'border-cyan-400 bg-cyan-400' : 'border-slate-600'
                                }`}
                              >
                                {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-slate-950" />}
                              </div>
                              <div>
                                <div className="text-xs font-bold text-white flex items-center gap-1.5">
                                  {server.name}
                                  {server.isBuiltIn && (
                                    <span className="px-1.5 py-0.2 rounded text-[8px] font-mono bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">
                                      BUILT-IN
                                    </span>
                                  )}
                                </div>
                                <div className="text-[10px] font-mono text-slate-400">
                                  {server.host}:{server.port}
                                </div>
                              </div>
                            </div>
                            <span
                              className={`px-1.5 py-0.5 rounded text-[9px] font-mono font-bold border ${getProtocolBadge(
                                server.protocol
                              )}`}
                            >
                              {server.protocol}
                            </span>
                          </div>

                          <div className="mt-2 pt-2 border-t border-slate-800/60 flex items-center justify-between">
                            <span
                              className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${
                                server.latency < 80
                                  ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40'
                                  : 'bg-amber-950/60 text-amber-400 border border-amber-800/40'
                              }`}
                            >
                              {server.latency} ms
                            </span>

                            <div className="flex items-center gap-2" onClick={e => e.stopPropagation()}>
                              <button
                                onClick={() => pingSingle(server.id)}
                                className="p-1 text-slate-400 hover:text-cyan-400"
                                title="Ping"
                              >
                                <Activity className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() => deleteServer(server.id)}
                                className="p-1 text-slate-400 hover:text-rose-400"
                                title="Delete"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* TAB 3: TERMINAL */}
              {activeTab === 'terminal' && (
                <div className="space-y-2 h-full flex flex-col animate-in fade-in duration-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-base font-black text-white flex items-center gap-1.5">
                        <Terminal className="w-4 h-4 text-emerald-400" /> LOG TERMINAL
                      </h2>
                      <div className="text-[10px] font-mono text-slate-400">DAEMON STDOUT/STDERR</div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() =>
                          navigator.clipboard.writeText(
                            logs.map(l => `[${l.time}] [${l.level}] [${l.tag}] ${l.message}`).join('\n')
                          )
                        }
                        className="p-1 text-slate-400 hover:text-white"
                        title="Copy logs"
                      >
                        <Copy className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => setLogs([])}
                        className="p-1 text-slate-400 hover:text-rose-400"
                        title="Clear logs"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Filter Pills */}
                  <div className="flex items-center gap-1.5 text-[10px] font-mono">
                    {(['ALL', 'INFO', 'WARN', 'ERROR'] as const).map(lvl => (
                      <button
                        key={lvl}
                        onClick={() => setLogFilter(lvl)}
                        className={`px-2 py-0.5 rounded ${
                          logFilter === lvl
                            ? 'bg-cyan-500 text-slate-950 font-bold'
                            : 'bg-slate-900 text-slate-400 hover:text-white'
                        }`}
                      >
                        {lvl}
                      </button>
                    ))}
                    <span className="ml-auto text-slate-400">Auto-scroll ON</span>
                  </div>

                  {/* Terminal Black Box */}
                  <div
                    ref={terminalRef}
                    className="flex-1 bg-[#050811] p-2.5 rounded-xl border border-slate-800 font-mono text-[10px] overflow-y-auto space-y-1 select-text"
                  >
                    {filteredLogs.map(l => (
                      <div key={l.id} className="leading-relaxed">
                        <span className="text-slate-500">{l.time} </span>
                        <span
                          className={`font-bold ${
                            l.level === 'INFO'
                              ? 'text-cyan-400'
                              : l.level === 'WARN'
                              ? 'text-amber-400'
                              : l.level === 'ERROR'
                              ? 'text-rose-400'
                              : 'text-violet-400'
                          }`}
                        >
                          [{l.level}]
                        </span>
                        <span className="text-slate-400"> &lt;{l.tag}&gt; </span>
                        <span className="text-slate-200">{l.message}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* TAB: SMART ROUTING & SPLIT TUNNELING */}
              {activeTab === 'routing' && (
                <div className="flex-1 overflow-y-auto p-4 space-y-3.5">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-base font-black text-white">SMART ROUTING & APPS</h2>
                      <div className="text-[10px] font-mono text-slate-400">DOMAIN & PER-APP TRAFFIC SHAPER</div>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 font-bold">
                      ACTIVE
                    </span>
                  </div>

                  {/* Mode Selector */}
                  <div className="grid grid-cols-3 gap-1.5 p-1 rounded-xl bg-slate-900 border border-slate-800 text-[10px] font-mono">
                    <button
                      onClick={() => {
                        setRoutingMode('BYPASS_DOMESTIC');
                        addLog('INFO', 'RoutingEngine', 'Switched mode: Bypass Myanmar domestic banks & direct sites');
                      }}
                      className={`p-2 rounded-lg text-center transition-all ${
                        routingMode === 'BYPASS_DOMESTIC'
                          ? 'bg-cyan-500 text-slate-950 font-bold shadow'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      Bypass MM
                    </button>
                    <button
                      onClick={() => {
                        setRoutingMode('SPLIT_TUNNEL');
                        addLog('INFO', 'RoutingEngine', 'Switched mode: Per-App Split Tunneling');
                      }}
                      className={`p-2 rounded-lg text-center transition-all ${
                        routingMode === 'SPLIT_TUNNEL'
                          ? 'bg-cyan-500 text-slate-950 font-bold shadow'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      Per-App
                    </button>
                    <button
                      onClick={() => {
                        setRoutingMode('GLOBAL');
                        addLog('INFO', 'RoutingEngine', 'Switched mode: Global Proxy (Full Device)');
                      }}
                      className={`p-2 rounded-lg text-center transition-all ${
                        routingMode === 'GLOBAL'
                          ? 'bg-cyan-500 text-slate-950 font-bold shadow'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      Global
                    </button>
                  </div>

                  {/* Mode Summary Banner */}
                  <div className="p-3 rounded-xl bg-slate-900/90 border border-slate-800 text-xs">
                    {routingMode === 'BYPASS_DOMESTIC' && (
                      <div className="space-y-1">
                        <div className="font-bold text-cyan-300 flex items-center gap-1.5">
                          <Globe className="w-3.5 h-3.5" /> Smart Domestic Bypass Active
                        </div>
                        <p className="text-[11px] text-slate-400 leading-relaxed">
                          Myanmar banks (KBZPay, WavePay, AYA, CB) and .mm domains go direct without VPN for fast speeds and zero bank blocking. Blocked sites route through tunnel.
                        </p>
                      </div>
                    )}
                    {routingMode === 'SPLIT_TUNNEL' && (
                      <div className="space-y-1">
                        <div className="font-bold text-violet-300 flex items-center gap-1.5">
                          <Split className="w-3.5 h-3.5" /> Per-App Package Filter Active
                        </div>
                        <p className="text-[11px] text-slate-400 leading-relaxed">
                          Only selected apps flow through the encrypted tunnel. Unselected apps use standard local connection.
                        </p>
                      </div>
                    )}
                    {routingMode === 'GLOBAL' && (
                      <div className="space-y-1">
                        <div className="font-bold text-amber-300 flex items-center gap-1.5">
                          <Shield className="w-3.5 h-3.5" /> Full Tunnel Protection
                        </div>
                        <p className="text-[11px] text-slate-400 leading-relaxed">
                          100% of phone traffic is encrypted and forwarded through the active proxy outbound.
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Security Shield Toggles */}
                  <div className="space-y-2">
                    <div className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Lock className="w-4 h-4 text-cyan-400" />
                        <div>
                          <div className="text-xs font-bold text-white">AdBlock & Tracker Shield</div>
                          <div className="text-[10px] font-mono text-slate-400">Reject ad tracking & telemetry domains</div>
                        </div>
                      </div>
                      <button
                        onClick={() => {
                          setIsAdBlockActive(!isAdBlockActive);
                          addLog('INFO', 'AdBlock', `Tracker Shield ${!isAdBlockActive ? 'ENABLED' : 'DISABLED'}`);
                        }}
                        className={`w-9 h-5 rounded-full p-0.5 transition-colors ${
                          isAdBlockActive ? 'bg-cyan-500' : 'bg-slate-700'
                        }`}
                      >
                        <div
                          className={`w-4 h-4 rounded-full bg-slate-950 transition-transform ${
                            isAdBlockActive ? 'translate-x-4' : 'translate-x-0'
                          }`}
                        />
                      </button>
                    </div>

                    <div className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Shield className="w-4 h-4 text-emerald-400" />
                        <div>
                          <div className="text-xs font-bold text-white">Strict Kill Switch</div>
                          <div className="text-[10px] font-mono text-slate-400">Block internet if VPN tunnel drops</div>
                        </div>
                      </div>
                      <button
                        onClick={() => {
                          setIsKillSwitchEnabled(!isKillSwitchEnabled);
                          addLog('WARN', 'KillSwitch', `Kill Switch ${!isKillSwitchEnabled ? 'ARMED' : 'DISARMED'}`);
                        }}
                        className={`w-9 h-5 rounded-full p-0.5 transition-colors ${
                          isKillSwitchEnabled ? 'bg-emerald-500' : 'bg-slate-700'
                        }`}
                      >
                        <div
                          className={`w-4 h-4 rounded-full bg-slate-950 transition-transform ${
                            isKillSwitchEnabled ? 'translate-x-4' : 'translate-x-0'
                          }`}
                        />
                      </button>
                    </div>
                  </div>

                  {/* App Selection List for Split Tunneling */}
                  <div className="space-y-2 pt-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-white">APPLICATION SPLIT LIST</span>
                      <span className="text-[10px] font-mono text-slate-400">
                        {installedApps.filter(a => a.isProxy).length} / {installedApps.length} Proxying
                      </span>
                    </div>

                    <div className="relative">
                      <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
                      <input
                        type="text"
                        placeholder="Search apps (e.g. Facebook, Bank)..."
                        value={searchAppQuery}
                        onChange={e => setSearchAppQuery(e.target.value)}
                        className="w-full pl-8 pr-3 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 font-mono"
                      />
                    </div>

                    <div className="space-y-1.5 max-h-56 overflow-y-auto pr-1">
                      {installedApps
                        .filter(a => a.name.toLowerCase().includes(searchAppQuery.toLowerCase()))
                        .map(app => (
                          <div
                            key={app.id}
                            onClick={() => toggleAppProxy(app.id)}
                            className="p-2.5 rounded-xl bg-slate-950 border border-slate-800/80 hover:border-slate-700 flex items-center justify-between cursor-pointer transition-colors"
                          >
                            <div>
                              <div className="text-xs font-bold text-white">{app.name}</div>
                              <div className="text-[9px] font-mono text-slate-400">{app.category}</div>
                            </div>
                            <span
                              className={`px-2 py-0.5 rounded text-[9px] font-mono font-bold border ${
                                app.isProxy
                                  ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'
                                  : 'bg-slate-800 text-slate-400 border-slate-700'
                              }`}
                            >
                              {app.isProxy ? 'VPN PROXY' : 'DIRECT'}
                            </span>
                          </div>
                        ))}
                    </div>
                  </div>
                </div>
              )}

              {/* TAB: HOTSPOT / LAN PROXY SHARING */}
              {activeTab === 'hotspot' && (
                <div className="flex-1 overflow-y-auto p-4 space-y-3.5">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-base font-black text-white">SHARE OVER WI-FI</h2>
                      <div className="text-[10px] font-mono text-slate-400">LAN SOCKS5 / HTTP PROXY HOST</div>
                    </div>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                        isLanProxyEnabled
                          ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                          : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}
                    >
                      {isLanProxyEnabled ? 'SHARING ON' : 'STANDBY'}
                    </span>
                  </div>

                  {/* Big Hotspot Master Toggle Card */}
                  <div className="p-4 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
                            isLanProxyEnabled
                              ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40 shadow-lg shadow-emerald-950/40'
                              : 'bg-slate-800 text-slate-400 border-slate-700'
                          }`}
                        >
                          <Wifi className="w-5 h-5" />
                        </div>
                        <div>
                          <div className="text-sm font-bold text-white">Hotspot Proxy Relay</div>
                          <div className="text-[10px] font-mono text-slate-400">
                            Port {lanProxyPort} • {hotspotClients} Devices Connected
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={toggleLanSharing}
                        className={`px-3 py-1.5 rounded-xl font-mono text-xs font-bold transition-all ${
                          isLanProxyEnabled
                            ? 'bg-rose-500 hover:bg-rose-400 text-white'
                            : 'bg-cyan-500 hover:bg-cyan-400 text-slate-950'
                        }`}
                      >
                        {isLanProxyEnabled ? 'Stop Share' : 'Start Share'}
                      </button>
                    </div>

                    {/* Gateway IP Details */}
                    {isLanProxyEnabled && (
                      <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono space-y-1.5">
                        <div className="flex items-center justify-between text-slate-400 text-[10px]">
                          <span>PROXY ADDRESS FOR PC &amp; TV:</span>
                          <span className="text-emerald-400">ONLINE</span>
                        </div>
                        <div className="flex items-center justify-between text-cyan-300 font-bold bg-slate-900 p-1.5 rounded-lg border border-slate-800">
                          <span>{lanProxyIp}:{lanProxyPort}</span>
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(`${lanProxyIp}:${lanProxyPort}`);
                              addLog('INFO', 'LanProxy', 'Copied proxy host address to clipboard');
                            }}
                            className="text-slate-400 hover:text-white"
                            title="Copy IP:Port"
                          >
                            <Copy className="w-3.5 h-3.5" />
                          </button>
                        </div>
                        <div className="text-[10px] text-slate-400">
                          Laptop or Smart TV can set HTTP / SOCKS5 proxy to this IP.
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Auto-Failover Monitor Card */}
                  <div className="p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Activity className="w-4 h-4 text-cyan-400" />
                        <div>
                          <div className="text-xs font-bold text-white">Seamless Auto-Failover</div>
                          <div className="text-[10px] font-mono text-slate-400">Zero-loss Node Heartbeat</div>
                        </div>
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded text-[9px] font-mono font-bold ${
                          failoverStatus === 'TRIGGERED'
                            ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40 animate-pulse'
                            : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                        }`}
                      >
                        {failoverStatus === 'TRIGGERED' ? 'HOPPING NODE...' : 'WATCHING'}
                      </span>
                    </div>

                    <p className="text-[11px] text-slate-400 leading-relaxed">
                      Monitors consecutive packet drops. If active node drops 3 pings, instantly switches to backup WARP or nearest low-latency lifeline.
                    </p>

                    <button
                      onClick={simulateFailover}
                      className="w-full py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-[11px] font-mono font-bold text-cyan-300 flex items-center justify-center gap-1.5 transition-colors border border-slate-700"
                    >
                      <Zap className="w-3.5 h-3.5 text-amber-400" /> Test Auto-Failover Simulation
                    </button>
                  </div>

                  {/* Connected Devices List */}
                  <div className="p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-2">
                    <div className="text-xs font-bold text-white">CONNECTED LAN DEVICES</div>
                    {isLanProxyEnabled ? (
                      <div className="space-y-1.5">
                        <div className="p-2 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between text-xs">
                          <div>
                            <div className="font-bold text-white text-[11px]">Windows 11 Workstation</div>
                            <div className="text-[9px] font-mono text-slate-400">192.168.43.15 • SOCKS5 tunnel</div>
                          </div>
                          <span className="text-[10px] font-mono text-emerald-400">3.4 MB/s</span>
                        </div>
                        <div className="p-2 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between text-xs">
                          <div>
                            <div className="font-bold text-white text-[11px]">Android Smart TV (Living Room)</div>
                            <div className="text-[9px] font-mono text-slate-400">192.168.43.22 • HTTP tunnel</div>
                          </div>
                          <span className="text-[10px] font-mono text-emerald-400">1.2 MB/s</span>
                        </div>
                      </div>
                    ) : (
                      <div className="p-3 rounded-xl bg-slate-950 border border-slate-800/60 text-center text-xs text-slate-500 font-mono">
                        Turn on Hotspot Proxy Relay to share VPN with nearby devices.
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Bottom Navigation Bar */}
            <div className="h-16 bg-[#111827] border-t border-slate-800 grid grid-cols-5 items-center px-2">
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`flex flex-col items-center justify-center gap-1 text-[10px] font-mono transition-colors ${
                  activeTab === 'dashboard' ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                <Shield className="w-4 h-4" />
                <span>Shield</span>
              </button>

              <button
                onClick={() => setActiveTab('servers')}
                className={`flex flex-col items-center justify-center gap-1 text-[10px] font-mono transition-colors ${
                  activeTab === 'servers' ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                <Server className="w-4 h-4" />
                <span>Nodes</span>
              </button>

              <button
                onClick={() => setActiveTab('routing')}
                className={`flex flex-col items-center justify-center gap-1 text-[10px] font-mono transition-colors ${
                  activeTab === 'routing' ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                <Split className="w-4 h-4" />
                <span>Rules</span>
              </button>

              <button
                onClick={() => setActiveTab('hotspot')}
                className={`flex flex-col items-center justify-center gap-1 text-[10px] font-mono transition-colors ${
                  activeTab === 'hotspot' ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                <Share2 className="w-4 h-4" />
                <span>Hotspot</span>
              </button>

              <button
                onClick={() => setActiveTab('terminal')}
                className={`flex flex-col items-center justify-center gap-1 text-[10px] font-mono transition-colors ${
                  activeTab === 'terminal' ? 'text-cyan-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                <Terminal className="w-4 h-4" />
                <span>Logs</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right: Technical Specification & Features Delivered */}
        <div className="lg:col-span-5 space-y-4">
          <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-4">
            <h3 className="text-base font-black tracking-tight text-white flex items-center gap-2">
              <Smartphone className="w-5 h-5 text-cyan-400" />
              Jetpack Compose Architecture Delivered
            </h3>

            <div className="space-y-3 text-xs text-slate-300">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-bold text-cyan-400 font-mono mb-1">1. PingLatencyTester.kt</div>
                <p className="text-slate-400 text-[11px]">
                  Implements raw TCP SYN-ACK socket latency timing (nanosecond resolution) and HTTP 204
                  generate_204 connectivity tests with coroutine async/await batching.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-bold text-emerald-400 font-mono mb-1">2. ServerViewModel.kt</div>
                <p className="text-slate-400 text-[11px]">
                  Reactive AndroidX ViewModel managing <span className="font-mono text-slate-200">StateFlow</span> for
                  active connection states, speed telemetry simulation, circular log buffer (300 lines), and
                  intent-based service commands.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-bold text-violet-400 font-mono mb-1">3. Jetpack Compose Screens</div>
                <ul className="list-disc list-inside text-slate-400 text-[11px] space-y-1">
                  <li>
                    <span className="font-mono text-slate-200">MainScreen.kt</span>: Pulsing glow connect button,
                    throughput meters, SVG waveform graph.
                  </li>
                  <li>
                    <span className="font-mono text-slate-200">ServerListScreen.kt</span>: Color-coded latency pills,
                    batch ping with spin animation, import FAB.
                  </li>
                  <li>
                    <span className="font-mono text-slate-200">LogViewerScreen.kt</span>: Monospace daemon logcat with
                    filtering and auto-scroll.
                  </li>
                </ul>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-bold text-amber-400 font-mono mb-1">4. MainActivity.kt &amp; VpnService</div>
                <p className="text-slate-400 text-[11px]">
                  Handles <span className="font-mono text-slate-200">VpnService.prepare()</span> system dialogs,
                  POST_NOTIFICATIONS runtime permissions (Android 13+), and deep-link intent schemes.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="font-bold text-cyan-300 font-mono mb-1">5. Flagship Anti-Censorship Suite</div>
                <ul className="list-disc list-inside text-slate-400 text-[11px] space-y-1">
                  <li>
                    <span className="font-mono text-cyan-300">Smart Domestic Bypass</span>: Direct .mm &amp; Banking apps (KBZPay, WavePay, AYA) without VPN block flags.
                  </li>
                  <li>
                    <span className="font-mono text-violet-300">Per-App Split Tunnel</span>: App package filter with native VpnService.Builder integration.
                  </li>
                  <li>
                    <span className="font-mono text-emerald-300">Seamless Auto-Failover</span>: Zero-downtime auto-hop to backup WARP or nearest lifeline.
                  </li>
                  <li>
                    <span className="font-mono text-rose-300">Strict Kill Switch &amp; DoH</span>: Prevents plaintext DNS &amp; IP leaks during packet drop.
                  </li>
                  <li>
                    <span className="font-mono text-amber-300">Hotspot LAN Proxy Relay</span>: Shares VPN with PCs and TVs via 0.0.0.0:10809.
                  </li>
                </ul>
              </div>
            </div>
          </div>

          {/* Quick Stats Grid */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
              <div className="text-[10px] font-mono text-slate-400 uppercase">Architecture Stack</div>
              <div className="text-2xl font-black text-white font-mono mt-1">Flagship</div>
              <div className="text-[10px] text-emerald-400 mt-1">Smart Rules • LAN Relay • Failover</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
              <div className="text-[10px] font-mono text-slate-400 uppercase">Supported Protocols</div>
              <div className="text-2xl font-black text-cyan-400 font-mono mt-1">9+ Protocols</div>
              <div className="text-[10px] text-slate-400 mt-1">VLESS, HY2, WARP, SSH, CLOAK...</div>
            </div>
          </div>
        </div>
      </div>

      {/* Import Modal */}
      {showImportDialog && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-black text-white font-mono">IMPORT PROXY NODE</h3>
              <button onClick={() => setShowImportDialog(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>
            <p className="text-xs text-slate-400">
              Paste standard share link (vless://, hysteria2://, tuic://, amneziawg://):
            </p>
            <textarea
              value={importText}
              onChange={e => setImportText(e.target.value)}
              placeholder="vless://... or hysteria2://..."
              className="w-full h-28 bg-slate-950 border border-slate-800 rounded-xl p-3 font-mono text-xs text-white focus:outline-none focus:border-cyan-500"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowImportDialog(false)}
                className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                onClick={handleImport}
                disabled={!importText.trim()}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-cyan-500 hover:bg-cyan-400 text-slate-950 disabled:opacity-40"
              >
                Import Node
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
