import type { SplitMethod } from '../types/api';
import { fromMinor, toMinor } from './money';

/**
 * Preview of how the server will split an expense: largest-remainder apportionment in minor
 * units, ties to the earlier participant, so the preview matches the saved shares to the cent.
 */
export function apportion(total: number, weights: number[], currency: string): number[] {
  const totalMinor = toMinor(total, currency);
  const weightSum = weights.reduce((a, b) => a + b, 0);
  if (weights.length === 0 || weightSum <= 0) return weights.map(() => 0);
  const exact = weights.map((w) => (totalMinor * w) / weightSum);
  const floors = exact.map((e) => Math.floor(e + 1e-9));
  let leftover = totalMinor - floors.reduce((a, b) => a + b, 0);
  const order = exact
    .map((e, i) => ({ i, remainder: e - Math.floor(e + 1e-9) }))
    .sort((a, b) => b.remainder - a.remainder || a.i - b.i);
  for (let k = 0; leftover > 0; k++, leftover--) {
    floors[order[k % order.length]!.i]! += 1;
  }
  return floors.map((minor) => fromMinor(minor, currency));
}

export interface SplitPreview {
  shares: number[];
  error: string | null;
}

/** Shares for the chosen method, or the reason the input can't be split yet. */
export function previewSplit(method: SplitMethod, total: number, values: number[], currency: string): SplitPreview {
  if (!(total > 0) || values.length < 2) return { shares: values.map(() => 0), error: null };
  if (method === 'EQUAL') return { shares: apportion(total, values.map(() => 1), currency), error: null };
  if (method === 'PERCENTAGE') {
    const sum = Math.round(values.reduce((a, b) => a + b, 0) * 100) / 100;
    if (sum !== 100) return { shares: values.map(() => 0), error: `Percentages add up to ${sum}%, not 100%.` };
    return { shares: apportion(total, values, currency), error: null };
  }
  const sumMinor = values.reduce((a, v) => a + toMinor(v, currency), 0);
  const diff = toMinor(total, currency) - sumMinor;
  return {
    shares: values,
    error: diff === 0 ? null : `Amounts add up to ${fromMinor(sumMinor, currency).toFixed(2)}; ${Math.abs(fromMinor(diff, currency)).toFixed(2)} ${diff > 0 ? 'still to assign' : 'too much'}.`,
  };
}
