import React, { useState, useEffect, useRef } from 'react';
import { Power, Shield, Zap, Sparkles, Radio } from 'lucide-react';

interface QuantumPowerButtonProps {
  isConnected: boolean;
  isConnecting: boolean;
  onToggle: () => void;
  serverName?: string;
  downloadSpeed?: number;
  uploadSpeed?: number;
}

interface Particle {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  alpha: number;
  color: string;
  life: number;
  maxLife: number;
}

interface Shockwave {
  id: number;
  scale: number;
  alpha: number;
  color: string;
}

export const QuantumPowerButton: React.FC<QuantumPowerButtonProps> = ({
  isConnected,
  isConnecting,
  onToggle,
  serverName = 'VLESS Reality HK-01',
  downloadSpeed = 4.8,
  uploadSpeed = 1.2,
}) => {
  const [particles, setParticles] = useState<Particle[]>([]);
  const [shockwaves, setShockwaves] = useState<Shockwave[]>([]);
  const [isPowerSurging, setIsPowerSurging] = useState(false);
  const [surgeType, setSurgeType] = useState<'on' | 'off' | null>(null);
  const [powerLevel, setPowerLevel] = useState(isConnected ? 100 : 0);

  const prevConnectedRef = useRef(isConnected);

  // Trigger purple power shockwave & plasma burst whenever state transitions
  useEffect(() => {
    if (prevConnectedRef.current !== isConnected) {
      const type = isConnected ? 'on' : 'off';
      triggerPowerBlast(type);
      prevConnectedRef.current = isConnected;
    }
  }, [isConnected]);

  // Power blast generator (Click to Connect or Disconnect)
  const triggerPowerBlast = (type: 'on' | 'off') => {
    setIsPowerSurging(true);
    setSurgeType(type);

    // Create 3 concentric expanding purple shockwaves
    const newShockwaves: Shockwave[] = [
      { id: Date.now() + 1, scale: 0.8, alpha: 0.95, color: '#d8b4fe' },
      { id: Date.now() + 2, scale: 0.6, alpha: 0.75, color: '#c084fc' },
      { id: Date.now() + 3, scale: 0.4, alpha: 0.6, color: '#a855f7' }
    ];
    setShockwaves(newShockwaves);

    // Create 28 radial purple plasma energy particles
    const newParticles: Particle[] = [];
    const colors = ['#f3e8ff', '#e9d5ff', '#d8b4fe', '#c084fc', '#a855f7', '#9333ea', '#e879f9'];

    for (let i = 0; i < 28; i++) {
      const angle = (i / 28) * Math.PI * 2 + (Math.random() - 0.5) * 0.3;
      const speed = 2.5 + Math.random() * 4.5;
      newParticles.push({
        id: Date.now() + i,
        x: 0,
        y: 0,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size: 2.5 + Math.random() * 3.5,
        alpha: 1,
        color: colors[Math.floor(Math.random() * colors.length)],
        life: 0,
        maxLife: 35 + Math.floor(Math.random() * 20)
      });
    }
    setParticles(newParticles);

    // Reset power surge flash
    setTimeout(() => {
      setIsPowerSurging(false);
      setSurgeType(null);
    }, 700);
  };

  // Particle & Shockwave animation loop
  useEffect(() => {
    if (particles.length === 0 && shockwaves.length === 0) return;

    const interval = setInterval(() => {
      // Update shockwaves
      setShockwaves(prev =>
        prev
          .map(sw => ({
            ...sw,
            scale: sw.scale + 0.08,
            alpha: sw.alpha - 0.035
          }))
          .filter(sw => sw.alpha > 0.02)
      );

      // Update particles
      setParticles(prev =>
        prev
          .map(p => ({
            ...p,
            x: p.x + p.vx,
            y: p.y + p.vy,
            alpha: 1 - p.life / p.maxLife,
            life: p.life + 1
          }))
          .filter(p => p.life < p.maxLife)
      );
    }, 20);

    return () => clearInterval(interval);
  }, [particles.length, shockwaves.length]);

  // Click handler
  const handleClick = () => {
    const nextType = isConnected ? 'off' : 'on';
    triggerPowerBlast(nextType);
    onToggle();
  };

  return (
    <div className="relative flex flex-col items-center justify-center my-3 select-none">
      {/* 1. OUTER AMBIENT PURPLE POWER RADIANCE */}
      <div
        className={`absolute w-64 h-64 rounded-full blur-3xl pointer-events-none transition-all duration-700 ${
          isConnected
            ? 'bg-purple-600/35 scale-110'
            : isConnecting
            ? 'bg-purple-500/25 animate-pulse scale-100'
            : 'bg-purple-950/15 scale-75'
        }`}
      />

      {/* 2. CONTINUOUS CONCENTRIC PULSING POWER RINGS (WHEN ACTIVE) */}
      {isConnected && (
        <>
          <div className="absolute w-52 h-52 rounded-full border border-purple-500/30 animate-ping pointer-events-none opacity-40 duration-1000" />
          <div className="absolute w-44 h-44 rounded-full border border-purple-400/40 animate-pulse pointer-events-none" />
        </>
      )}

      {/* 3. DYNAMIC EXPLOSIVE PURPLE SHOCKWAVES ON CLICK */}
      {shockwaves.map(sw => (
        <div
          key={sw.id}
          className="absolute rounded-full pointer-events-none border-2 transition-transform"
          style={{
            width: '150px',
            height: '150px',
            transform: `scale(${sw.scale})`,
            borderColor: sw.color,
            opacity: sw.alpha,
            boxShadow: `0 0 24px ${sw.color}`
          }}
        />
      ))}

      {/* 4. RADIAL BURST OF PURPLE PLASMA PARTICLES */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-30">
        {particles.map(p => (
          <div
            key={p.id}
            className="absolute rounded-full pointer-events-none"
            style={{
              width: `${p.size}px`,
              height: `${p.size}px`,
              transform: `translate(${p.x}px, ${p.y}px)`,
              backgroundColor: p.color,
              opacity: p.alpha,
              boxShadow: `0 0 ${p.size * 2.5}px ${p.color}`
            }}
          />
        ))}
      </div>

      {/* 5. MAIN BIG CIRCULAR BUTTON CONTAINER (~160px) */}
      <div className="relative flex items-center justify-center">
        {/* Outer Rotating HUD Segmented Ring */}
        <svg
          className={`absolute w-44 h-44 pointer-events-none transition-all duration-700 ${
            isConnected
              ? 'animate-[spin_16s_linear_infinite]'
              : isConnecting
              ? 'animate-[spin_4s_linear_infinite]'
              : 'opacity-40'
          }`}
          viewBox="0 0 160 160"
        >
          {/* Outer dashed track */}
          <circle
            cx="80"
            cy="80"
            r="74"
            fill="none"
            stroke={isConnected ? '#c084fc' : '#581c87'}
            strokeWidth="1.8"
            strokeDasharray="6 8"
            strokeOpacity={isConnected ? '0.85' : '0.4'}
          />
          {/* Cardinal Ticks */}
          <line x1="80" y1="2" x2="80" y2="8" stroke="#d8b4fe" strokeWidth="2.5" strokeLinecap="round" />
          <line x1="80" y1="152" x2="80" y2="158" stroke="#d8b4fe" strokeWidth="2.5" strokeLinecap="round" />
          <line x1="2" y1="80" x2="8" y2="80" stroke="#d8b4fe" strokeWidth="2.5" strokeLinecap="round" />
          <line x1="152" y1="80" x2="158" y2="80" stroke="#d8b4fe" strokeWidth="2.5" strokeLinecap="round" />
        </svg>

        {/* Counter-Rotating Inner Cyber Ring */}
        <svg
          className={`absolute w-38 h-38 pointer-events-none transition-all duration-500 ${
            isConnected
              ? 'animate-[spin_10s_linear_infinite_reverse]'
              : isConnecting
              ? 'animate-[spin_2.5s_linear_infinite_reverse]'
              : 'opacity-30'
          }`}
          viewBox="0 0 140 140"
        >
          <circle
            cx="70"
            cy="70"
            r="66"
            fill="none"
            stroke={isConnected ? '#e879f9' : '#3b0764'}
            strokeWidth="2"
            strokeDasharray="22 14 8 14"
            strokeOpacity={isConnected ? '0.9' : '0.3'}
          />
        </svg>

        {/* 6. THE MASSIVE TACTICAL BUTTON ITSELF */}
        <button
          onClick={handleClick}
          disabled={isConnecting}
          className={`group relative z-20 w-36 h-36 rounded-full flex flex-col items-center justify-center transition-all duration-300 active:scale-90 border-2 cursor-pointer shadow-2xl ${
            isConnected
              ? 'bg-gradient-to-b from-[#1e0836] via-[#10041d] to-[#08020e] border-purple-400 shadow-[0_0_40px_rgba(168,85,247,0.7),inset_0_0_25px_rgba(192,132,252,0.4)]'
              : isConnecting
              ? 'bg-[#150524] border-purple-400 animate-pulse shadow-[0_0_30px_rgba(168,85,247,0.5)]'
              : 'bg-gradient-to-b from-[#140624] to-[#07020d] border-purple-900/80 text-purple-300/80 hover:border-purple-500 hover:text-white hover:shadow-[0_0_25px_rgba(168,85,247,0.4)]'
          }`}
        >
          {/* Inner Power Core Glow Flash */}
          <div
            className={`absolute inset-1 rounded-full pointer-events-none transition-opacity duration-300 ${
              isPowerSurging
                ? 'bg-gradient-to-r from-purple-400 via-fuchsia-300 to-purple-500 opacity-90 blur-sm'
                : isConnected
                ? 'bg-purple-600/15 opacity-100'
                : 'opacity-0'
            }`}
          />

          {/* Central High-Voltage Power Icon */}
          <div className="relative z-10 flex flex-col items-center justify-center">
            <div
              className={`p-2 rounded-full transition-all duration-300 ${
                isConnected
                  ? 'bg-purple-500/20 text-white drop-shadow-[0_0_16px_#c084fc]'
                  : isConnecting
                  ? 'text-purple-300 animate-spin drop-shadow-[0_0_12px_#a855f7]'
                  : 'text-purple-400 group-hover:text-purple-200 group-hover:scale-110'
              }`}
            >
              <Power className="w-10 h-10 stroke-[2.5]" />
            </div>

            {/* Tactical Label inside the Big Circle */}
            <span
              className={`mt-1 font-mono font-black text-xs tracking-widest uppercase transition-all ${
                isConnected
                  ? 'text-purple-200 drop-shadow-[0_0_8px_#c084fc]'
                  : isConnecting
                  ? 'text-purple-300 animate-pulse'
                  : 'text-purple-400/90 group-hover:text-purple-100'
              }`}
            >
              {isConnected ? 'DISENGAGE' : isConnecting ? 'LOCKING...' : 'CONNECT'}
            </span>

            {/* Small Subtext / Status Readout */}
            <span className="text-[9px] font-mono tracking-wider text-purple-400/70 mt-0.5">
              {isConnected ? 'ONLINE' : isConnecting ? 'PURPLE CORE' : 'TAP TO SHIELD'}
            </span>
          </div>

          {/* Glowing perimeter reflection */}
          <div className="absolute inset-x-4 top-2 h-6 bg-gradient-to-b from-purple-300/25 to-transparent rounded-t-full pointer-events-none" />
        </button>
      </div>

      {/* 7. BOTTOM POWER TELEMETRY PILL */}
      <div className="mt-3 flex items-center gap-2">
        <div
          className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-mono font-bold tracking-wider border shadow-md transition-all ${
            isConnected
              ? 'bg-purple-950/80 border-purple-500/60 text-purple-200 shadow-[0_0_15px_rgba(168,85,247,0.35)]'
              : isConnecting
              ? 'bg-purple-950/60 border-purple-600/50 text-purple-300'
              : 'bg-slate-950/80 border-purple-900/40 text-purple-400/80'
          }`}
        >
          <Zap
            className={`w-3 h-3 ${
              isConnected
                ? 'text-purple-400 animate-bounce'
                : isConnecting
                ? 'text-amber-400 animate-spin'
                : 'text-purple-600'
            }`}
          />
          <span>
            {isConnected
              ? `PURPLE POWER ONLINE // ${serverName}`
              : isConnecting
              ? 'CALIBRATING QUANTUM PURPLE CORE...'
              : 'PURPLE POWER // READY TO ENGAGE'}
          </span>
        </div>
      </div>
    </div>
  );
};
