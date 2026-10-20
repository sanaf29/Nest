import { applyTheme, readThemePreference, setThemePreference } from './theme';

describe('theme preference', () => {
  afterEach(() => localStorage.clear());

  it('defaults to dark and remembers a choice', () => {
    expect(readThemePreference()).toBe('dark');
    setThemePreference('light');
    expect(readThemePreference()).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('ignores unknown stored values', () => {
    localStorage.setItem('nest.theme', 'neon');
    expect(readThemePreference()).toBe('dark');
    applyTheme();
    expect(document.documentElement.dataset.theme).toBe('dark');
  });
});
