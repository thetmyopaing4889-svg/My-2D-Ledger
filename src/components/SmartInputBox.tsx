import React, { useRef, useEffect } from 'react';
import { ParsedItemPreview } from '../types';
import { Send, Trash2, HelpCircle, CheckCircle2, AlertTriangle } from 'lucide-react';

interface SmartInputBoxProps {
  rawInput: string;
  onInputChange: (val: string) => void;
  parsedPreviews: ParsedItemPreview[];
  totalParsedAmount: number;
  errorTokens: string[];
  onConfirm: () => void;
  onClear: () => void;
  customerName: string;
  onCustomerNameChange: (val: string) => void;
  limitPerNumber: number;
}

export const SmartInputBox: React.FC<SmartInputBoxProps> = ({
  rawInput,
  onInputChange,
  parsedPreviews,
  totalParsedAmount,
  errorTokens,
  onConfirm,
  onClear,
  customerName,
  onCustomerNameChange,
  limitPerNumber,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  // Auto-expand dynamic height based on content
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.max(110, textareaRef.current.scrollHeight)}px`;
    }
  }, [rawInput]);

  const hasItems = parsedPreviews.length > 0;
  const overLimitCount = parsedPreviews.filter(p => p.isOverLimit).length;

  const insertQuickText = (text: string) => {
    onInputChange(rawInput ? `${rawInput} ${text} ` : `${text} `);
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col">
      {/* Header Bar */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 px-4 py-3 text-white flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
          <h2 className="font-bold text-sm sm:text-base tracking-wide flex items-center gap-1.5">
            <span>📝 စာရင်းသွင်း ရိုက်ထည့်ရန် (Smart Data Input)</span>
          </h2>
        </div>

        {/* Customer / Note input */}
        <div className="flex items-center gap-2">
          <label className="text-xs text-slate-300 whitespace-nowrap">ဖောက်သည်/အမည်:</label>
          <input
            type="text"
            value={customerName}
            onChange={(e) => onCustomerNameChange(e.target.value)}
            placeholder="ဥပမာ - ကိုကျော်"
            className="bg-slate-700/80 border border-slate-600 rounded-lg px-2.5 py-1 text-xs text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-indigo-400 w-32 sm:w-40"
          />
        </div>
      </div>

      {/* Quick Formula Helper Pills */}
      <div className="bg-slate-50 border-b border-slate-200 px-3 py-2 flex flex-wrap items-center gap-1.5 text-xs text-slate-700">
        <span className="font-semibold text-slate-500 mr-1">အတိုကောက်များ:</span>
        <button
          type="button"
          onClick={() => insertQuickText('10R100')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-indigo-700"
        >
          10R100 (အာခတ်)
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('ပါဝါ 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-purple-700"
        >
          ပါဝါ 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('နက္ခတ် 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-emerald-700"
        >
          နက္ခတ် 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('အပူး 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-pink-700"
        >
          အပူး 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('ညီအကို 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-amber-700"
        >
          ညီအကို 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('1ထိပ် 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-cyan-700"
        >
          1ထိပ် 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('5ပိတ် 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-teal-700"
        >
          5ပိတ် 500
        </button>
        <button
          type="button"
          onClick={() => insertQuickText('0ဘရိတ် 500')}
          className="px-2 py-0.5 rounded-md bg-white border border-slate-200 hover:bg-slate-100 transition-colors text-[11px] font-mono text-rose-700"
        >
          0ဘရိတ် 500
        </button>
      </div>

      {/* Main Dynamic Expanding Textarea */}
      <div className="p-3">
        <textarea
          ref={textareaRef}
          value={rawInput}
          onChange={(e) => onInputChange(e.target.value)}
          placeholder={`စာရင်းများကို လွတ်လပ်စွာ ရိုက်ထည့်ပါ သို့မဟုတ် ကော်ပီကူးထည့်ပါ...\nဥပမာ - \n00 100\n00-100\n00.100\n10R500\nပါဝါ 1000\n1ထိပ် 500 5ပိတ် 300 0ဘရိတ် 200`}
          className="w-full min-h-[110px] p-3 text-sm sm:text-base font-mono border-2 border-slate-200 focus:border-indigo-500 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-100 resize-none transition-all placeholder:text-slate-400 text-slate-800"
        />

        {/* Realtime Feedback Status Bar */}
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs">
          <div className="flex items-center gap-3">
            <span className="text-slate-600">
              စုစုပေါင်းအကွက်: <strong className="text-indigo-600 font-bold">{parsedPreviews.length} ကွက်</strong>
            </span>
            <span className="text-slate-600">
              စုစုပေါင်းငွေ: <strong className="text-emerald-700 font-bold text-sm">{totalParsedAmount.toLocaleString()} ကျပ်</strong>
            </span>
            {overLimitCount > 0 && (
              <span className="inline-flex items-center gap-1 text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200 font-medium">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                {overLimitCount} ကွက် ဘောင်ကျော်နေသည်
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {rawInput.length > 0 && (
              <button
                type="button"
                onClick={onClear}
                className="px-3 py-1.5 rounded-lg border border-slate-300 text-slate-600 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 transition-colors flex items-center gap-1 font-medium"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>ရှင်းမည် (Clear)</span>
              </button>
            )}

            <button
              type="button"
              onClick={onConfirm}
              disabled={!hasItems}
              className={`px-5 py-2 rounded-xl font-bold flex items-center gap-2 shadow-sm transition-all ${
                hasItems
                  ? 'bg-indigo-600 hover:bg-indigo-700 text-white active:scale-95 shadow-indigo-200'
                  : 'bg-slate-200 text-slate-400 cursor-not-allowed'
              }`}
            >
              <Send className="w-4 h-4" />
              <span>အတည်ပြု သွင်းမည် (Confirm)</span>
            </button>
          </div>
        </div>
      </div>

      {/* REAL-TIME PREVIEW PANEL (User Requested Feature) */}
      {hasItems && (
        <div className="border-t border-slate-200 bg-indigo-50/40 p-3 sm:p-4">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-indigo-600" />
              <h3 className="text-xs sm:text-sm font-bold text-slate-800">
                ⚡ ကြိုတင်စစ်ဆေးမှု စာရင်း (Real-time Live Preview)
              </h3>
              <span className="text-[11px] text-slate-500 bg-white px-2 py-0.5 rounded-full border border-slate-200">
                Confirm မနှိပ်မီ ရှိပြီးစာရင်းနှင့် ပေါင်းပြီး ပြသထားသည်
              </span>
            </div>
          </div>

          {/* Grid of Preview Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2 max-h-56 overflow-y-auto pr-1">
            {parsedPreviews.map((item, idx) => (
              <div
                key={`${item.number}-${idx}`}
                className={`p-2 rounded-xl border text-xs flex flex-col justify-between transition-all ${
                  item.isOverLimit
                    ? 'bg-amber-50 border-amber-300 text-amber-950'
                    : item.currentTotal > 0
                    ? 'bg-emerald-50/70 border-emerald-200 text-emerald-950'
                    : 'bg-white border-slate-200 text-slate-800'
                }`}
              >
                <div className="flex items-center justify-between border-b border-slate-200/60 pb-1 mb-1">
                  <span className="font-mono font-bold text-base text-slate-900 bg-slate-100 px-1.5 py-0.2 rounded">
                    {item.number}
                  </span>
                  <span className="font-bold text-indigo-600">
                    +{item.amount.toLocaleString()} Ks
                  </span>
                </div>

                <div className="text-[10px] space-y-0.5">
                  <div className="flex justify-between text-slate-500">
                    <span>ရှိပြီး:</span>
                    <span className="font-mono font-medium">{item.currentTotal.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between font-semibold text-slate-700">
                    <span>စုစုပေါင်း:</span>
                    <span className={`font-mono ${item.isOverLimit ? 'text-amber-700 font-bold' : 'text-slate-900'}`}>
                      {item.newTotal.toLocaleString()}
                    </span>
                  </div>
                </div>

                {item.formulaType && (
                  <span className="mt-1 text-[9px] px-1 py-0.5 rounded bg-indigo-100 text-indigo-700 font-semibold inline-block text-center">
                    {item.formulaType}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Unparsed tokens warning if any */}
      {errorTokens.length > 0 && (
        <div className="bg-rose-50 border-t border-rose-200 px-3 py-2 text-xs text-rose-700 flex items-center gap-1.5">
          <AlertTriangle className="w-3.5 h-3.5 text-rose-500 shrink-0" />
          <span>ဖတ်မရသော စာလုံးများ: </span>
          <span className="font-mono font-semibold">{errorTokens.join(', ')}</span>
        </div>
      )}
    </div>
  );
};
