import { apportion, previewSplit } from './split';

describe('split preview', () => {
  it('matches the server: 10.00 among three is 3.34 + 3.33 + 3.33', () => {
    expect(apportion(10, [1, 1, 1], 'CAD')).toEqual([3.34, 3.33, 3.33]);
    expect(apportion(1000, [1, 1, 1], 'JPY')).toEqual([334, 333, 333]);
  });

  it('always sums exactly to the total', () => {
    for (const [total, n] of [[0.01, 3], [100, 7], [1999.99, 6]] as const) {
      const shares = apportion(total, Array(n).fill(1), 'CAD');
      expect(Math.round(shares.reduce((a, b) => a + b, 0) * 100)).toBe(Math.round(total * 100));
    }
  });

  it('validates percentages and custom amounts', () => {
    expect(previewSplit('PERCENTAGE', 1000, [60, 40], 'CAD')).toEqual({ shares: [600, 400], error: null });
    expect(previewSplit('PERCENTAGE', 1000, [60, 30], 'CAD').error).toBe('Percentages add up to 90%, not 100%.');
    expect(previewSplit('CUSTOM', 100, [60, 30], 'CAD').error).toBe('Amounts add up to 90.00; 10.00 still to assign.');
    expect(previewSplit('CUSTOM', 100, [60, 40], 'CAD').error).toBeNull();
  });
});
