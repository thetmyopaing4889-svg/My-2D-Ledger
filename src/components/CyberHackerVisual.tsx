import React, { useEffect, useRef, useState } from 'react';
import { Shield, Zap, Terminal, Activity, Radio, Cpu, Waves } from 'lucide-react';

interface CyberHackerVisualProps {
  isConnected: boolean;
  isConnecting: boolean;
  downloadSpeed?: number;
  uploadSpeed?: number;
  activeServerName?: string;
  onPowerClick?: () => void;
}

interface WaveConfig {
  frequency: number;
  amplitude: number;
  speed: number;
  phase: number;
  color: string;
  fillColor: string;
  lineWidth: number;
}

interface FloatingDigit {
  x: number;
  y: number;
  vy: number;
  val: string;
  alpha: number;
  size: number;
  layer: number;
}

interface MistParticle {
  x: number;
  y: number;
  radius: number;
  alpha: number;
  maxAlpha: number;
  vy: number;
  vx: number;
  growth: number;
  life: number;
  maxLife: number;
}

export const CyberHackerVisual: React.FC<CyberHackerVisualProps> = ({
  isConnected,
  isConnecting,
  downloadSpeed = 4.8,
  uploadSpeed = 1.2,
  activeServerName = 'VLESS Reality HK-01',
  onPowerClick
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  // Waveform visualization modes: Harmonic Sine Spectrum vs 3D Perspective Grid
  const [waveMode, setWaveMode] = useState<'spectral' | 'mesh'>('spectral');
  const [cipherHex, setCipherHex] = useState('0x9F4C...D81E');
  const [frequencyHz, setFrequencyHz] = useState(432.8);
  const [jitter, setJitter] = useState(0.4);

  // Mouse / Touch 3D tilt tracking
  const tiltRef = useRef({ x: 0, y: 0 });
  const mousePosRef = useRef({ x: 0.5, y: 0.5 });

  // Dynamic cipher updates and frequency oscillation
  useEffect(() => {
    const timer = setInterval(() => {
      const hex = '0123456789ABCDEF';
      let r1 = '';
      let r2 = '';
      for (let i = 0; i < 4; i++) r1 += hex[Math.floor(Math.random() * hex.length)];
      for (let i = 0; i < 4; i++) r2 += hex[Math.floor(Math.random() * hex.length)];
      setCipherHex(`0x${r1}...${r2}`);

      const baseHz = isConnected ? 864.2 : isConnecting ? 512.4 : 128.0;
      setFrequencyHz(+(baseHz + (Math.random() - 0.5) * 12).toFixed(1));
      setJitter(+(0.2 + Math.random() * 0.4).toFixed(2));
    }, 850);
    return () => clearInterval(timer);
  }, [isConnected, isConnecting]);

  // Pointer move handlers
  const handlePointer = (cx: number, cy: number) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const nx = Math.max(0, Math.min(1, (cx - rect.left) / rect.width));
    const ny = Math.max(0, Math.min(1, (cy - rect.top) / rect.height));
    mousePosRef.current = { x: nx, y: ny };
    tiltRef.current = {
      x: (ny - 0.5) * 14,
      y: -(nx - 0.5) * 16
    };
  };

  // Main Canvas Render Loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;
    let width = (canvas.width = canvas.offsetWidth || 340);
    let height = (canvas.height = canvas.offsetHeight || 260);

    const onResize = () => {
      if (!canvas) return;
      width = canvas.width = canvas.offsetWidth || 340;
      height = canvas.height = canvas.offsetHeight || 260;
    };
    window.addEventListener('resize', onResize);

    // Dynamic Digital Matrix Streams & Cryptographic Data Columns
    const digits: FloatingDigit[] = [];
    const hexPool = [
      '01', 'FF', '7F', 'A9', 'C4', 'E2', '3B', '8D', '10', '00',
      '0xFA', '0x1C', 'λ', 'Ω', 'Ψ', 'Σ', '101', '011', '110', '001',
      'AES', 'GCM', 'X25519', 'TLS1.3', 'CHA'
    ];

    for (let i = 0; i < 35; i++) {
      digits.push({
        x: Math.random() * width,
        y: Math.random() * height,
        vy: -(0.5 + Math.random() * 1.4),
        val: hexPool[Math.floor(Math.random() * hexPool.length)],
        alpha: 0.15 + Math.random() * 0.65,
        size: 8 + Math.floor(Math.random() * 4),
        layer: Math.random() > 0.5 ? 1 : 2
      });
    }

    // Volumetric Neon Purple Vapor / Mist rising from bottom
    const mistParticles: MistParticle[] = [];
    for (let i = 0; i < 22; i++) {
      mistParticles.push({
        x: Math.random() * width,
        y: Math.random() * height,
        radius: 20 + Math.random() * 40,
        alpha: 0.01,
        maxAlpha: 0.07 + Math.random() * 0.09,
        vy: -(0.3 + Math.random() * 0.7),
        vx: (Math.random() - 0.5) * 0.4,
        growth: 0.25 + Math.random() * 0.35,
        life: Math.random() * 200,
        maxLife: 200 + Math.random() * 100
      });
    }

    let frame = 0;

    const render = () => {
      frame++;
      ctx.clearRect(0, 0, width, height);

      // 1. Deep Midnight Background with Shifting Ultraviolet / Neon Purple Aurora
      const bgGrad = ctx.createRadialGradient(
        width * (0.3 + mousePosRef.current.x * 0.4),
        height * 0.5,
        15,
        width * 0.5,
        height * 0.5,
        width * 0.75
      );
      const pulse = 0.22 + Math.sin(frame * 0.02) * 0.08;
      bgGrad.addColorStop(0, `rgba(168, 85, 247, ${pulse})`); // Neon Purple #a855f7
      bgGrad.addColorStop(0.45, `rgba(126, 34, 206, ${pulse * 0.55})`); // Deep Purple #7e22ce
      bgGrad.addColorStop(0.8, 'rgba(30, 11, 54, 0.45)');
      bgGrad.addColorStop(1, 'rgba(2, 4, 10, 0)');
      ctx.fillStyle = bgGrad;
      ctx.fillRect(0, 0, width, height);

      // 2. Fine High-Tech Cyber Waveform Grid Lines (Horizontal and Vertical Rulers)
      ctx.strokeStyle = 'rgba(168, 85, 247, 0.07)';
      ctx.lineWidth = 1;
      const gridStep = 24;
      for (let x = 0; x < width; x += gridStep) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
        ctx.stroke();
      }
      for (let y = 0; y < height; y += gridStep) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
        ctx.stroke();
      }

      // 3. Volumetric Purple Convection Mist (Rising up gracefully)
      if (frame % 6 === 0 && mistParticles.length < 28) {
        mistParticles.push({
          x: width * 0.1 + Math.random() * (width * 0.8),
          y: height + 10,
          radius: 20 + Math.random() * 35,
          alpha: 0.01,
          maxAlpha: 0.06 + Math.random() * 0.08,
          vy: -(0.35 + Math.random() * 0.65),
          vx: (Math.random() - 0.5) * 0.35,
          growth: 0.25 + Math.random() * 0.3,
          life: 0,
          maxLife: 220 + Math.random() * 90
        });
      }

      for (let i = mistParticles.length - 1; i >= 0; i--) {
        const m = mistParticles[i];
        m.life++;
        m.y += m.vy;
        m.x += m.vx + Math.sin(frame * 0.015 + m.y * 0.01) * 0.35;
        m.radius += m.growth;

        const progress = m.life / m.maxLife;
        if (progress < 0.2) {
          m.alpha = (progress / 0.2) * m.maxAlpha;
        } else {
          m.alpha = (1 - (progress - 0.2) / 0.8) * m.maxAlpha;
        }

        if (progress >= 1 || m.y < -m.radius) {
          mistParticles.splice(i, 1);
          continue;
        }

        const mistGrad = ctx.createRadialGradient(m.x, m.y, 0, m.x, m.y, m.radius);
        mistGrad.addColorStop(0, `rgba(192, 132, 252, ${Math.max(0, m.alpha)})`);
        mistGrad.addColorStop(0.6, `rgba(147, 51, 234, ${Math.max(0, m.alpha * 0.4)})`);
        mistGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

        ctx.fillStyle = mistGrad;
        ctx.beginPath();
        ctx.arc(m.x, m.y, m.radius, 0, Math.PI * 2);
        ctx.fill();
      }

      // 4. Streaming Digital Numbers & Cryptographic Matrix Telemetry
      ctx.font = '9px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace';
      for (let i = 0; i < digits.length; i++) {
        const d = digits[i];
        d.y += d.vy;

        // Reset if reached top
        if (d.y < -15) {
          d.y = height + 10;
          d.x = Math.random() * width;
          d.val = hexPool[Math.floor(Math.random() * hexPool.length)];
          d.alpha = 0.2 + Math.random() * 0.7;
        }

        // Color tone: Radiant Neon Purple with bright electric lilac accents
        if (d.layer === 1) {
          ctx.fillStyle = `rgba(216, 180, 254, ${d.alpha * 0.75})`; // #d8b4fe
        } else {
          ctx.fillStyle = `rgba(168, 85, 247, ${d.alpha * 0.55})`; // #a855f7
        }
        ctx.fillText(d.val, d.x, d.y);
      }

      // 5. ANIMATED WAVE LINES / OSCILLATING DIGITAL FREQUENCY WAVES
      const baseCenterY = height * 0.54;
      const speedMultiplier = isConnected ? 1.4 : isConnecting ? 2.2 : 0.6;
      const dynamicAmp = isConnected
        ? Math.min(38, 14 + downloadSpeed * 2.8)
        : isConnecting
        ? 28
        : 10;

      if (waveMode === 'spectral') {
        // --- MULTI-LAYERED HARMONIC CYBER WAVEFORMS ---
        const waves: WaveConfig[] = [
          // Background Deep Ambient Purple Wave
          {
            frequency: 0.012,
            amplitude: dynamicAmp * 1.35,
            speed: 0.024 * speedMultiplier,
            phase: 0,
            color: 'rgba(147, 51, 234, 0.45)', // #9333ea
            fillColor: 'rgba(126, 34, 206, 0.12)',
            lineWidth: 1.4
          },
          // Secondary Magenta-Violet Wave (Carrier Wave)
          {
            frequency: 0.022,
            amplitude: dynamicAmp * 0.95,
            speed: -0.032 * speedMultiplier,
            phase: Math.PI * 0.45,
            color: 'rgba(192, 132, 252, 0.75)', // #c084fc
            fillColor: 'rgba(168, 85, 247, 0.16)',
            lineWidth: 2.0
          },
          // Primary Ultra-Sharp Electric Violet Laser Beam Wave (Signal Pulse)
          {
            frequency: 0.035,
            amplitude: dynamicAmp * 0.75,
            speed: 0.045 * speedMultiplier,
            phase: Math.PI * 0.85,
            color: '#f3e8ff', // #f3e8ff (Bright white-violet core)
            fillColor: 'rgba(192, 132, 252, 0.22)',
            lineWidth: 2.6
          },
          // Micro-Ripple High Frequency Data Wave (Quantum Noise)
          {
            frequency: 0.065,
            amplitude: dynamicAmp * 0.35,
            speed: 0.065 * speedMultiplier,
            phase: Math.PI * 1.2,
            color: 'rgba(232, 121, 249, 0.9)', // #e879f9
            fillColor: 'rgba(0, 0, 0, 0)',
            lineWidth: 1.2
          }
        ];

        // Render each wave with dynamic bezier and gradient area
        waves.forEach((w, waveIdx) => {
          ctx.save();
          ctx.beginPath();

          const points: { x: number; y: number }[] = [];
          const step = 6;

          for (let x = 0; x <= width + step; x += step) {
            // Complex multi-frequency wave equation with noise
            const t = frame * w.speed;
            const primaryHarmonic = Math.sin(x * w.frequency + t + w.phase);
            const secondaryHarmonic = Math.cos(x * (w.frequency * 0.5) - t * 0.7) * 0.45;
            const tertiaryNoise = Math.sin(x * 0.08 + t * 2) * 0.15;

            // Envelope tapering at left and right edges for sleek studio finish
            const envelope = Math.sin((x / width) * Math.PI);
            const y = baseCenterY + (primaryHarmonic + secondaryHarmonic + tertiaryNoise) * w.amplitude * envelope;

            points.push({ x, y });
          }

          // Draw fill under wave
          if (w.fillColor !== 'rgba(0, 0, 0, 0)') {
            ctx.beginPath();
            ctx.moveTo(0, height);
            ctx.lineTo(points[0].x, points[0].y);
            for (let i = 1; i < points.length; i++) {
              ctx.lineTo(points[i].x, points[i].y);
            }
            ctx.lineTo(width, height);
            ctx.closePath();

            const areaGrad = ctx.createLinearGradient(0, baseCenterY - w.amplitude, 0, height);
            areaGrad.addColorStop(0, w.fillColor);
            areaGrad.addColorStop(1, 'rgba(15, 5, 29, 0)');
            ctx.fillStyle = areaGrad;
            ctx.fill();
          }

          // Draw Glowing Wave Stroke
          ctx.beginPath();
          ctx.moveTo(points[0].x, points[0].y);
          for (let i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].x, points[i].y);
          }

          // Anamorphic Glow Filter on Primary Wave
          if (waveIdx === 2) {
            ctx.shadowColor = '#c084fc';
            ctx.shadowBlur = 14;
          } else {
            ctx.shadowBlur = 0;
          }

          ctx.strokeStyle = w.color;
          ctx.lineWidth = w.lineWidth;
          ctx.stroke();
          ctx.restore();

          // Particle Nodes at peak amplitudes
          if (waveIdx === 2 && isConnected) {
            for (let i = 4; i < points.length - 4; i += 7) {
              const pt = points[i];
              ctx.fillStyle = '#ffffff';
              ctx.beginPath();
              ctx.arc(pt.x, pt.y, 2.2, 0, Math.PI * 2);
              ctx.fill();

              // Vertical radar tick
              ctx.strokeStyle = 'rgba(216, 180, 254, 0.4)';
              ctx.lineWidth = 0.8;
              ctx.beginPath();
              ctx.moveTo(pt.x, pt.y - 8);
              ctx.lineTo(pt.x, pt.y + 8);
              ctx.stroke();
            }
          }
        });

        // Dynamic Laser Frequency Horizon Beam through center
        ctx.save();
        const centerLineY = baseCenterY;
        const beamGrad = ctx.createLinearGradient(0, centerLineY, width, centerLineY);
        beamGrad.addColorStop(0, 'rgba(168, 85, 247, 0)');
        beamGrad.addColorStop(0.25, 'rgba(192, 132, 252, 0.25)');
        beamGrad.addColorStop(0.5, 'rgba(243, 232, 255, 0.85)');
        beamGrad.addColorStop(0.75, 'rgba(192, 132, 252, 0.25)');
        beamGrad.addColorStop(1, 'rgba(168, 85, 247, 0)');

        ctx.strokeStyle = beamGrad;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(0, centerLineY);
        ctx.lineTo(width, centerLineY);
        ctx.stroke();
        ctx.restore();

      } else {
        // --- 3D PERSPECTIVE CYBER MESH / SYNTHWAVE DATA WIRE GRID ---
        const horizonY = height * 0.32;
        const fov = 160;
        const time = frame * 0.035 * speedMultiplier;

        ctx.save();
        ctx.strokeStyle = 'rgba(168, 85, 247, 0.35)';
        ctx.lineWidth = 1.0;

        // Perspective longitudinal lines
        const rays = 18;
        for (let r = 0; r <= rays; r++) {
          const u = r / rays;
          const bottomX = (u - 0.5) * width * 2.2 + width * 0.5;
          ctx.beginPath();
          ctx.moveTo(width * 0.5 + (u - 0.5) * 40, horizonY);
          ctx.lineTo(bottomX, height + 10);
          ctx.stroke();
        }

        // Horizontal moving undulating grid lines
        const numGridLines = 14;
        for (let i = 0; i < numGridLines; i++) {
          const offset = ((i + time) % numGridLines) / numGridLines;
          const y = horizonY + Math.pow(offset, 1.9) * (height - horizonY);
          const alpha = Math.min(1, offset * 1.5) * 0.7;

          ctx.strokeStyle = `rgba(216, 180, 254, ${alpha})`;
          ctx.beginPath();

          for (let x = 0; x <= width; x += 12) {
            // Wave pulse traveling through mesh
            const waveOffset = Math.sin(x * 0.04 + frame * 0.05) * (14 * offset);
            if (x === 0) ctx.moveTo(x, y + waveOffset);
            else ctx.lineTo(x, y + waveOffset);
          }
          ctx.stroke();
        }
        ctx.restore();
      }

      // 6. TACTICAL RETICLES & OSCILLOSCOPE READOUT OVERLAYS
      ctx.strokeStyle = 'rgba(192, 132, 252, 0.45)';
      ctx.lineWidth = 1;
      const corner = 12;
      // Top Left Corner
      ctx.beginPath();
      ctx.moveTo(10, 10 + corner);
      ctx.lineTo(10, 10);
      ctx.lineTo(10 + corner, 10);
      ctx.stroke();
      // Top Right Corner
      ctx.beginPath();
      ctx.moveTo(width - 10 - corner, 10);
      ctx.lineTo(width - 10, 10);
      ctx.lineTo(width - 10, 10 + corner);
      ctx.stroke();
      // Bottom Left Corner
      ctx.beginPath();
      ctx.moveTo(10, height - 10 - corner);
      ctx.lineTo(10, height - 10);
      ctx.lineTo(10 + corner, height - 10);
      ctx.stroke();
      // Bottom Right Corner
      ctx.beginPath();
      ctx.moveTo(width - 10 - corner, height - 10);
      ctx.lineTo(width - 10, height - 10);
      ctx.lineTo(width - 10, height - 10 - corner);
      ctx.stroke();

      animId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener('resize', onResize);
    };
  }, [isConnected, isConnecting, waveMode, downloadSpeed, uploadSpeed]);

  return (
    <div
      ref={containerRef}
      onMouseMove={e => handlePointer(e.clientX, e.clientY)}
      onTouchMove={e => e.touches[0] && handlePointer(e.touches[0].clientX, e.touches[0].clientY)}
      className="relative w-full rounded-2xl overflow-hidden select-none bg-[#030208] border border-purple-500/30 shadow-[0_20px_50px_rgba(25,10,40,0.95)] p-3 flex flex-col items-center justify-center transition-all duration-300"
    >
      {/* 1. Ultraviolet Scanline Overlay */}
      <div
        className="absolute inset-0 pointer-events-none opacity-[0.05] z-10"
        style={{
          backgroundImage:
            'linear-gradient(rgba(168, 85, 247, 0.4) 1px, transparent 1px), linear-gradient(90deg, rgba(168, 85, 247, 0.4) 1px, transparent 1px)',
          backgroundSize: '20px 20px'
        }}
      />

      {/* 2. Top Telemetry & Frequency Header */}
      <div className="relative z-20 w-full flex items-center justify-between px-1 mb-1 text-[10px] font-mono">
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-purple-400 shadow-[0_0_10px_#c084fc] animate-pulse" />
          <span className="text-purple-200 font-bold tracking-widest uppercase text-[10px]">
            QUANTUM OSCILLOSCOPE // {frequencyHz} MHz
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          {/* Wave Mode Switcher */}
          <button
            onClick={() => setWaveMode(m => (m === 'spectral' ? 'mesh' : 'spectral'))}
            className="flex items-center gap-1 px-2.5 py-0.5 rounded bg-purple-950/70 text-purple-300 border border-purple-700/60 hover:bg-purple-900/80 hover:text-white text-[9px] transition-all"
            title="Toggle Waveform Type"
          >
            <Waves className="w-2.5 h-2.5 text-purple-400" />
            <span>{waveMode === 'spectral' ? 'HARMONIC' : '3D MESH'}</span>
          </button>

          {/* Cipher Metric */}
          <span className="px-2 py-0.5 rounded bg-purple-950/60 text-purple-300 border border-purple-800/60 text-[9px] tracking-wider">
            {cipherHex}
          </span>
        </div>
      </div>

      {/* 3. The Living Digital Frequency Canvas */}
      <div className="relative z-10 w-full h-[235px] flex items-center justify-center">
        <canvas
          ref={canvasRef}
          className="w-full h-full block rounded-xl"
        />

        {/* Live Tunnel Security Telemetry Badge */}
        <div
          className="absolute bottom-2.5 z-20 px-3.5 py-1 rounded-full text-[9px] font-mono font-bold tracking-wider flex items-center gap-2 shadow-2xl backdrop-blur-md border transition-all"
          style={{
            background: 'rgba(8, 2, 16, 0.88)',
            borderColor: '#a855f7',
            color: '#e9d5ff'
          }}
        >
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              isConnected
                ? 'bg-purple-400 shadow-[0_0_8px_#c084fc] animate-ping'
                : isConnecting
                ? 'bg-amber-400 animate-spin'
                : 'bg-rose-500'
            }`}
          />
          <span className="uppercase tracking-wider">
            {isConnected
              ? `QUANTUM TUNNEL ACTIVE // ${activeServerName}`
              : isConnecting
              ? 'CALIBRATING FREQUENCY LIFELINE...'
              : 'CARRIER WAVE DORMANT // READY'}
          </span>
          <span className="text-[8px] text-purple-400 border-l border-purple-800 pl-2">
            JITTER: {jitter}ms
          </span>
        </div>
      </div>

      {/* 4. Action Command Console (Neon Purple Styling) */}
      {onPowerClick && (
        <div className="relative z-20 w-full mt-2 flex items-center justify-between pt-2 border-t border-purple-900/50">
          <div className="flex items-center gap-2 text-[10px] font-mono text-purple-300">
            <Activity className="w-3.5 h-3.5 text-purple-400" />
            {isConnected ? (
              <span className="text-purple-200 font-bold">
                ▲ {downloadSpeed} MB/s · ▼ {uploadSpeed} MB/s
              </span>
            ) : (
              <span className="text-purple-400/80">CARRIER FREQUENCY: STANDBY</span>
            )}
          </div>

          <button
            onClick={onPowerClick}
            className={`px-4 py-1.5 rounded-xl text-xs font-mono font-bold flex items-center gap-1.5 transition-all active:scale-95 border ${
              isConnected
                ? 'bg-rose-950/40 text-rose-300 border-rose-600/40 hover:bg-rose-900/50'
                : isConnecting
                ? 'bg-amber-950/40 text-amber-300 border-amber-600/40'
                : 'bg-purple-600 hover:bg-purple-500 text-white border-purple-400 shadow-[0_0_15px_rgba(168,85,247,0.5)]'
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            <span className="tracking-wider">
              {isConnected ? 'DISENGAGE' : isConnecting ? 'LOCKING WAVE...' : 'ACTIVATE WAVE'}
            </span>
          </button>
        </div>
      )}
    </div>
  );
};
