// Appearance preference. Dark is the default; the choice is a per-device convenience, so it
// lives in localStorage and the app works normally if storage is unavailable.

export type ThemePreference = 'dark' | 'light' | 'system';

const KEY = 'nest.theme';
const listeners = new Set<() => void>();

export function readThemePreference(): ThemePreference {
  try {
    const value = localStorage.getItem(KEY);
    return value === 'light' || value === 'system' ? value : 'dark';
  } catch {
    return 'dark';
  }
}

function systemPrefersLight(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: light)').matches;
}

export function resolveTheme(preference: ThemePreference): 'dark' | 'light' {
  return preference === 'system' ? (systemPrefersLight() ? 'light' : 'dark') : preference;
}

export function applyTheme(preference: ThemePreference = readThemePreference()): void {
  const theme = resolveTheme(preference);
  document.documentElement.dataset.theme = theme;
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'light' ? '#f3f5fa' : '#111728');
  listeners.forEach((listener) => listener());
}

export function setThemePreference(preference: ThemePreference): void {
  try {
    localStorage.setItem(KEY, preference);
  } catch {
    // Storage blocked: the choice still applies for this visit.
  }
  applyTheme(preference);
}

/** Applies the stored theme now and follows the OS setting when the preference is "system". */
export function initTheme(): void {
  applyTheme();
  if (typeof window.matchMedia === 'function') {
    window.matchMedia('(prefers-color-scheme: light)').addEventListener?.('change', () => {
      if (readThemePreference() === 'system') applyTheme('system');
    });
  }
}

export function onThemeChange(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
