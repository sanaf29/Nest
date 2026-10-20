import { expect, type Page } from '@playwright/test';

export const PASSWORD = 'Str0ngPassw0rd';

export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1e6)}@example.test`;
}

/** Saves a full-page screenshot when E2E_SCREENSHOT_DIR is set (used for visual review, not committed). */
export async function snapshot(page: Page, name: string): Promise<void> {
  const dir = process.env.E2E_SCREENSHOT_DIR;
  if (dir) await page.screenshot({ path: `${dir}/${name}.png`, fullPage: true });
}

export async function register(page: Page, email: string, currency = 'CAD'): Promise<void> {
  await page.goto('/register');
  await page.getByLabel('First name').fill('Avery');
  await page.getByLabel('Last name').fill('Tester');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(PASSWORD);
  await page.getByLabel('Preferred currency').selectOption(currency);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('heading', { name: /^Good (morning|afternoon|evening), Avery$/ })).toBeVisible();
}

export async function addAccount(page: Page, name: string, type: string, opening: string): Promise<void> {
  await page.goto('/accounts');
  await page.getByRole('button', { name: 'Add account' }).first().click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Name').fill(name);
  await dialog.getByLabel('Type').selectOption(type);
  await dialog.getByLabel('Opening balance').fill(opening);
  await dialog.getByRole('button', { name: 'Add account' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByRole('link', { name })).toBeVisible();
}
