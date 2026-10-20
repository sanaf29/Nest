import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests drive the real UI against the real Spring Boot API and SQL Server.
 * Start the backend first (see README), then: npm run e2e
 *   E2E_API_URL   backend URL for the Vite proxy (default http://localhost:8080)
 *   E2E_BASE_URL  frontend URL (default: Vite dev server started by Playwright on :5173)
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  timeout: 60_000,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // Charts skip their entry animation, so assertions and screenshots see final values.
    contextOptions: { reducedMotion: 'reduce' },
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] }, testIgnore: /mobile/ },
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /mobile/ },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev -- --port 5173 --strictPort',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        env: { VITE_API_PROXY_TARGET: process.env.E2E_API_URL ?? 'http://localhost:8080' },
      },
});
