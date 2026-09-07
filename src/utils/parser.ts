import { ParsedItemPreview } from '../types';

// Convert Myanmar digit characters (၀-၉) to standard English numbers (0-9)
export function normalizeMyanmarDigits(input: string): string {
  const myanmarDigits = ['၀', '၁', '၂', '၃', '၄', '၅', '၆', '၇', '၈', '၉'];
  let result = input;
  myanmarDigits.forEach((char, index) => {
    result = result.replaceAll(char, index.toString());
  });
  return result;
}

// Ensure 2-digit format e.g., '5' -> '05'
export function formatTwoDigit(num: number | string): string {
  const n = parseInt(num.toString(), 10);
  if (isNaN(n) || n < 0 || n > 99) return '';
  return n.toString().padStart(2, '0');
}

// Generate power pairs: 05, 16, 27, 38, 49 + their reverse
export const POWER_NUMBERS = [
  '05', '50', '16', '61', '27', '72', '38', '83', '49', '94'
];

// Natkhat pairs: 07, 18, 24, 35, 69 + their reverse
export const NATKHAT_NUMBERS = [
  '07', '70', '18', '81', '24', '42', '35', '53', '69', '96'
];

// Twin / Double numbers: 00, 11, 22, 33, 44, 55, 66, 77, 88, 99
export const TWIN_NUMBERS = [
  '00', '11', '22', '33', '44', '55', '66', '77', '88', '99'
];

// Brother / Consecutive numbers: 01, 12, 23, 34, 45, 56, 67, 78, 89, 90 + reverse
export const BROTHER_NUMBERS = [
  '01', '10', '12', '21', '23', '32', '34', '43', '45', '54',
  '56', '65', '67', '76', '78', '87', '89', '98', '90', '09'
];

// Get Break (Sum Total % 10) numbers
export function getBreakNumbers(breakDigit: number): string[] {
  const target = breakDigit % 10;
  const list: string[] = [];
  for (let i = 0; i <= 99; i++) {
    const tens = Math.floor(i / 10);
    const ones = i % 10;
    if ((tens + ones) % 10 === target) {
      list.push(formatTwoDigit(i));
    }
  }
  return list;
}

// Get Head (ထိပ်စီး) numbers
export function getHeadNumbers(digit: number): string[] {
  const d = digit % 10;
  const list: string[] = [];
  for (let i = 0; i <= 9; i++) {
    list.push(`${d}${i}`);
  }
  return list;
}

// Get Tail (နောက်ပိတ်) numbers
export function getTailNumbers(digit: number): string[] {
  const d = digit % 10;
  const list: string[] = [];
  for (let i = 0; i <= 9; i++) {
    list.push(`${i}${d}`);
  }
  return list;
}

/**
 * Intelligent 2D Text Parsing Engine
 * Parses raw strings containing:
 * - 00 100
 * - 00-100
 * - 00.100
 * - 00=100
 * - 12 r 500 or 12r500 or 12.500r (reverse)
 * - power 500, ပါဝါ 500, pw 500
 * - natkhat 500, နက္ခတ် 500, nk 500
 * - twin 500, အပူး 500, ပူး 500
 * - brother 500, ညီအကို 500, ညီအစ်ကို 500
 * - 1 ထိပ် 500, 1T 500, 1ထိပ် 500
 * - 5 ပိတ် 500, 5P 500, 5ပိတ် 500
 * - 0 ဘရိတ် 500, 0B 500
 */
