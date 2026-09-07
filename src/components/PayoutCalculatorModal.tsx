import React, { useState } from 'react';
import { PayoutResult } from '../types';
import { Trophy, Calculator, DollarSign, ArrowUpRight, Check } from 'lucide-react';
import { formatTwoDigit } from '../utils/parser';

interface PayoutCalculatorModalProps {
  totals: Record<string, number>;
  totalCollected: number;
  onSetWinningNumber: (num: string | null) => void;
  currentWinningNumber: string | null;
}

export const PayoutCalculatorModal: React.FC<PayoutCalculatorModalProps> = ({
  totals,
  totalCollected,
  onSetWinningNumber,
  currentWinningNumber,
}) => {
  const [winNumInput, setWinNumInput] = useState(currentWinningNumber || '');
  const [multiplier, setMultiplier] = useState(80); // Default 80 times (၈၀ ဆ)
  const [commissionPercent, setCommissionPercent] = useState(15); // Default 15% commission (ကော်မရှင်)

  const formattedNum = formatTwoDigit(winNumInput);
  const winningBetTotal = formattedNum ? totals[formattedNum] || 0 : 0;
  const totalPayout = winningBetTotal * multiplier;
  const commissionAmount = (totalCollected * commissionPercent) / 100;
  const netEarnings = totalCollected - commissionAmount - totalPayout;

  const handleApplyWin = () => {
    if (formattedNum) {
      onSetWinningNumber(formattedNum);
    }
  };

  const handleResetWin = () => {
    setWinNumInput('');
    onSetWinningNumber(null);
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 sm:p-5">
      <div className="flex items-center justify-between border-b border-slate-200 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
            <Trophy className="w-4 h-4" />
          </div>
          <div>
            <h3 className="font-bold text-slate-900 text-sm sm:text-base">
              🏆 ပေါက်ဂဏန်းနှင့် လျော်ကြေး တွက်ချက်စနစ် (Payout Calculator)
            </h3>
            <p className="text-xs text-slate-500">
              ထွက်ဂဏန်းရိုက်ထည့်ပြီး ဒိုင်/ကိုယ်စားလှယ် အရှုံးအမြတ်နှင့် လျော်ကြေးကို တွက်ပါ
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Input Parameters */}
        <div className="space-y-3 bg-slate-50 p-3.5 rounded-xl border border-slate-200">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              ပေါက်သီးဂဏန်း (00 - 99):
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                maxLength={2}
                value={winNumInput}
                onChange={(e) => setWinNumInput(e.target.value.replace(/\D/g, ''))}
                placeholder="ဥပမာ 25"
                className="w-24 font-mono font-bold text-lg text-center p-2 rounded-lg border-2 border-indigo-300 focus:border-indigo-600 focus:outline-none bg-white text-slate-900"
              />
              <button
                type="button"
                onClick={handleApplyWin}
                disabled={!formattedNum}
                className="px-3 py-2 rounded-lg bg-indigo-600 text-white font-bold text-xs hover:bg-indigo-700 transition-colors disabled:bg-slate-300"
              >
                စစ်ဆေးမည်
              </button>
              {currentWinningNumber && (
                <button
                  type="button"
                  onClick={handleResetWin}
                  className="px-2.5 py-2 rounded-lg border border-slate-300 text-slate-600 text-xs hover:bg-slate-100"
                >
                  ပယ်ဖျက်
                </button>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                လျော်ကြေးအဆ (Odds):
              </label>
              <div className="flex items-center gap-1">
                <input
                  type="number"
                  value={multiplier}
                  onChange={(e) => setMultiplier(Math.max(1, parseInt(e.target.value, 10) || 1))}
                  className="w-full font-mono text-sm p-1.5 rounded-lg border border-slate-300 bg-white"
                />
                <span className="text-xs text-slate-500 font-semibold">ဆ</span>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                ကော်မရှင် (Commission):
              </label>
              <div className="flex items-center gap-1">
                <input
                  type="number"
                  value={commissionPercent}
                  onChange={(e) => setCommissionPercent(Math.max(0, parseInt(e.target.value, 10) || 0))}
                  className="w-full font-mono text-sm p-1.5 rounded-lg border border-slate-300 bg-white"
                />
                <span className="text-xs text-slate-500 font-semibold">%</span>
              </div>
            </div>
          </div>
        </div>

        {/* Calculation Summary Details */}
        <div className="md:col-span-2 grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-200">
            <span className="text-[11px] text-slate-500 block font-medium">စုစုပေါင်း ရောင်းရငွေ</span>
            <strong className="text-sm sm:text-base text-slate-900 font-mono font-bold block mt-1">
              {totalCollected.toLocaleString()} Ks
            </strong>
          </div>

          <div className="bg-indigo-50/60 p-3 rounded-xl border border-indigo-200">
            <span className="text-[11px] text-indigo-700 block font-medium">
              ပေါက်ဂဏန်း {formattedNum ? `(${formattedNum})` : ''} အရောင်း
            </span>
            <strong className="text-sm sm:text-base text-indigo-900 font-mono font-bold block mt-1">
              {winningBetTotal.toLocaleString()} Ks
            </strong>
          </div>

          <div className="bg-rose-50 p-3 rounded-xl border border-rose-200">
            <span className="text-[11px] text-rose-700 block font-medium">
              စုစုပေါင်း လျော်ကြေး ({multiplier} ဆ)
            </span>
            <strong className="text-sm sm:text-base text-rose-800 font-mono font-bold block mt-1">
              {totalPayout.toLocaleString()} Ks
            </strong>
          </div>

          <div
            className={`p-3 rounded-xl border ${
              netEarnings >= 0
                ? 'bg-emerald-50 border-emerald-200 text-emerald-950'
                : 'bg-rose-100 border-rose-300 text-rose-950'
            }`}
          >
            <span className="text-[11px] font-medium block">
              {netEarnings >= 0 ? 'အသားတင် အမြတ်ငွေ' : 'အသားတင် အရှုံးငွေ'}
            </span>
            <strong
              className={`text-sm sm:text-base font-mono font-black block mt-1 ${
                netEarnings >= 0 ? 'text-emerald-700' : 'text-rose-700'
              }`}
            >
              {Math.abs(netEarnings).toLocaleString()} Ks
            </strong>
          </div>
        </div>
      </div>
    </div>
  );
};
