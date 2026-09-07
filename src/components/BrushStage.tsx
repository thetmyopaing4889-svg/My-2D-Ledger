import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Sparkles, CheckCircle, Droplets } from 'lucide-react';
import { soundFx } from '../utils/audio';

interface BrushStageProps {
  onComplete: () => void;
  onBrushAction: () => void;
  onThanakaApply: () => void;
}

export const BrushStage: React.FC<BrushStageProps> = ({ onComplete, onBrushAction, onThanakaApply }) => {
  const [brushProgress, setBrushProgress] = useState(0);
  const [faceWashed, setFaceWashed] = useState(false);
  const [thanakaApplied, setThanakaApplied] = useState(false);

  const handleBrushClick = () => {
    soundFx.playBrush();
    onBrushAction();
    setBrushProgress(prev => Math.min(100, prev + 25));
  };

  const handleWashFace = () => {
    soundFx.playSplash();
    setFaceWashed(true);
  };

  const handleApplyThanaka = () => {
    soundFx.playSparkle();
    setThanakaApplied(true);
    onThanakaApply();
  };

  const isDone = brushProgress >= 100 && faceWashed && thanakaApplied;

  return (
    <div className="flex flex-col items-center gap-4 w-full max-w-md mx-auto p-4 rounded-3xl bg-pink-50/80 border-2 border-pink-200 shadow-xl backdrop-blur-sm">
      <div className="text-center">
        <h3 className="text-lg font-bold text-pink-950 flex items-center justify-center gap-2 font-sans">
          <span>🪥</span>
          <span>မျက်နှာသစ်၊ သွားတိုက် & သနပ်ခါးလူးမယ်</span>
        </h3>
        <p className="text-xs text-pink-700 mt-1">
          သွားဖြူဖြူလေးဖြစ်အောင် တိုက်ပြီး ပါးဖောင်းဖောင်းလေးမှာ သနပ်ခါးလိမ်းပေးရအောင်!
        </p>
      </div>

      {/* Brush Teeth Interactive Meter */}
      <div className="w-full p-4 rounded-2xl bg-white border border-pink-200 shadow-sm space-y-2">
        <div className="flex items-center justify-between text-xs font-bold text-pink-900">
          <span className="flex items-center gap-1.5">
            <span>🪥 သွားတိုက်ခြင်း</span>
            <span className="text-[10px] text-pink-500 font-normal">(Brushing Teeth)</span>
          </span>
          <span className="font-mono text-pink-600">{brushProgress}%</span>
        </div>

        {/* Progress Bar */}
        <div className="w-full h-3.5 rounded-full bg-pink-100 overflow-hidden border border-pink-200">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${brushProgress}%` }}
            className="h-full bg-gradient-to-r from-pink-400 via-rose-400 to-emerald-400"
          />
        </div>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleBrushClick}
          className={`w-full py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all ${
            brushProgress >= 100
              ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
              : 'bg-gradient-to-r from-pink-500 to-rose-500 text-white shadow-md shadow-pink-500/20'
          }`}
        >
          <span>{brushProgress >= 100 ? '✨ သွားလေးတွေ ဖြူဝင်းသွားပြီ!' : '🪥 သွားပွတ်တံလေးနဲ့ ပွတ်တိုက်ပါ (Tap to Brush)'}</span>
        </motion.button>
      </div>

      {/* Wash Face & Thanaka Apply Row */}
      <div className="grid grid-cols-2 gap-3 w-full">
        {/* Face Wash */}
        <motion.button
          whileHover={{ scale: 1.04 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleWashFace}
          className={`p-3.5 rounded-2xl border-2 flex flex-col items-center justify-center gap-2 text-center transition-all ${
            faceWashed
              ? 'bg-sky-100 border-sky-400 text-sky-900 shadow-sky-200'
              : 'bg-white border-sky-200 text-sky-800'
          }`}
        >
          <div className="w-10 h-10 rounded-full bg-sky-400/20 flex items-center justify-center text-xl">
            <Droplets className="w-5 h-5 text-sky-600" />
          </div>
          <div className="text-xs font-bold font-sans">
            {faceWashed ? '💧 မျက်နှာလေး သန့်ပြီ' : 'မျက်နှာသစ်ပေးမယ်'}
          </div>
          <div className="text-[10px] text-sky-600">Fresh Clean Water</div>
        </motion.button>

        {/* Myanmar Thanaka */}
        <motion.button
          whileHover={{ scale: 1.04 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleApplyThanaka}
          className={`p-3.5 rounded-2xl border-2 flex flex-col items-center justify-center gap-2 text-center transition-all ${
            thanakaApplied
              ? 'bg-amber-100 border-amber-400 text-amber-900 shadow-amber-200'
              : 'bg-white border-amber-200 text-amber-800'
          }`}
        >
          <div className="w-10 h-10 rounded-full bg-amber-400/20 flex items-center justify-center text-xl">
            🪵
          </div>
          <div className="text-xs font-bold font-sans">
            {thanakaApplied ? '🪵 သနပ်ခါး လိမ်းပြီးပြီ' : 'သနပ်ခါးလေး လိမ်းမယ်'}
          </div>
          <div className="text-[10px] text-amber-600">Golden Thanaka Paste</div>
        </motion.button>
      </div>

      {isDone && (
        <motion.button
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          whileTap={{ scale: 0.95 }}
          onClick={onComplete}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-bold text-sm shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
        >
          <CheckCircle className="w-4 h-4" />
          <span>နောက်တစ်ဆင့် ရေချိုး ဆပ်ပြာတိုက်မယ် ➔</span>
        </motion.button>
      )}
    </div>
  );
};
