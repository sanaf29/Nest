import { expect, test } from '@playwright/test';
import { addAccount, register, snapshot, uniqueEmail } from './helpers';

test('the app is usable on a phone', async ({ page }) => {
  await register(page, uniqueEmail('mobile'));
  await snapshot(page, 'm1-dashboard');

  await page.getByRole('button', { name: 'Open menu' }).click();
  const menu = page.getByRole('dialog', { name: 'Menu' });
  await expect(menu.getByRole('link', { name: 'Budget' })).toBeVisible();
  await snapshot(page, 'm2-menu');
  await menu.getByRole('link', { name: 'Accounts' }).click();
  await expect(page.getByRole('heading', { name: 'Accounts', exact: true })).toBeVisible();

  // No horizontal scrolling at phone width.
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});

test('the tab bar and the + screen record an expense', async ({ page }) => {
  await register(page, uniqueEmail('tabs'));
  await addAccount(page, 'Chequing', 'CHECKING', '500');

  const tabs = page.getByRole('navigation', { name: 'Tabs' });
  await tabs.getByRole('link', { name: 'Home' }).click();
  await tabs.getByRole('link', { name: 'Add transaction' }).click();
  await page.getByLabel('Amount (CAD)').fill('42.50');
  await page.getByRole('radio', { name: 'Groceries' }).click();
  await page.getByLabel('Notes (optional)').fill('Corner store');
  await snapshot(page, 'm3-add');
  await page.getByRole('button', { name: 'Save transaction' }).click();

  // Back on Home, the expense shows in the overview and recent list.
  await expect(page.getByRole('group', { name: 'Expenses' })).toContainText('42.50');
  await expect(page.getByText('Corner store')).toBeVisible();

  await tabs.getByRole('link', { name: 'Profile' }).click();
  await expect(page.getByRole('heading', { name: 'Profile & settings' })).toBeVisible();
  await page.getByRole('radio', { name: 'Light' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
});
