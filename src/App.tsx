import React, { useState, useEffect, useMemo } from 'react';
import { LedgerEntry, ParsedItemPreview } from './types';
import { parseRawInput, formatTwoDigit } from './utils/parser';
import { SmartInputBox } from './components/SmartInputBox';
import { NumberGrid100 } from './components/NumberGrid100';
import { PayoutCalculatorModal } from './components/PayoutCalculatorModal';
import { EntryHistoryTable } from './components/EntryHistoryTable';
import { 
  BarChart3, 
  Settings, 
  ShieldAlert, 
  Sparkles, 
  RefreshCw, 
  FileSpreadsheet, 
  TrendingUp,
  Sliders,
  DollarSign
} from 'lucide-react';

const STORAGE_KEY_ENTRIES = '2d_ledger_entries_v1';
const STORAGE_KEY_LIMIT = '2d_ledger_limit_v1';

export const App: React.FC = () => {
  // Application State
  const [entries, setEntries] = useState<LedgerEntry[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_ENTRIES);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [limitPerNumber, setLimitPerNumber] = useState<number>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY_LIMIT);
      return saved ? parseInt(saved, 10) : 50000;
    } catch {
      return 50000;
    }
  });

  const [rawInput, setRawInput] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [selectedNumber, setSelectedNumber] = useState<string | null>(null);
  const [winningNumber, setWinningNumber] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'grid' | 'logs' | 'calculator'>('grid');

  // Persist entries and limits
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_ENTRIES, JSON.stringify(entries));
  }, [entries]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_LIMIT, limitPerNumber.toString());
  }, [limitPerNumber]);

  // Aggregate totals per number (00-99)
  const totals = useMemo(() => {
    const map: Record<string, number> = {};
    for (let i = 0; i <= 99; i++) {
      map[formatTwoDigit(i)] = 0;
    }
    entries.forEach((e) => {
      map[e.number] = (map[e.number] || 0) + e.amount;
    });
    return map;
  }, [entries]);

  // Overall statistics
  const totalCollected = useMemo(() => {
    return Object.values(totals).reduce((sum: number, val: number) => sum + val, 0);
  }, [totals]);

  const activeNumbersCount = useMemo(() => {
    return Object.values(totals).filter((v: number) => v > 0).length;
  }, [totals]);

  // Real-time parsing of current user input
  const { items: parsedPreviews, totalParsedAmount, errorTokens } = useMemo(() => {
    if (!rawInput.trim()) {
      return { items: [], totalParsedAmount: 0, errorTokens: [] };
    }
    return parseRawInput(rawInput, totals, limitPerNumber);
  }, [rawInput, totals, limitPerNumber]);

  // Action: Confirm and commit parsed entries
  const handleConfirm = () => {
    if (parsedPreviews.length === 0) return;

    const timestamp = Date.now();
    const newEntries: LedgerEntry[] = parsedPreviews.map((p, idx) => ({
      id: `${timestamp}-${idx}-${Math.random().toString(36).substr(2, 5)}`,
      number: p.number,
      amount: p.amount,
      timestamp,
      customerName: customerName.trim() || undefined,
      sourceNote: p.sourceText,
    }));

    setEntries((prev) => [...prev, ...newEntries]);
    setRawInput('');
  };

  // Action: Clear input box
  const handleClear = () => {
    setRawInput('');
  };

  // Action: Delete single entry
  const handleDeleteEntry = (id: string) => {
    setEntries((prev) => prev.filter((e) => e.id !== id));
  };

  // Action: Clear all stored entries
  const handleClearAll = () => {
    if (window.confirm('လက်ရှိ စာရင်းအားလုံးကို အပြီးတိုင် ဖျက်မည်မှာ သေချာပါသလား?')) {
      setEntries([]);
      setWinningNumber(null);
      setSelectedNumber(null);
    }
  };

  // Action: Export summary as CSV / Text file
  const handleExportCSV = () => {
    let csv = 'ဂဏန်း,ရောင်းရငွေ (ကျပ်)\n';
    for (let i = 0; i <= 99; i++) {
      const num = formatTwoDigit(i);
      const amt = totals[num] || 0;
      if (amt > 0) {
        csv += `${num},${amt}\n`;
      }
    }
    csv += `စုစုပေါင်း,${totalCollected}\n`;

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `2D_Ledger_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Number Details Dialog/Panel when clicking on a number
  const selectedNumberEntries = useMemo(() => {
    if (!selectedNumber) return [];
    return entries.filter((e) => e.number === selectedNumber);
  }, [selectedNumber, entries]);

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900 font-sans pb-16">
      {/* Top Navbar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
        <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-600 to-indigo-800 text-white flex items-center justify-center font-black shadow-sm text-sm">
              2D
            </div>
            <div>
              <h1 className="font-bold text-base sm:text-lg text-slate-900 tracking-tight leading-tight">
                2D Number Ledger & Analytics
              </h1>
              <p className="text-xs text-slate-500">
                00 မှ 99 စာရင်းတွက်ချက်မှုနှင့် Live Preview စနစ်
              </p>
            </div>
          </div>

          {/* Quick Stats Banner */}
          <div className="flex items-center gap-3 sm:gap-4 text-xs">
            <div className="bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-xl flex flex-col items-end">
              <span className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider">ပါဝင်အကွက်</span>
              <strong className="text-indigo-600 font-bold text-sm font-mono">{activeNumbersCount} / 100</strong>
            </div>

            <div className="bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-xl flex flex-col items-end">
              <span className="text-[10px] text-emerald-700 font-semibold uppercase tracking-wider">စုစုပေါင်းငွေ</span>
              <strong className="text-emerald-800 font-bold text-sm font-mono">{totalCollected.toLocaleString()} ကျပ်</strong>
            </div>

            {/* Export CSV Button */}
            <button
              type="button"
              onClick={handleExportCSV}
              className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 transition-colors"
              title="CSV ထုတ်ယူမည်"
            >
              <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Workspace */}
      <main className="max-w-7xl mx-auto px-4 py-5 space-y-5">
        {/* 1. SMART INPUT BOX WITH AUTO-EXPAND & REALTIME PREVIEW */}
        <SmartInputBox
          rawInput={rawInput}
          onInputChange={setRawInput}
          parsedPreviews={parsedPreviews}
          totalParsedAmount={totalParsedAmount}
          errorTokens={errorTokens}
          onConfirm={handleConfirm}
          onClear={handleClear}
          customerName={customerName}
          onCustomerNameChange={setCustomerName}
          limitPerNumber={limitPerNumber}
        />

        {/* Global Controls & Limit Settings */}
        <div className="bg-white rounded-2xl p-3.5 border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2">
            <Sliders className="w-4 h-4 text-slate-500" />
            <span className="font-bold text-slate-700">တစ်ကွက် ကန့်သတ်ဘောင်ငွေ (Limit):</span>
            <input
              type="number"
              step={1000}
              value={limitPerNumber}
              onChange={(e) => setLimitPerNumber(Math.max(0, parseInt(e.target.value, 10) || 0))}
              className="w-28 font-mono font-bold text-slate-900 border border-slate-300 rounded-lg px-2 py-1 bg-slate-50"
            />
            <span className="text-slate-500 font-medium">ကျပ် (ကျော်လွန်ပါက သတိပေးမည်)</span>
          </div>

          {/* Navigation View Tabs */}
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl">
            <button
              type="button"
              onClick={() => setActiveTab('grid')}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                activeTab === 'grid' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              🔢 အကွက် ၁၀၀ ဇယား (Grid)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('calculator')}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                activeTab === 'calculator' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              🏆 ပေါက်သီး / လျော်ကြေး (Payout)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('logs')}
              className={`px-3 py-1.5 rounded-lg font-bold transition-all ${
                activeTab === 'logs' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              📋 မှတ်တမ်းအသေးစိတ် ({entries.length})
            </button>
          </div>
        </div>

        {/* 2. TAB VIEWS */}
        {activeTab === 'grid' && (
          <div className="space-y-5">
            <NumberGrid100
              totals={totals}
              entries={entries}
              limitPerNumber={limitPerNumber}
              onSelectNumber={setSelectedNumber}
              selectedNumber={selectedNumber}
              winningNumber={winningNumber}
            />

            {/* Selected Single Number Detail Drawer/Modal */}
            {selectedNumber && (
              <div className="bg-white rounded-2xl border-2 border-indigo-500 p-4 shadow-md">
                <div className="flex items-center justify-between border-b border-slate-200 pb-2 mb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xl font-black font-mono bg-indigo-600 text-white px-3 py-1 rounded-xl">
                      {selectedNumber}
                    </span>
                    <div>
                      <h4 className="font-bold text-sm text-slate-900">
                        ဂဏန်း '{selectedNumber}' ၏ အရောင်းစာရင်း အသေးစိတ်
                      </h4>
                      <span className="text-xs text-slate-500">
                        စုစုပေါင်း: <strong className="text-indigo-600 font-bold font-mono">{(totals[selectedNumber] || 0).toLocaleString()} ကျပ်</strong> ({selectedNumberEntries.length} ကြိမ်)
                      </span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => setSelectedNumber(null)}
                    className="text-xs px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold"
                  >
                    ပိတ်မည် ✕
                  </button>
                </div>

                {selectedNumberEntries.length === 0 ? (
                  <p className="text-xs text-slate-400 py-2">အရောင်းမှတ်တမ်း မရှိသေးပါ</p>
                ) : (
                  <div className="max-h-48 overflow-y-auto space-y-1.5 pr-1 text-xs">
                    {selectedNumberEntries.map((e) => (
                      <div key={e.id} className="flex items-center justify-between p-2 rounded-lg bg-slate-50 border border-slate-200 font-mono">
                        <div>
                          <span className="font-sans font-semibold text-slate-800 mr-2">
                            {e.customerName || 'အထွေထွေ'}
                          </span>
                          <span className="text-[11px] text-slate-400 font-sans">
                            {new Date(e.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <strong className="text-slate-900 font-bold">{e.amount.toLocaleString()} Ks</strong>
                          <button
                            type="button"
                            onClick={() => handleDeleteEntry(e.id)}
                            className="text-slate-400 hover:text-rose-600 transition-colors"
                          >
                            ဖျက်
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {activeTab === 'calculator' && (
          <PayoutCalculatorModal
            totals={totals}
            totalCollected={totalCollected}
            onSetWinningNumber={setWinningNumber}
            currentWinningNumber={winningNumber}
          />
        )}

        {activeTab === 'logs' && (
          <EntryHistoryTable
            entries={entries}
            onDeleteEntry={handleDeleteEntry}
            onClearAll={handleClearAll}
          />
        )}
      </main>
    </div>
  );
};

export default App;
