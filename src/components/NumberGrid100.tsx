import React, { useState } from 'react';
import { formatTwoDigit } from '../utils/parser';
import { LedgerEntry } from '../types';
import { Eye, TrendingUp, AlertCircle, Sparkles } from 'lucide-react';

interface NumberGrid100Props {
  totals: Record<string, number>;
  entries: LedgerEntry[];
  limitPerNumber: number;
  onSelectNumber: (num: string) => void;
  selectedNumber: string | null;
  winningNumber: string | null;
}

export const NumberGrid100: React.FC<NumberGrid100Props> = ({
  totals,
  entries,
  limitPerNumber,
  onSelectNumber,
  selectedNumber,
  winningNumber,
}) => {
  const [filterMode, setFilterMode] = useState<'all' | 'sold' | 'empty' | 'overlimit'>('all');

  // Find maximum amount on a single number to compute relative heatmap intensity
  let maxAmount = 1;
  for (let i = 0; i <= 99; i++) {
    const key = formatTwoDigit(i);
    const val = totals[key] || 0;
    if (val > maxAmount) maxAmount = val;
  }

  // Generate 00 - 99 numbers array
  const allNumbers = Array.from({ length: 100 }, (_, i) => formatTwoDigit(i));

  const filteredNumbers = allNumbers.filter((num) => {
    const amt = totals[num] || 0;
    if (filterMode === 'sold') return amt > 0;
    if (filterMode === 'empty') return amt === 0;
    if (filterMode === 'overlimit') return limitPerNumber > 0 && amt > limitPerNumber;
    return true;
  });

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
      {/* Header & Filter Controls */}
      <div className="p-4 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-bold text-slate-900 text-sm sm:text-base flex items-center gap-2">
            <span>🔢 00 မှ 99 ဂဏန်း အကွက် ၁၀၀ ဇယား (100 Number Grid)</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            အရောင်းများသော အကွက်များကို အရောင်အနုအရင့်ဖြင့် ပြသပေးပါသည်
          </p>
        </div>

        {/* Filter Buttons */}
        <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl text-xs font-medium text-slate-600">
          <button
            type="button"
            onClick={() => setFilterMode('all')}
            className={`px-2.5 py-1 rounded-lg transition-all ${
              filterMode === 'all' ? 'bg-white text-slate-900 font-bold shadow-xs' : 'hover:text-slate-900'
            }`}
          >
            အားလုံး (100)
          </button>
          <button
            type="button"
            onClick={() => setFilterMode('sold')}
            className={`px-2.5 py-1 rounded-lg transition-all ${
              filterMode === 'sold' ? 'bg-white text-emerald-700 font-bold shadow-xs' : 'hover:text-slate-900'
            }`}
          >
            ပါပြီးသား ({allNumbers.filter((n) => (totals[n] || 0) > 0).length})
          </button>
          <button
            type="button"
            onClick={() => setFilterMode('empty')}
            className={`px-2.5 py-1 rounded-lg transition-all ${
              filterMode === 'empty' ? 'bg-white text-slate-700 font-bold shadow-xs' : 'hover:text-slate-900'
            }`}
          >
            မရောင်းရသေး ({allNumbers.filter((n) => (totals[n] || 0) === 0).length})
          </button>
          {limitPerNumber > 0 && (
            <button
              type="button"
              onClick={() => setFilterMode('overlimit')}
              className={`px-2.5 py-1 rounded-lg transition-all ${
                filterMode === 'overlimit' ? 'bg-white text-rose-700 font-bold shadow-xs' : 'hover:text-slate-900'
              }`}
            >
              ဘောင်ကျော် ({allNumbers.filter((n) => (totals[n] || 0) > limitPerNumber).length})
            </button>
          )}
        </div>
      </div>

      {/* 10 x 10 Responsive Grid */}
      <div className="p-3 sm:p-4">
        <div className="grid grid-cols-5 sm:grid-cols-10 gap-1.5 sm:gap-2">
          {filteredNumbers.map((num) => {
            const amt = totals[num] || 0;
            const isSold = amt > 0;
            const isOverLimit = limitPerNumber > 0 && amt > limitPerNumber;
            const isSelected = selectedNumber === num;
            const isWinner = winningNumber === num;

            // Calculate heatmap color intensity
            const intensity = Math.min(1, amt / maxAmount);

            let bgClass = 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100';
            if (isWinner) {
              bgClass = 'bg-amber-400 border-amber-600 text-slate-950 ring-2 ring-amber-500 font-black scale-105 z-10 shadow-md';
            } else if (isOverLimit) {
              bgClass = 'bg-rose-100 border-rose-400 text-rose-950 font-bold shadow-xs';
            } else if (isSold) {
              if (intensity > 0.7) {
                bgClass = 'bg-indigo-600 border-indigo-700 text-white font-bold shadow-xs';
              } else if (intensity > 0.3) {
                bgClass = 'bg-indigo-100 border-indigo-300 text-indigo-950 font-semibold';
              } else {
                bgClass = 'bg-emerald-50 border-emerald-300 text-emerald-950';
              }
            }

            if (isSelected) {
              bgClass += ' ring-2 ring-indigo-500 ring-offset-1 scale-105 z-10';
            }

            return (
              <button
                key={num}
                type="button"
                onClick={() => onSelectNumber(num)}
                className={`p-1.5 sm:p-2 rounded-xl border flex flex-col items-center justify-between transition-all select-none cursor-pointer aspect-square ${bgClass}`}
              >
                <div className="flex items-center justify-between w-full">
                  <span className="font-mono font-bold text-xs sm:text-sm">{num}</span>
                  {isWinner && <Sparkles className="w-3 h-3 text-amber-950" />}
                </div>

                <div className="text-[10px] sm:text-[11px] font-mono leading-tight tracking-tight mt-0.5 truncate w-full text-center">
                  {isSold ? (
                    <span>{amt >= 1000 ? `${(amt / 1000).toFixed(amt % 1000 === 0 ? 0 : 1)}k` : amt}</span>
                  ) : (
                    <span className="text-slate-300 font-normal">-</span>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Legend / Helper Footer */}
      <div className="bg-slate-50 border-t border-slate-200 px-4 py-2.5 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-slate-100 border border-slate-300" />
            <span>မရောင်းရသေး</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-emerald-50 border border-emerald-300" />
            <span>ရောင်းရငွေ ပုံမှန်</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-indigo-600 border border-indigo-700" />
            <span>ရောင်းရငွေ များ</span>
          </div>
          {limitPerNumber > 0 && (
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded bg-rose-100 border border-rose-400" />
              <span>ဘောင်ကျော် ({limitPerNumber.toLocaleString()} ကျပ်ကျော်)</span>
            </div>
          )}
        </div>

        <div>
          <span>ကလစ်နှိပ်၍ အကွက်အလိုက် စာရင်းအသေးစိတ် စစ်ဆေးနိုင်ပါသည်</span>
        </div>
      </div>
    </div>
  );
};
