import { readFile } from 'node:fs/promises';
import { expect, test } from '@playwright/test';
import { addAccount, PASSWORD, register, snapshot, uniqueEmail } from './helpers';

test('recurring bills, splits, notifications, reminders, reports, and settings', async ({ page }) => {
  const email = uniqueEmail('s3');
  await register(page, email);
  await addAccount(page, 'Chequing', 'CHECKING', '2000');

  // Recurring: a weekly bill that started two weeks ago is caught up immediately (3 occurrences).
  await page.goto('/recurring');
  await page.getByRole('button', { name: 'Schedule' }).first().click();
  let dialog = page.getByRole('dialog');
  await dialog.getByLabel('Description').fill('Cleaning service');
  await dialog.getByLabel('Amount (CAD)').fill('40');
  await dialog.getByLabel('Repeats').selectOption('WEEKLY');
  await dialog.getByLabel('Category').selectOption({ label: 'Housing' });
  const start = new Date(Date.now() - 14 * 864e5);
  const iso = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-${String(start.getDate()).padStart(2, '0')}`;
  await dialog.getByLabel('First date').fill(iso);
  await dialog.getByLabel('Remind me').selectOption('1');
  await dialog.getByRole('button', { name: 'Schedule' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByText('Cleaning service')).toBeVisible();
  await page.getByRole('button', { name: 'Record due now' }).click();
  await expect(page.getByText('Everything is up to date.')).toBeVisible(); // nothing generated twice
  await snapshot(page, 's3-01-recurring');

  await page.goto('/transactions');
  await page.getByLabel('Search').fill('Cleaning');
  await expect(page.getByText(/^[34] transactions$/)).toBeVisible(); // 3 or 4 depending on the UTC date

  // Split 10.00 three ways: 3.34 / 3.33 / 3.33.
  await page.goto('/shared');
  await page.getByRole('button', { name: 'Split an expense' }).first().click();
  dialog = page.getByRole('dialog');
  await dialog.getByLabel('What for').fill('Pizza');
  await dialog.getByLabel('Total (CAD)').fill('10');
  await dialog.getByLabel('Person 2', { exact: true }).fill('Alex');
  await dialog.getByRole('button', { name: 'Add person' }).click();
  await dialog.getByLabel('Person 3', { exact: true }).fill('Sam');
  await expect(dialog.getByLabel('Avery share')).toContainText('3.34');
  await dialog.getByRole('button', { name: 'Save split' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByText('Alex owes you')).toBeVisible();
  await page.getByRole('button', { name: 'Mark Alex as settled' }).click();
  await expect(page.getByText('Alex owes you')).toBeHidden();
  await expect(page.getByText('Sam owes you')).toBeVisible();
  await snapshot(page, 's3-02-split');

  // Reaching a goal creates a notification; the bell shows it.
  await page.goto('/goals');
  await page.getByRole('button', { name: 'New goal' }).click();
  await page.getByRole('dialog').getByLabel('Goal').fill('Headphones');
  await page.getByRole('dialog').getByLabel('Target (CAD)').fill('200');
  await page.getByRole('dialog').getByRole('button', { name: 'Create goal' }).click();
  await page.getByRole('button', { name: 'Contribute' }).click();
  await page.getByRole('dialog').getByLabel('Amount (CAD)').fill('200');
  await page.getByRole('dialog').getByRole('button', { name: 'Add contribution' }).click();
  await page.goto('/');
  const bell = page.getByRole('link', { name: /Notifications, \d+ unread/ }).first();
  await expect(bell).toBeVisible();
  await bell.click();
  await expect(page.getByText(/Goal achieved: Headphones/)).toBeVisible();
  await page.getByRole('button', { name: 'Mark all read' }).click();
  await expect(page.getByRole('link', { name: 'Notifications', exact: true }).first()).toBeVisible();
  await snapshot(page, 's3-03-notifications');

  // Parking reminder.
  await page.goto('/reminders');
  await page.getByRole('button', { name: 'New reminder' }).click();
  dialog = page.getByRole('dialog');
  await dialog.getByLabel('Kind').selectOption('PARKING');
  await dialog.getByLabel('Where you parked').fill('Level 2, spot B14');
  const expiry = new Date(Date.now() + 2 * 3600_000);
  const pad = (n: number) => String(n).padStart(2, '0');
  await dialog.getByLabel('Parking expires').fill(
    `${expiry.getFullYear()}-${pad(expiry.getMonth() + 1)}-${pad(expiry.getDate())}T${pad(expiry.getHours())}:${pad(expiry.getMinutes())}`);
  await dialog.getByRole('button', { name: 'Create reminder' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByText('Level 2, spot B14', { exact: false })).toBeVisible();
  await snapshot(page, 's3-04-reminders');

  // Insights page loads with its disclaimer.
  await page.goto('/insights');
  await expect(page.getByText(/not financial advice/)).toBeVisible();

  // Report and CSV export.
  await page.goto('/reports');
  await expect(page.getByText('Expenses by category')).toBeVisible();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Transactions CSV' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^nest-transactions-.*\.csv$/);
  const csv = await readFile((await download.path())!, 'utf8');
  expect(csv).toContain('"Date","Type","Account","Category","Description","Amount","Currency","Transfer ID"');
  expect(csv).toContain('"Cleaning service"');
  await snapshot(page, 's3-05-reports');

  // Settings: change password, then log in with the new one.
  await page.goto('/settings');
  await page.getByLabel('Current password').fill(PASSWORD);
  await page.getByLabel('New password', { exact: true }).fill('N3wPassw0rd!');
  await page.getByLabel('Confirm new password').fill('N3wPassw0rd!');
  await page.getByRole('button', { name: 'Change password' }).click();
  await expect(page.getByText('Password changed')).toBeVisible();
  await page.getByRole('button', { name: 'Log out' }).click();
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill('N3wPassw0rd!');
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByRole('heading', { name: /^Good (morning|afternoon|evening), Avery$/ })).toBeVisible();
});