export function parseRawInput(
  rawText: string,
  existingTotals: Record<string, number>,
  limitPerNumber: number = 50000
): { items: ParsedItemPreview[]; totalParsedAmount: number; errorTokens: string[] } {
  const normalized = normalizeMyanmarDigits(rawText.toLowerCase());
  
  // Replace line breaks, commas, semicolons, plus signs with spaces
  const cleaned = normalized.replace(/[\n\r,;+]/g, ' ');
  // Split tokens by spaces
  const tokens = cleaned.split(/\s+/).filter(t => t.trim().length > 0);

  const parsedItems: ParsedItemPreview[] = [];
  const errorTokens: string[] = [];

  // Working copy of totals to calculate running additions in single batch
  const runningTotals: Record<string, number> = { ...existingTotals };

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];

    // Check for explicit special keywords with next token or self-contained
    // 1. POWER / ပါဝါ
    if (token === 'power' || token === 'ပါဝါ' || token === 'pw') {
      const nextAmount = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
      if (nextAmount > 0) {
        POWER_NUMBERS.forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + nextAmount;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: nextAmount,
            sourceText: `${token} ${nextAmount}`,
            isFormula: true,
            formulaType: 'ပါဝါ',
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        i++; // skip next amount
        continue;
      }
    }

    // 2. NATKHAT / နက္ခတ်
    if (token === 'natkhat' || token === 'နက္ခတ်' || token === 'nk') {
      const nextAmount = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
      if (nextAmount > 0) {
        NATKHAT_NUMBERS.forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + nextAmount;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: nextAmount,
            sourceText: `${token} ${nextAmount}`,
            isFormula: true,
            formulaType: 'နက္ခတ်',
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        i++;
        continue;
      }
    }

    // 3. TWIN / အပူး / ပူး
    if (token === 'twin' || token === 'အပူး' || token === 'ပူး' || token === 'double') {
      const nextAmount = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
      if (nextAmount > 0) {
        TWIN_NUMBERS.forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + nextAmount;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: nextAmount,
            sourceText: `${token} ${nextAmount}`,
            isFormula: true,
            formulaType: 'အပူး',
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        i++;
        continue;
      }
    }

    // 4. BROTHER / ညီအကို / ညီအစ်ကို
    if (token === 'brother' || token === 'ညီအကို' || token === 'ညီအစ်ကို' || token === 'bro') {
      const nextAmount = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
      if (nextAmount > 0) {
        BROTHER_NUMBERS.forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + nextAmount;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: nextAmount,
            sourceText: `${token} ${nextAmount}`,
            isFormula: true,
            formulaType: 'ညီအကို',
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        i++;
        continue;
      }
    }

    // 5. Head / ထိပ်စီး (e.g., '1ထိပ်', '1t', '1ထိပ် 500')
    const headMatch = token.match(/^(\d)(ထိပ်|t|head)(.*)$/);
    if (headMatch) {
      const digit = parseInt(headMatch[1], 10);
      let amt = parseInt(headMatch[3].replace(/\D/g, ''), 10);
      if (isNaN(amt) || amt <= 0) {
        amt = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
        if (amt > 0) i++;
      }
      if (amt > 0) {
        getHeadNumbers(digit).forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + amt;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: amt,
            sourceText: `${digit}ထိပ် ${amt}`,
            isFormula: true,
            formulaType: `${digit} ထိပ်စီး`,
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        continue;
      }
    }

    // 6. Tail / နောက်ပိတ် (e.g., '5ပိတ်', '5p', '5ပိတ် 500')
    const tailMatch = token.match(/^(\d)(ပိတ်|p|tail)(.*)$/);
    if (tailMatch) {
      const digit = parseInt(tailMatch[1], 10);
      let amt = parseInt(tailMatch[3].replace(/\D/g, ''), 10);
      if (isNaN(amt) || amt <= 0) {
        amt = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
        if (amt > 0) i++;
      }
      if (amt > 0) {
        getTailNumbers(digit).forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + amt;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: amt,
            sourceText: `${digit}ပိတ် ${amt}`,
            isFormula: true,
            formulaType: `${digit} နောက်ပိတ်`,
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        continue;
      }
    }

    // 7. Break / ဘရိတ် (e.g., '0ဘရိတ် 500', '0b 500')
    const breakMatch = token.match(/^(\d)(ဘရိတ်|b|break)(.*)$/);
    if (breakMatch) {
      const digit = parseInt(breakMatch[1], 10);
      let amt = parseInt(breakMatch[3].replace(/\D/g, ''), 10);
      if (isNaN(amt) || amt <= 0) {
        amt = i + 1 < tokens.length ? parseInt(tokens[i + 1].replace(/\D/g, ''), 10) : 0;
        if (amt > 0) i++;
      }
      if (amt > 0) {
        getBreakNumbers(digit).forEach(num => {
          const current = runningTotals[num] || 0;
          const newTot = current + amt;
          runningTotals[num] = newTot;
          parsedItems.push({
            number: num,
            amount: amt,
            sourceText: `${digit}ဘရိတ် ${amt}`,
            isFormula: true,
            formulaType: `${digit} ဘရိတ်`,
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        continue;
      }
    }

    // 8. Reverse R patterns within single token: e.g., '12r100', '12.100r', '12-100r', '12r=100'
    const singleTokenRMatch = token.match(/^(\d{2})r[=.\-]?(\d+)$/);
    if (singleTokenRMatch) {
      const num = formatTwoDigit(singleTokenRMatch[1]);
      const amt = parseInt(singleTokenRMatch[2], 10);
      const rev = `${num[1]}${num[0]}`;

      [num, rev].forEach((n, idx) => {
        // If it's a twin (e.g. 00r), avoid duplicate addition
        if (idx === 1 && num === rev) return;
        const current = runningTotals[n] || 0;
        const newTot = current + amt;
        runningTotals[n] = newTot;
        parsedItems.push({
          number: n,
          amount: amt,
          sourceText: `${num}R${amt}`,
          isFormula: true,
          formulaType: 'အာ (R)',
          currentTotal: current,
          newTotal: newTot,
          isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
        });
      });
      continue;
    }

    // 9. Standard delimiters within single token:
    // e.g., '00-100', '00.100', '00=100', '00/100', '00*100'
    const singleDelimMatch = token.match(/^(\d{2})[\.\-=\/*](\d+)(r?)$/);
    if (singleDelimMatch) {
      const num = formatTwoDigit(singleDelimMatch[1]);
      const amt = parseInt(singleDelimMatch[2], 10);
      const hasR = singleDelimMatch[3] === 'r';
      const targetNumbers = hasR && num[0] !== num[1] ? [num, `${num[1]}${num[0]}`] : [num];

      targetNumbers.forEach(n => {
        const current = runningTotals[n] || 0;
        const newTot = current + amt;
        runningTotals[n] = newTot;
        parsedItems.push({
          number: n,
          amount: amt,
          sourceText: token,
          isFormula: hasR,
          formulaType: hasR ? 'အာ (R)' : undefined,
          currentTotal: current,
          newTotal: newTot,
          isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
        });
      });
      continue;
    }

    // 10. Multi-token format:
    // e.g., '00' followed by '100', or '12' followed by 'r' followed by '500', or '12' '500' 'r'
    if (/^\d{2}$/.test(token)) {
      const num = formatTwoDigit(token);
      let amt = 0;
      let isR = false;

      // Check next tokens
      if (i + 1 < tokens.length) {
        const next = tokens[i + 1];
        if (next === 'r' || next === 'အာ') {
          isR = true;
          if (i + 2 < tokens.length && /^\d+$/.test(tokens[i + 2])) {
            amt = parseInt(tokens[i + 2], 10);
            i += 2;
          }
        } else if (/^\d+$/.test(next)) {
          amt = parseInt(next, 10);
          i += 1;
          // check if next next is 'r'
          if (i + 1 < tokens.length && (tokens[i + 1] === 'r' || tokens[i + 1] === 'အာ')) {
            isR = true;
            i += 1;
          }
        }
      }

      if (amt > 0) {
        const targetNumbers = isR && num[0] !== num[1] ? [num, `${num[1]}${num[0]}`] : [num];
        targetNumbers.forEach(n => {
          const current = runningTotals[n] || 0;
          const newTot = current + amt;
          runningTotals[n] = newTot;
          parsedItems.push({
            number: n,
            amount: amt,
            sourceText: isR ? `${num} R ${amt}` : `${num} ${amt}`,
            isFormula: isR,
            formulaType: isR ? 'အာ (R)' : undefined,
            currentTotal: current,
            newTotal: newTot,
            isOverLimit: limitPerNumber > 0 && newTot > limitPerNumber,
          });
        });
        continue;
      }
    }

    // If it didn't match known patterns, mark as unparsed token
    if (token.length > 0) {
      errorTokens.push(token);
    }
  }

  const totalParsedAmount = parsedItems.reduce((acc, item) => acc + item.amount, 0);

  return {
    items: parsedItems,
    totalParsedAmount,
    errorTokens,
  };
}
