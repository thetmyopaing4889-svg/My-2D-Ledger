import React, { useState, useMemo } from 'react';
import { 
  FileCode, Play, CheckCircle, Copy, Check, Terminal, Shield, 
  Sparkles, RefreshCw, Cpu, Layers, AlertCircle 
} from 'lucide-react';

interface PresetItem {
  id: string;
  name: string;
  protocol: 'VLESS' | 'Hysteria2' | 'NaiveProxy' | 'TUIC' | 'AmneziaWG';
  scheme: string;
  description: string;
  sample: string;
}

const PRESETS: PresetItem[] = [
  {
    id: 'vless-reality-xhttp',
    name: 'VLESS XTLS-Reality + XHTTP',
    protocol: 'VLESS',
    scheme: 'vless://',
    description: 'Camouflaged against GFW/DPI with Chrome uTLS fingerprint, Reality public key, and XHTTP stream transport.',
    sample: 'vless://9a8b7c6d-5e4f-3a2b-1c0d-ef9876543210@198.51.100.1:443?security=reality&encryption=none&pbk=7z_K_kH2xP3R9wQ0aB8c-DeFgHiJkLmNoPqRsTuVwXy&headerType=none&type=xhttp&flow=xtls-rprx-vision&sni=www.apple.com&fp=chrome&sid=1a2b3c4d&spx=%2F&path=%2Fpush-service#US-Reality-XHTTP'
  },
  {
    id: 'hysteria2-salamander',
    name: 'Hysteria 2 with Salamander',
    protocol: 'Hysteria2',
    scheme: 'hysteria2://',
    description: 'Obfuscated QUIC UDP transport with Salamander randomizer and brutal congestion control.',
    sample: 'hysteria2://supersecretpass@203.0.113.50:443?insecure=0&sni=gateway.icloud.com&obfs=salamander&obfs-password=SaltPass998&upmbps=50&downmbps=200#SG-Hy2-Salamander'
  },
  {
    id: 'naive-https-cronet',
    name: 'NaïveProxy (Chromium Cronet)',
    protocol: 'NaiveProxy',
    scheme: 'naive+https://',
    description: 'Chromium network stack disguising traffic as genuine HTTPS web browsing with uniform packet padding.',
    sample: 'naive+https://user88:CronetSecPass99@proxy.example.com:443?sni=proxy.example.com&padding=1#DE-Naive-Cronet'
  },
  {
    id: 'tuic-v5-bbr',
    name: 'TUIC v5 (0-RTT BBR)',
    protocol: 'TUIC',
    scheme: 'tuic://',
    description: '0-RTT QUIC multiplexing with custom ALPN and server-side BBR rate management.',
    sample: 'tuic://9a8b7c6d-5e4f-3a2b-1c0d-ef9876543210:AuthSecret2026@198.51.100.4:8443?congestion_control=bbr&udp_relay_mode=native&sni=cf.cloudflare.com&alpn=h3,spdy/3.1#JP-TUIC-BBR'
  },
  {
    id: 'amnezia-uri',
    name: 'AmneziaWG URI (Junk Injection)',
    protocol: 'AmneziaWG',
    scheme: 'amneziawg://',
    description: 'WireGuard with randomized packet headers and junk packet bursts (Jc, Jmin, Jmax, S1, S2, H1..H4).',
    sample: 'amneziawg://Y2xpZW50X3ByaXZhdGVfa2V5=@198.51.100.80:51820?public_key=c2VydmVyX3B1YmxpY19rZXk=&address=10.0.0.2/32&jc=4&jmin=40&jmax=70&s1=20&s2=20&h1=1&h2=2&h3=3&h4=4#AWG-DPI-Resistant'
  },
  {
    id: 'amnezia-conf',
    name: 'AmneziaWG / WireGuard .conf Text',
    protocol: 'AmneziaWG',
    scheme: '[Interface]',
    description: 'Standard INI format containing [Interface] and [Peer] blocks with Amnezia header mutation keys.',
    sample: `[Interface]
Address = 10.0.0.2/32
PrivateKey = Y2xpZW50X3ByaXZhdGVfa2V5=
DNS = 1.1.1.1, 8.8.8.8
MTU = 1360
Jc = 4
Jmin = 40
Jmax = 70
S1 = 20
S2 = 20
H1 = 1
H2 = 2
H3 = 3
H4 = 4

[Peer]
PublicKey = c2VydmVyX3B1YmxpY19rZXk=
Endpoint = 198.51.100.80:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25`
  }
];

