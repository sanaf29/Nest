// Display and light client-side arithmetic for money. The server is the authority for every
// stored or reported amount; the client only formats values and previews simple sums.
// Previews use integer minor units (cents) so 0.1 + 0.2 never becomes 0.30000000000000004.

const formatters = new Map<string, Intl.NumberFormat>();

function formatterFor(currency: string, locale?: string): Intl.NumberFormat {
  const key = `${locale ?? ''}|${currency}`;
  let formatter = formatters.get(key);
  if (!formatter) {
    formatter = new Intl.NumberFormat(locale, { style: 'currency', currency });
    formatters.set(key, formatter);
  }
  return formatter;
}

/** Formats an amount in its currency, e.g. formatMoney(1234.5, 'CAD') → "CA$1,234.50" (locale dependent). */
export function formatMoney(amount: number, currency: string, locale?: string): string {
  return formatterFor(currency, locale).format(amount);
}

/** Number of decimal places the currency uses (CAD 2, JPY 0, BHD 3). */
export function minorUnits(currency: string): number {
  return formatterFor(currency, 'en').resolvedOptions().maximumFractionDigits ?? 2;
}

export function toMinor(amount: number, currency: string): number {
  return Math.round(amount * 10 ** minorUnits(currency));
}

export function fromMinor(minor: number, currency: string): number {
  return minor / 10 ** minorUnits(currency);
}

/** Exact sum of amounts in one currency. */
export function sumMoney(amounts: number[], currency: string): number {
  return fromMinor(amounts.reduce((total, a) => total + toMinor(a, currency), 0), currency);
}

/** True if the text is a valid amount for the currency: digits with at most its minor units. */
export function isValidAmount(text: string, currency: string, { allowNegative = false } = {}): boolean {
  const units = minorUnits(currency);
  const decimals = units === 0 ? '' : `(\\.\\d{1,${units}})?`;
  return new RegExp(`^${allowNegative ? '-?' : ''}\\d+${decimals}$`).test(text.trim());
}

export const COMMON_CURRENCIES = ['CAD', 'USD', 'EUR', 'GBP', 'MAD', 'JPY', 'AUD', 'CHF'] as const;
