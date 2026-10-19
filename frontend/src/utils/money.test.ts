import { formatMoney, isValidAmount, minorUnits, sumMoney } from './money';

describe('money utilities', () => {
  it('formats amounts in their own currency', () => {
    expect(formatMoney(1234.5, 'CAD', 'en-CA')).toBe('$1,234.50');
    expect(formatMoney(-20, 'USD', 'en-US')).toBe('-$20.00');
    expect(formatMoney(1500, 'JPY', 'en-US')).toBe('¥1,500');
  });

  it('knows each currency’s minor units', () => {
    expect(minorUnits('CAD')).toBe(2);
    expect(minorUnits('JPY')).toBe(0);
    expect(minorUnits('BHD')).toBe(3);
  });

  it('sums exactly, without floating-point drift', () => {
    expect(0.1 + 0.2).not.toBe(0.3);
    expect(sumMoney([0.1, 0.2], 'CAD')).toBe(0.3);
    expect(sumMoney([2000, 400, 0.01], 'CAD')).toBe(2400.01);
  });

  it('validates amount text against the currency precision', () => {
    expect(isValidAmount('84.37', 'CAD')).toBe(true);
    expect(isValidAmount('84', 'CAD')).toBe(true);
    expect(isValidAmount('84.375', 'CAD')).toBe(false);
    expect(isValidAmount('10.5', 'JPY')).toBe(false);
    expect(isValidAmount('-5', 'CAD')).toBe(false);
    expect(isValidAmount('-5', 'CAD', { allowNegative: true })).toBe(true);
    expect(isValidAmount('abc', 'CAD')).toBe(false);
  });
});
