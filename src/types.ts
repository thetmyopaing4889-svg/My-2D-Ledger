export interface LedgerEntry {
  id: string;
  number: string; // '00' through '99'
  amount: number; // in MMK
  timestamp: number;
  customerName?: string;
  sourceNote?: string;
}

export interface ParsedItemPreview {
  number: string;
  amount: number;
  sourceText: string;
  isFormula?: boolean;
  formulaType?: string;
  currentTotal: number;
  newTotal: number;
  isOverLimit?: boolean;
}

export interface FormulaHelper {
  nameMm: string;
  nameEn: string;
  desc: string;
  example: string;
}

export interface NumberSummary {
  number: string;
  totalAmount: number;
  entryCount: number;
  entries: LedgerEntry[];
}

export interface PayoutResult {
  winningNumber: string;
  payoutMultiplier: number;
  totalCollected: number;
  winningBetTotal: number;
  totalPayout: number;
  netProfit: number;
}