export const Phase4ConfigLab: React.FC = () => {
  const [selectedPresetId, setSelectedPresetId] = useState(PRESETS[0].id);
  const [inputText, setInputText] = useState(PRESETS[0].sample);
  const [socksPort, setSocksPort] = useState(10808);
  const [copiedConfig, setCopiedConfig] = useState(false);
  const [copiedUri, setCopiedUri] = useState(false);
  const [fileSimulatedSaved, setFileSimulatedSaved] = useState(false);

  const activePreset = PRESETS.find(p => p.id === selectedPresetId) || PRESETS[0];

  const handleSelectPreset = (preset: PresetItem) => {
    setSelectedPresetId(preset.id);
    setInputText(preset.sample);
    setFileSimulatedSaved(false);
  };

  // Parsing simulation adhering to UriParser.kt
  const parseResult = useMemo(() => {
    const raw = inputText.trim();
    try {
      if (raw.startsWith('vless://')) {
        const uri = new URL(raw);
        const params = new URLSearchParams(uri.search);
        return {
          valid: true,
          protocol: 'VLESS_REALITY',
          kotlinModel: 'VlessConfig',
          binary: 'xray',
          server: uri.hostname,
          port: uri.port ? parseInt(uri.port) : 443,
          uuid: uri.username,
          flow: params.get('flow') || 'xtls-rprx-vision',
          security: params.get('security') || 'reality',
          publicKey: params.get('pbk') || '',
          sni: params.get('sni') || uri.hostname,
          fingerprint: params.get('fp') || 'chrome',
          shortId: params.get('sid') || '',
          spiderX: params.get('spx') || '/',
          transportType: params.get('type') || 'tcp',
          path: params.get('path') || '',
          name: decodeURIComponent(uri.hash.replace('#', '')) || 'VLESS Node',
          antiDpiDetails: [
            { label: 'Reality Camouflage SNI', val: params.get('sni') || uri.hostname },
            { label: 'Reality Public Key (pbk)', val: params.get('pbk') || 'N/A' },
            { label: 'uTLS Fingerprint', val: params.get('fp') || 'chrome' },
            { label: 'Short ID (sid)', val: params.get('sid') || 'none' },
            { label: 'SpiderX Crawl Path', val: params.get('spx') || '/' },
            { label: 'Transport Layer', val: params.get('type') || 'tcp' },
            { label: 'XHTTP Path', val: params.get('path') || 'N/A' }
          ]
        };
      }

      if (raw.startsWith('hysteria2://') || raw.startsWith('hy2://')) {
        const normalized = raw.replace(/^hy2:\/\//, 'hysteria2://');
        const uri = new URL(normalized);
        const params = new URLSearchParams(uri.search);
        return {
          valid: true,
          protocol: 'HYSTERIA2',
          kotlinModel: 'Hysteria2Config',
          binary: 'hysteria',
          server: uri.hostname,
          port: uri.port ? parseInt(uri.port) : 443,
          auth: uri.username,
          obfsType: params.get('obfs') || 'salamander',
          obfsPassword: params.get('obfs-password') || '',
          sni: params.get('sni') || uri.hostname,
          insecure: params.get('insecure') === '1',
          upMbps: parseInt(params.get('upmbps') || '50'),
          downMbps: parseInt(params.get('downmbps') || '200'),
          name: decodeURIComponent(uri.hash.replace('#', '')) || 'Hysteria 2 Node',
          antiDpiDetails: [
            { label: 'Obfuscation Type', val: params.get('obfs') || 'salamander' },
            { label: 'Salamander Password', val: params.get('obfs-password') || 'SecretKey' },
            { label: 'SNI Camouflage', val: params.get('sni') || uri.hostname },
            { label: 'Bandwidth Up / Down', val: `${params.get('upmbps') || '50'}M / ${params.get('downmbps') || '200'}M` },
            { label: 'Insecure Skip Verify', val: params.get('insecure') === '1' ? 'true' : 'false' }
          ]
        };
      }

      if (raw.startsWith('naive+https://') || raw.startsWith('naive+quic://')) {
        const isQuic = raw.startsWith('naive+quic://');
        const normalized = raw.replace(/^naive\+https:\/\//, 'https://').replace(/^naive\+quic:\/\//, 'https://');
        const uri = new URL(normalized);
        const params = new URLSearchParams(uri.search);
        return {
          valid: true,
          protocol: 'NAIVE_PROXY',
          kotlinModel: 'NaiveConfig',
          binary: 'naive',
          server: uri.hostname,
          port: uri.port ? parseInt(uri.port) : 443,
          username: uri.username,
          password: uri.password,
          networkType: isQuic ? 'quic' : 'https',
          sni: params.get('sni') || uri.hostname,
          padding: params.get('padding') !== '0',
          name: decodeURIComponent(uri.hash.replace('#', '')) || 'Naïve Node',
          antiDpiDetails: [
            { label: 'Stack Camouflage', val: 'Chromium Cronet HTTP/2 & TLS Engine' },
            { label: 'Network Transport', val: isQuic ? 'HTTP/3 (QUIC)' : 'HTTP/2 (HTTPS)' },
            { label: 'Padding Enabled', val: params.get('padding') !== '0' ? 'Yes (Prevents size fingerprinting)' : 'No' },
            { label: 'SNI Target', val: params.get('sni') || uri.hostname }
          ]
        };
      }

      if (raw.startsWith('tuic://')) {
        const uri = new URL(raw);
        const params = new URLSearchParams(uri.search);
        return {
          valid: true,
          protocol: 'TUIC',
          kotlinModel: 'TuicConfig',
          binary: 'tuic-client',
          server: uri.hostname,
          port: uri.port ? parseInt(uri.port) : 443,
          uuid: uri.username,
          password: uri.password,
          congestionControl: params.get('congestion_control') || 'bbr',
          udpRelayMode: params.get('udp_relay_mode') || 'native',
          sni: params.get('sni') || uri.hostname,
          alpn: (params.get('alpn') || 'h3,spdy/3.1').split(','),
          name: decodeURIComponent(uri.hash.replace('#', '')) || 'TUIC Node',
          antiDpiDetails: [
            { label: 'Congestion Algorithm', val: (params.get('congestion_control') || 'bbr').toUpperCase() },
            { label: 'UDP Relay Mode', val: params.get('udp_relay_mode') || 'native' },
            { label: '0-RTT Handshake', val: 'Enabled' },
            { label: 'ALPN Negotiation', val: params.get('alpn') || 'h3, spdy/3.1' }
          ]
        };
      }

      if (raw.startsWith('amneziawg://') || raw.startsWith('awg://')) {
        const normalized = raw.replace(/^awg:\/\//, 'amneziawg://');
        const uri = new URL(normalized);
        const params = new URLSearchParams(uri.search);
        return {
          valid: true,
          protocol: 'AMNEZIA_WG',
          kotlinModel: 'AmneziaWgConfig',
          binary: 'amneziawg-go',
          server: uri.hostname,
          port: uri.port ? parseInt(uri.port) : 51820,
          privateKey: decodeURIComponent(uri.username),
          publicKey: params.get('public_key') || '',
          addressIpv4: params.get('address') || '10.0.0.2/32',
          jc: parseInt(params.get('jc') || '4'),
          jmin: parseInt(params.get('jmin') || '40'),
          jmax: parseInt(params.get('jmax') || '70'),
          s1: parseInt(params.get('s1') || '20'),
          s2: parseInt(params.get('s2') || '20'),
          h1: params.get('h1') || '1',
          h2: params.get('h2') || '2',
          h3: params.get('h3') || '3',
          h4: params.get('h4') || '4',
          name: decodeURIComponent(uri.hash.replace('#', '')) || 'AmneziaWG Node',
          antiDpiDetails: [
            { label: 'Junk Packet Count (Jc)', val: `${params.get('jc') || 4} bursts` },
            { label: 'Junk Size Range (Jmin..Jmax)', val: `${params.get('jmin') || 40}..${params.get('jmax') || 70} bytes` },
            { label: 'Init/Resp Header Junk (S1, S2)', val: `S1=${params.get('s1') || 20}B, S2=${params.get('s2') || 20}B` },
            { label: 'Mutated Packet Types (H1..H4)', val: `H1=${params.get('h1') || 1}, H2=${params.get('h2') || 2}, H3=${params.get('h3') || 3}, H4=${params.get('h4') || 4}` }
          ]
        };
      }

      if (raw.includes('[Interface]') && raw.includes('[Peer]')) {
        const lines = raw.split('\n');
        const getVal = (k: string) => {
          const l = lines.find(x => x.toLowerCase().startsWith(k.toLowerCase() + ' '));
          return l ? l.split('=')[1]?.trim() : '';
        };
        const endpoint = getVal('Endpoint') || '198.51.100.80:51820';
        const [host, portStr] = endpoint.split(':');
        return {
          valid: true,
          protocol: 'AMNEZIA_WG',
          kotlinModel: 'AmneziaWgConfig (Parsed from .conf)',
          binary: 'amneziawg-go',
          server: host || '198.51.100.80',
          port: portStr ? parseInt(portStr) : 51820,
          privateKey: getVal('PrivateKey') || '',
          publicKey: getVal('PublicKey') || '',
          addressIpv4: getVal('Address') || '10.0.0.2/32',
          jc: parseInt(getVal('Jc') || '4'),
          jmin: parseInt(getVal('Jmin') || '40'),
          jmax: parseInt(getVal('Jmax') || '70'),
          s1: parseInt(getVal('S1') || '20'),
          s2: parseInt(getVal('S2') || '20'),
          h1: getVal('H1') || '1',
          h2: getVal('H2') || '2',
          h3: getVal('H3') || '3',
          h4: getVal('H4') || '4',
          name: 'AmneziaWG Imported Profile',
          antiDpiDetails: [
            { label: 'Junk Packet Count (Jc)', val: `${getVal('Jc') || 4} packets` },
            { label: 'Junk Size Window (Jmin..Jmax)', val: `${getVal('Jmin') || 40}..${getVal('Jmax') || 70} bytes` },
            { label: 'Handshake Padding (S1, S2)', val: `S1=${getVal('S1') || 20}B, S2=${getVal('S2') || 20}B` },
            { label: 'Custom Header Magic (H1..H4)', val: `H1=${getVal('H1') || 1}, H2=${getVal('H2') || 2}, H3=${getVal('H3') || 3}, H4=${getVal('H4') || 4}` }
          ]
        };
      }

      return { valid: false, error: 'Unrecognized URI scheme or invalid configuration format.' };
    } catch (err: any) {
      return { valid: false, error: err.message || 'Syntax parsing error.' };
    }
  }, [inputText]);

  // Generator simulation mirroring XrayConfigGenerator / HysteriaConfigGenerator / etc.
  const generatedConfig = useMemo(() => {
    if (!parseResult.valid) return null;

    if (parseResult.protocol === 'VLESS_REALITY') {
      const xray = {
        log: { loglevel: 'warning' },
        inbounds: [
          {
            tag: 'socks-in',
            listen: '127.0.0.1',
            port: socksPort,
            protocol: 'socks',
            settings: { auth: 'noauth', udp: true },
            sniffing: { enabled: true, destOverride: ['http', 'tls', 'quic'] }
          }
        ],
        outbounds: [
          {
            tag: 'proxy',
            protocol: 'vless',
            settings: {
              vnext: [
                {
                  address: parseResult.server,
                  port: parseResult.port,
                  users: [
                    {
                      id: parseResult.uuid,
                      encryption: 'none',
                      flow: parseResult.flow
                    }
                  ]
                }
              ]
            },
            streamSettings: {
              network: parseResult.transportType,
              security: parseResult.security,
              realitySettings: {
                show: false,
                fingerprint: parseResult.fingerprint,
                serverName: parseResult.sni,
                publicKey: parseResult.publicKey,
                shortId: parseResult.shortId,
                spiderX: parseResult.spiderX
              },
              ...(parseResult.transportType === 'xhttp' ? {
                xhttpSettings: { path: parseResult.path || '/', mode: 'auto' }
              } : {})
            }
          },
          { tag: 'direct', protocol: 'freedom' },
          { tag: 'block', protocol: 'blackhole' }
        ],
        dns: { servers: ['1.1.1.1', '8.8.8.8', 'localhost'] },
        routing: {
          domainStrategy: 'IPIfNonMatch',
          rules: [
            { type: 'field', outboundTag: 'direct', ip: ['geoip:private'] },
            { type: 'field', outboundTag: 'proxy', network: 'tcp,udp' }
          ]
        }
      };

      return {
        fileName: 'xray_config.json',
        filePath: 'context.filesDir/configs/xray_config.json',
        binary: 'xray',
        launchArgs: ['run', '-c', '/data/user/0/org.anticensor.vpn/files/configs/xray_config.json'],
        content: JSON.stringify(xray, null, 2)
      };
    }

    if (parseResult.protocol === 'HYSTERIA2') {
      const yaml = `server: ${parseResult.server}:${parseResult.port}
auth: "${parseResult.auth}"

socks5:
  listen: 127.0.0.1:${socksPort}
  timeout: 300

obfs:
  type: ${parseResult.obfsType}
  ${parseResult.obfsType}:
    password: "${parseResult.obfsPassword}"

tls:
  sni: ${parseResult.sni}
  insecure: ${parseResult.insecure}

bandwidth:
  up: ${parseResult.upMbps} mbps
  down: ${parseResult.downMbps} mbps

quic:
  initStreamReceiveWindow: 8388608
  maxStreamReceiveWindow: 8388608
  initConnReceiveWindow: 20971520
  maxConnReceiveWindow: 20971520
  maxIdleTimeout: 30s
  keepAlivePeriod: 10s
  disablePathMTUDiscovery: false

fastOpen: true`;

      return {
        fileName: 'hysteria2_config.yaml',
        filePath: 'context.filesDir/configs/hysteria2_config.yaml',
        binary: 'hysteria',
        launchArgs: ['client', '-c', '/data/user/0/org.anticensor.vpn/files/configs/hysteria2_config.yaml'],
        content: yaml
      };
    }

    if (parseResult.protocol === 'NAIVE_PROXY') {
      const naive = {
        listen: `socks://127.0.0.1:${socksPort}`,
        proxy: `${parseResult.networkType}://${encodeURIComponent(parseResult.username || '')}:${encodeURIComponent(parseResult.password || '')}@${parseResult.server}:${parseResult.port}`,
        padding: parseResult.padding,
        insecure_concurrency: 1,
        log: ''
      };

      return {
        fileName: 'naive_config.json',
        filePath: 'context.filesDir/configs/naive_config.json',
        binary: 'naive',
        launchArgs: ['/data/user/0/org.anticensor.vpn/files/configs/naive_config.json'],
        content: JSON.stringify(naive, null, 2)
      };
    }

    if (parseResult.protocol === 'TUIC') {
      const tuic = {
        relay: {
          server: `${parseResult.server}:${parseResult.port}`,
          uuid: parseResult.uuid,
          password: parseResult.password,
          certificates: [],
          udp_relay_mode: parseResult.udpRelayMode,
          zero_rtt_handshake: true,
          disable_sni: false,
          congestion_control: parseResult.congestionControl,
          heartbeat: '10s',
          sni: parseResult.sni,
          alpn: parseResult.alpn
        },
        local: {
          server: `127.0.0.1:${socksPort}`,
          dual_stack: false,
          max_packet_size: 1500
        },
        log_level: 'warn'
      };

      return {
        fileName: 'tuic_config.json',
        filePath: 'context.filesDir/configs/tuic_config.json',
        binary: 'tuic-client',
        launchArgs: ['-c', '/data/user/0/org.anticensor.vpn/files/configs/tuic_config.json'],
        content: JSON.stringify(tuic, null, 2)
      };
    }

    if (parseResult.protocol === 'AMNEZIA_WG') {
      const conf = `[Interface]
Address = ${parseResult.addressIpv4}
PrivateKey = ${parseResult.privateKey}
DNS = 1.1.1.1, 8.8.8.8
MTU = 1360
Jc = ${parseResult.jc}
Jmin = ${parseResult.jmin}
Jmax = ${parseResult.jmax}
S1 = ${parseResult.s1}
S2 = ${parseResult.s2}
H1 = ${parseResult.h1}
H2 = ${parseResult.h2}
H3 = ${parseResult.h3}
H4 = ${parseResult.h4}

[Peer]
PublicKey = ${parseResult.publicKey}
Endpoint = ${parseResult.server}:${parseResult.port}
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25`;

      return {
        fileName: 'amnezia_wg0.conf',
        filePath: 'context.filesDir/configs/amnezia_wg0.conf',
        binary: 'amneziawg-go',
        launchArgs: ['-f', '/data/user/0/org.anticensor.vpn/files/configs/amnezia_wg0.conf'],
        content: conf
      };
    }

    return null;
  }, [parseResult, socksPort]);

  const copyToClipboard = (text: string, type: 'config' | 'uri') => {
    navigator.clipboard.writeText(text);
    if (type === 'config') {
      setCopiedConfig(true);
      setTimeout(() => setCopiedConfig(false), 2000);
    } else {
      setCopiedUri(true);
      setTimeout(() => setCopiedUri(false), 2000);
    }
  };

  const handleSimulateSave = () => {
    setFileSimulatedSaved(true);
    setTimeout(() => setFileSimulatedSaved(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950/80 border border-indigo-500/20 rounded-xl p-6 shadow-xl">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                PHASE 4 DELIVERED
              </span>
              <span className="text-xs text-slate-400">UriParser.kt • ConfigGeneratorFactory.kt</span>
            </div>
            <h2 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
              <FileCode className="w-5 h-5 text-indigo-400" />
              Anti-Censorship URI Parsers &amp; Config Generators
            </h2>
            <p className="text-sm text-slate-300 mt-1 max-w-3xl">
              Parses raw share links and WireGuard configuration text into strongly-typed Kotlin data models,
              then synthesizes ready-to-execute configuration files into <code className="text-indigo-300">context.filesDir/configs/</code> with
              anti-censorship camouflage and local SOCKS5 loopback on <code className="text-emerald-300">127.0.0.1:10808</code>.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="bg-slate-950/80 px-4 py-2.5 rounded-lg border border-slate-700/60 text-right">
              <div className="text-[11px] text-slate-400 font-mono">Loopback SOCKS5 Port</div>
              <div className="text-base font-bold font-mono text-emerald-400">127.0.0.1:{socksPort}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Preset Selector Chips */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-2">
        {PRESETS.map((preset) => {
          const isSelected = preset.id === selectedPresetId;
          return (
            <button
              key={preset.id}
              onClick={() => handleSelectPreset(preset)}
              className={`text-left p-3 rounded-lg border transition-all text-xs flex flex-col justify-between ${
                isSelected
                  ? 'bg-indigo-950/60 border-indigo-500 text-white shadow-md ring-1 ring-indigo-500/50'
                  : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
              }`}
            >
              <div className="font-semibold truncate mb-1">{preset.name}</div>
              <span className="text-[10px] font-mono text-indigo-400">{preset.scheme}</span>
            </button>
          );
        })}
      </div>

      {/* Main Dual-Column Layout: Input / Parser on Left, Generated Config on Right */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: URI / Conf Input + Kotlin Model Inspection (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-slate-900/80 rounded-xl border border-slate-800 p-4 shadow-lg">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                <Terminal className="w-3.5 h-3.5 text-indigo-400" />
                Input Share Link or Configuration Text
              </label>
              <button
                onClick={() => copyToClipboard(inputText, 'uri')}
                className="text-[11px] text-slate-400 hover:text-white flex items-center gap-1 transition-colors"
              >
                {copiedUri ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                {copiedUri ? 'Copied' : 'Copy'}
              </button>
            </div>

            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              rows={6}
              className="w-full bg-slate-950/90 border border-slate-800 rounded-lg p-3 font-mono text-xs text-indigo-200 focus:outline-none focus:ring-1 focus:ring-indigo-500 resize-none"
              placeholder="Paste vless://, hysteria2://, naive+https://, tuic://, amneziawg://, or [Interface]..."
            />

            <p className="text-[11px] text-slate-400 mt-2">
              {activePreset.description}
            </p>
          </div>

          {/* Parsed Kotlin Model State */}
          <div className="bg-slate-900/80 rounded-xl border border-slate-800 p-4 shadow-lg space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <div className="text-xs font-bold text-slate-200 flex items-center gap-2">
                <Layers className="w-4 h-4 text-emerald-400" />
                UriParser.kt Result
              </div>
              {parseResult.valid ? (
                <span className="px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                  <CheckCircle className="w-3 h-3" />
                  {parseResult.kotlinModel}
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded text-[11px] font-mono font-medium bg-rose-500/10 text-rose-400 border border-rose-500/20 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" />
                  Parse Error
                </span>
              )}
            </div>

            {parseResult.valid ? (
              <div className="space-y-2 text-xs font-mono">
                <div className="grid grid-cols-2 gap-2 bg-slate-950/60 p-2.5 rounded border border-slate-800/80">
                  <div>
                    <span className="text-slate-500 block text-[10px]">TARGET SERVER</span>
                    <span className="text-white font-semibold">{parseResult.server}:{parseResult.port}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">CORE EXECUTABLE</span>
                    <span className="text-indigo-400 font-semibold">{parseResult.binary}</span>
                  </div>
                </div>

                <div className="space-y-1 pt-1">
                  <div className="text-[11px] font-semibold text-slate-300 flex items-center gap-1">
                    <Shield className="w-3 h-3 text-cyan-400" />
                    Anti-Censorship &amp; Obfuscation Parameters
                  </div>
                  <div className="space-y-1 bg-slate-950/80 p-2.5 rounded-lg border border-slate-800/60">
                    {parseResult.antiDpiDetails?.map((item: any, idx: number) => (
                      <div key={idx} className="flex items-center justify-between text-[11px] py-0.5 border-b border-slate-900 last:border-0">
                        <span className="text-slate-400">{item.label}:</span>
                        <span className="text-cyan-300 font-semibold truncate max-w-[200px]" title={item.val}>{item.val}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-rose-400 text-xs bg-rose-950/20 p-3 rounded border border-rose-900/30">
                {parseResult.error}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Generated Ready-to-Execute Config File (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          <div className="bg-slate-900/80 rounded-xl border border-slate-800 p-4 shadow-lg space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800 pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <FileCode className="w-4 h-4 text-indigo-400" />
                  <span className="text-sm font-bold text-white font-mono">
                    {generatedConfig?.fileName || 'config'}
                  </span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                    {generatedConfig?.binary}
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                  {generatedConfig?.filePath}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleSimulateSave}
                  className="px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/30 text-xs font-semibold flex items-center gap-1.5 transition-all"
                >
                  {fileSimulatedSaved ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      Saved to filesDir!
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
                      Write to context.filesDir
                    </>
                  )}
                </button>

                <button
                  onClick={() => generatedConfig && copyToClipboard(generatedConfig.content, 'config')}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors"
                >
                  {copiedConfig ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  {copiedConfig ? 'Copied' : 'Copy'}
                </button>
              </div>
            </div>

            {/* Subprocess Launch Command Preview */}
            {generatedConfig && (
              <div className="bg-slate-950/90 rounded-lg p-2.5 border border-slate-800/80 font-mono text-[11px] text-slate-300 flex items-center justify-between">
                <span className="text-slate-500">Launch Command:</span>
                <span className="text-amber-400 font-semibold">
                  {generatedConfig.binary} {generatedConfig.launchArgs.join(' ')}
                </span>
              </div>
            )}

            {/* Generated Code Preview */}
            <div className="relative">
              <pre className="bg-slate-950 p-4 rounded-lg font-mono text-xs text-slate-300 overflow-x-auto max-h-[460px] border border-slate-800 leading-relaxed">
                {generatedConfig ? generatedConfig.content : '// Waiting for valid proxy input...'}
              </pre>
            </div>

            <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
              <span>Verified Inbound: SOCKS5 127.0.0.1:10808</span>
              <span>Directs hev-socks5-tunnel traffic through native core</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
