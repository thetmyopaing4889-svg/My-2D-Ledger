import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Sparkles, CheckCircle2, Heart } from 'lucide-react';
import { soundFx } from '../utils/audio';

interface BathStageProps {
  onComplete: () => void;
  onBubbleCreate: () => void;
}

interface Bubble {
  id: number;
  x: number;
  y: number;
  size: number;
}

export const BathStage: React.FC<BathStageProps> = ({ onComplete, onBubbleCreate }) => {
  const [bubbles, setBubbles] = useState<Bubble[]>([
    { id: 1, x: 20, y: 30, size: 36 },
    { id: 2, x: 70, y: 25, size: 48 },
    { id: 3, x: 45, y: 60, size: 40 },
    { id: 4, x: 15, y: 70, size: 32 },
    { id: 5, x: 80, y: 65, size: 42 }
  ]);
  const [poppedCount, setPoppedCount] = useState(0);
  const [showerTurnedOn, setShowerTurnedOn] = useState(false);
  const [duckyQuacked, setDuckyQuacked] = useState(false);

  const handlePopBubble = (id: number) => {
    soundFx.playBubblePop();
    setBubbles(prev => prev.filter(b => b.id !== id));
    setPoppedCount(prev => prev + 1);
    onBubbleCreate();
  };

  const handleAddMoreBubbles = () => {
    soundFx.playSplash();
    const newBubbles: Bubble[] = Array.from({ length: 4 }).map((_, idx) => ({
      id: Date.now() + idx,
      x: Math.floor(Math.random() * 80 + 10),
      y: Math.floor(Math.random() * 70 + 10),
      size: Math.floor(Math.random() * 24 + 32)
    }));
    setBubbles(prev => [...prev, ...newBubbles]);
  };

  const handleToggleShower = () => {
    soundFx.playSplash();
    setShowerTurnedOn(true);
  };

  const handleDucky = () => {
    soundFx.playChime();
    setDuckyQuacked(true);
  };

  const isComplete = poppedCount >= 5 && showerTurnedOn;

  return (
    <div className="flex flex-col items-center gap-4 w-full max-w-md mx-auto p-4 rounded-3xl bg-cyan-50/80 border-2 border-cyan-200 shadow-xl backdrop-blur-sm">
      <div className="text-center">
        <h3 className="text-lg font-bold text-cyan-950 flex items-center justify-center gap-2 font-sans">
          <span>🛁</span>
          <span>ရေချိုး၊ ဆပ်ပြာတိုက် & ပူဖောင်းဖောက်မယ်</span>
        </h3>
        <p className="text-xs text-cyan-700 mt-1">
          ဆပ်ပြာပူဖောင်းလေးတွေကို လက်ချောင်းလေးနဲ့ ထိပြီး ဖောက်ပေးပါ!
        </p>
      </div>

      {/* Interactive Bathtub Soap Bubble Area */}
      <div className="relative w-full h-48 rounded-2xl bg-gradient-to-b from-sky-200/80 via-cyan-100 to-sky-300 border-2 border-cyan-300 shadow-inner overflow-hidden flex items-center justify-center">
        {/* Water wave texture */}
        <div className="absolute inset-x-0 bottom-0 h-16 bg-cyan-400/40 rounded-t-3xl backdrop-blur-xs flex items-center justify-center">
          <span className="text-cyan-800/60 text-xs font-bold font-sans">
            💧 ရေနွေးနွေးလေးနဲ့ ဆပ်ပြာမွှေးမွှေးလေး 💧
          </span>
        </div>

        {/* Rubber Ducky Interactive Toy */}
        <motion.button
          animate={{ y: [0, -6, 0], rotate: [-4, 4, -4] }}
          transition={{ repeat: Infinity, duration: 2, ease: 'easeInOut' }}
          onClick={handleDucky}
          className="absolute bottom-6 left-6 text-3xl cursor-pointer select-none filter drop-shadow-md z-10"
        >
          🐥
        </motion.button>

        {duckyQuacked && (
          <div className="absolute bottom-16 left-6 px-2 py-0.5 rounded-full bg-white text-[10px] font-bold text-amber-600 shadow border border-amber-200 z-10 animate-bounce">
            Quack! 💛
          </div>
        )}

        {/* Soap Bubbles Floating */}
        {bubbles.map(bubble => (
          <motion.div
            key={bubble.id}
            initial={{ scale: 0 }}
            animate={{
              scale: 1,
              y: [0, -8, 0],
              x: [0, 4, 0]
            }}
            transition={{ repeat: Infinity, duration: 2 + (bubble.id % 3) }}
            whileHover={{ scale: 1.15 }}
            onClick={() => handlePopBubble(bubble.id)}
            style={{
              left: `${bubble.x}%`,
              top: `${bubble.y}%`,
              width: `${bubble.size}px`,
              height: `${bubble.size}px`
            }}
            className="absolute rounded-full bg-gradient-to-tr from-white/70 via-pink-200/60 to-cyan-200/80 border-2 border-white/90 shadow-md backdrop-blur-xs cursor-pointer flex items-center justify-center z-10"
          >
            <div className="w-2 h-2 rounded-full bg-white absolute top-1 left-1" />
            <span className="text-xs">🫧</span>
          </motion.div>
        ))}

        {bubbles.length === 0 && (
          <div className="text-center z-10">
            <p className="text-xs font-bold text-cyan-900 mb-2">ပူဖောင်းတွေ အကုန်ဖောက်ပြီးပြီ!</p>
            <button
              onClick={handleAddMoreBubbles}
              className="px-3 py-1.5 rounded-xl bg-white/90 text-cyan-800 text-xs font-bold shadow-md hover:bg-white"
            >
              🫧 ဆပ်ပြာပူဖောင်း ထပ်ထုတ်မယ်
            </button>
          </div>
        )}
      </div>

      {/* Bath Tools: Shower & Soap Bar */}
      <div className="grid grid-cols-2 gap-3 w-full">
        <motion.button
          whileHover={{ scale: 1.04 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleToggleShower}
          className={`p-3 rounded-2xl border-2 flex items-center justify-center gap-2 transition-all ${
            showerTurnedOn
              ? 'bg-sky-100 border-sky-400 text-sky-950 shadow-md'
              : 'bg-white border-sky-200 text-sky-800'
          }`}
        >
          <span className="text-2xl">🚿</span>
          <div className="text-left">
            <div className="text-xs font-bold font-sans">
              {showerTurnedOn ? 'ရေပန်း ဖွင့်ပြီးပြီ' : 'ရေပန်း ဖွင့်မယ်'}
            </div>
            <div className="text-[10px] text-sky-600">Warm Shower Spray</div>
          </div>
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.04 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleAddMoreBubbles}
          className="p-3 rounded-2xl border-2 bg-white border-pink-200 text-pink-800 flex items-center justify-center gap-2 shadow-sm"
        >
          <span className="text-2xl">🧼</span>
          <div className="text-left">
            <div className="text-xs font-bold font-sans">ဆပ်ပြာမွှေး တိုက်မယ်</div>
            <div className="text-[10px] text-pink-600">Fragrant Rose Soap</div>
          </div>
        </motion.button>
      </div>

      {isComplete && (
        <motion.button
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          whileTap={{ scale: 0.95 }}
          onClick={onComplete}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-cyan-500 to-blue-500 text-white font-bold text-sm shadow-lg shadow-cyan-500/20 flex items-center justify-center gap-2"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>နောက်တစ်ဆင့် ဂါဝန်ဝဲဝဲလေး ရွေးဝတ်မယ် ➔</span>
        </motion.button>
      )}
    </div>
  );
};
