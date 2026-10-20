import { expect, test } from '@playwright/test';
import { register, snapshot, uniqueEmail } from './helpers';

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
