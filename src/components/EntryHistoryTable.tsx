import React, { useState } from 'react';
import { LedgerEntry } from '../types';
import { Search, User, Clock, Hash, Trash2, ArrowUpDown } from 'lucide-react';

interface EntryHistoryTableProps {
  entries: LedgerEntry[];
  onDeleteEntry: (id: string) => void;
  onClearAll: () => void;
}

export const EntryHistoryTable: React.FC<EntryHistoryTableProps> = ({
  entries,
  onDeleteEntry,
  onClearAll,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'latest' | 'number' | 'amount'>('latest');

  const filtered = entries.filter((e) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      e.number.includes(term) ||
      (e.customerName && e.customerName.toLowerCase().includes(term)) ||
      e.amount.toString().includes(term)
    );
  });

  const sorted = [...filtered].sort((a, b) => {
    if (sortBy === 'latest') return b.timestamp - a.timestamp;
    if (sortBy === 'number') return a.number.localeCompare(b.number);
    if (sortBy === 'amount') return b.amount - a.amount;
    return 0;
  });

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
      {/* Table Header Controls */}
      <div className="p-4 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-slate-500" />
          <h3 className="font-bold text-slate-900 text-sm sm:text-base">
            📋 စာရင်းသွင်း မှတ်တမ်းအသေးစိတ် (Entry Logs - {entries.length} ခု)
          </h3>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="ဂဏန်း / အမည် ရှာရန်..."
              className="pl-8 pr-3 py-1.5 text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500 w-36 sm:w-48 text-slate-800"
            />
          </div>

          {/* Sort Selector */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="text-xs border border-slate-300 rounded-xl px-2.5 py-1.5 bg-white text-slate-700 focus:outline-none"
          >
            <option value="latest">နောက်ဆုံးသွင်းသည့်အစဉ်</option>
            <option value="amount">ငွေပမာဏ အများဆုံး</option>
            <option value="number">ဂဏန်းအစဉ်လိုက် (00-99)</option>
          </select>

          {entries.length > 0 && (
            <button
              type="button"
              onClick={onClearAll}
              className="px-2.5 py-1.5 rounded-xl border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs font-medium flex items-center gap-1 transition-colors"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>စာရင်းအားလုံးဖျက်မည်</span>
            </button>
          )}
        </div>
      </div>

      {/* Table Data */}
      <div className="max-h-72 overflow-y-auto">
        {sorted.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs sm:text-sm">
            မှတ်တမ်းစာရင်း မရှိသေးပါ
          </div>
        ) : (
          <table className="w-full text-left text-xs border-collapse">
            <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 sticky top-0">
              <tr>
                <th className="py-2.5 px-3">အချိန်</th>
                <th className="py-2.5 px-3">ဖောက်သည်/အမည်</th>
                <th className="py-2.5 px-3 text-center">ဂဏန်း</th>
                <th className="py-2.5 px-3 text-right">ငွေပမာဏ (ကျပ်)</th>
                <th className="py-2.5 px-3 text-center">စီမံရန်</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-mono">
              {sorted.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-2 px-3 text-slate-500 font-sans text-[11px]">
                    {new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </td>
                  <td className="py-2 px-3 text-slate-800 font-sans font-medium">
                    {item.customerName || <span className="text-slate-400 font-normal">အထွေထွေ</span>}
                  </td>
                  <td className="py-2 px-3 text-center">
                    <span className="bg-indigo-50 border border-indigo-200 text-indigo-700 font-bold px-2 py-0.5 rounded-md text-xs">
                      {item.number}
                    </span>
                  </td>
                  <td className="py-2 px-3 text-right font-bold text-slate-900">
                    {item.amount.toLocaleString()}
                  </td>
                  <td className="py-2 px-3 text-center">
                    <button
                      type="button"
                      onClick={() => onDeleteEntry(item.id)}
                      className="text-slate-400 hover:text-rose-600 transition-colors p-1"
                      title="ဖျက်မည်"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
