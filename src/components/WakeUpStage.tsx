import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Sun, Sparkles, CheckCircle2, Heart } from 'lucide-react';
import { soundFx } from '../utils/audio';

interface WakeUpStageProps {
  onComplete: () => void;
  onWakeAction: () => void;
}

export const WakeUpStage: React.FC<WakeUpStageProps> = ({ onComplete, onWakeAction }) => {
  const [sunClicks, setSunClicks] = useState(0);
  const [blanketFolded, setBlanketFolded] = useState(false);
  const [windowOpen, setWindowOpen] = useState(false);

  const handleSunClick = () => {
    soundFx.playChime();
    setSunClicks(prev => {
      const next = prev + 1;
      if (next === 1) onWakeAction();
      return next;
    });
  };

  const handleWindowToggle = () => {
    soundFx.playSparkle();
    setWindowOpen(true);
  };

  const handleFoldBlanket = () => {
    soundFx.playChime();
    setBlanketFolded(true);
    if (sunClicks >= 1 && windowOpen) {
      setTimeout(onComplete, 900);
    }
  };

  return (
    <div className="flex flex-col items-center gap-4 w-full max-w-md mx-auto p-4 rounded-3xl bg-amber-50/80 border-2 border-amber-200 shadow-xl backdrop-blur-sm">
      <div className="text-center">
        <h3 className="text-lg font-bold text-amber-900 flex items-center justify-center gap-2 font-sans">
          <Sun className="w-5 h-5 text-amber-500 animate-spin" />
          <span>မနက်ခင်း နိုးထခြင်း (Good Morning)</span>
        </h3>
        <p className="text-xs text-amber-700 mt-1">
          နေမင်းကြီးကို နှိပ်ပြီး ပြတင်းပေါက်ဖွင့်ကာ သမီးလေး စောင်ခေါက်ပေးရအောင်!
        </p>
      </div>

      {/* Interactive Sun & Window */}
      <div className="grid grid-cols-2 gap-3 w-full">
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleSunClick}
          className={`p-4 rounded-2xl border-2 flex flex-col items-center justify-center gap-2 transition-all shadow-md ${
            sunClicks > 0 
              ? 'bg-amber-100 border-amber-400 text-amber-900 shadow-amber-200' 
              : 'bg-white border-amber-200 text-amber-800'
          }`}
        >
          <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-300 flex items-center justify-center shadow-lg animate-pulse">
            <Sun className="w-7 h-7 text-white" />
          </div>
          <span className="text-xs font-bold font-sans">
            {sunClicks > 0 ? '☀️ နေမင်းကြီး ထွက်လာပြီ!' : 'နေမင်းကြီးကို နှိပ်ပါ'}
          </span>
          <span className="text-[10px] text-amber-600 font-mono">Sunshine +{sunClicks}</span>
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleWindowToggle}
          className={`p-4 rounded-2xl border-2 flex flex-col items-center justify-center gap-2 transition-all shadow-md ${
            windowOpen 
              ? 'bg-sky-100 border-sky-400 text-sky-900 shadow-sky-200' 
              : 'bg-white border-sky-200 text-sky-800'
          }`}
        >
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-400 to-indigo-300 flex items-center justify-center shadow-lg text-2xl">
            {windowOpen ? '🪟' : '🏡'}
          </div>
          <span className="text-xs font-bold font-sans">
            {windowOpen ? '✨ လေနုအေး ဝင်လာပြီ' : 'ပြတင်းပေါက် ဖွင့်ပါ'}
          </span>
          <span className="text-[10px] text-sky-600 font-mono">Fresh Air Breeze</span>
        </motion.button>
      </div>

      {/* Fold Blanket Action */}
      <motion.button
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.96 }}
        onClick={handleFoldBlanket}
        className={`w-full py-3.5 px-4 rounded-2xl border-2 flex items-center justify-between transition-all shadow-md ${
          blanketFolded
            ? 'bg-emerald-100 border-emerald-400 text-emerald-900 shadow-emerald-200'
            : 'bg-gradient-to-r from-pink-400 to-rose-400 border-pink-300 text-white'
        }`}
      >
        <div className="flex items-center gap-3">
          <span className="text-2xl">{blanketFolded ? '🌸' : '🛏️'}</span>
          <div className="text-left">
            <div className="text-xs font-bold font-sans">
              {blanketFolded ? 'စောင်လေး သေသပ်စွာ ခေါက်ပြီးပါပြီ!' : 'သမီးလေး စောင်ခေါက်ပေးမယ်'}
            </div>
            <div className="text-[10px] opacity-80">
              {blanketFolded ? 'Good job, sweet girl!' : 'Tap to fold princess blanket'}
            </div>
          </div>
        </div>
        {blanketFolded ? (
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
        ) : (
          <Sparkles className="w-5 h-5 text-amber-200 animate-spin" />
        )}
      </motion.button>

      {blanketFolded && windowOpen && (
        <motion.button
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          whileTap={{ scale: 0.95 }}
          onClick={onComplete}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-sm shadow-lg shadow-orange-500/20 flex items-center justify-center gap-2"
        >
          <Heart className="w-4 h-4 fill-white text-white" />
          <span>နောက်တစ်ဆင့် သွားတိုက် မျက်နှာသစ်မယ် ➔</span>
        </motion.button>
      )}
    </div>
  );
};
