import { expect, test } from '@playwright/test';
import { addAccount, register, snapshot, uniqueEmail } from './helpers';

/**
 * The Sprint 2 user journey from the end-to-end test plan: register, accounts, income, expense,
 * transfer, budget, savings goal, dashboard, calendar, logout. Runs against a real backend.
 */
test('a new user can plan and track a month of finances', async ({ page }) => {
  await register(page, uniqueEmail());
  await expect(page.getByText('Add your first account')).toBeVisible();
  await snapshot(page, '01-dashboard-empty');

  // Accounts
  await addAccount(page, 'Chequing', 'CHECKING', '1000');
  await addAccount(page, 'Savings', 'SAVINGS', '0');
  await addAccount(page, 'Visa', 'CREDIT_CARD', '-100');

  // A savings account cannot start negative (client-side rule mirroring the server).
  await page.getByRole('button', { name: 'Add account' }).first().click();
  await page.getByRole('dialog').getByLabel('Name').fill('Bad');
  await page.getByRole('dialog').getByLabel('Type').selectOption('SAVINGS');
  await page.getByRole('dialog').getByLabel('Opening balance').fill('-5');
  await page.getByRole('dialog').getByRole('button', { name: 'Add account' }).click();
  await expect(page.getByText('A savings account cannot start below zero.')).toBeVisible();
  await page.keyboard.press('Escape');

  // Income, expense, transfer
  await page.goto('/transactions');
  await page.getByRole('button', { name: 'Add transaction' }).click();
  let dialog = page.getByRole('dialog');
  await dialog.getByRole('radio', { name: 'Income' }).click();
  await dialog.getByLabel('Amount (CAD)').fill('3000');
  await dialog.getByLabel('Category').selectOption({ label: 'Salary' });
  await dialog.getByLabel('Description (optional)').fill('October pay');
  await dialog.getByRole('button', { name: 'Add income' }).click();
  await expect(dialog).toBeHidden();

  await page.getByRole('button', { name: 'Add transaction' }).click();
  dialog = page.getByRole('dialog');
  await dialog.getByLabel('Amount (CAD)').fill('84.37');
  await dialog.getByLabel('Category').selectOption({ label: 'Groceries' });
  await dialog.getByLabel('Description (optional)').fill('Weekly groceries');
  await dialog.getByRole('button', { name: 'Add expense' }).click();
  await expect(dialog).toBeHidden();

  await page.getByRole('button', { name: 'Transfer' }).click();
  dialog = page.getByRole('dialog');
  await dialog.getByLabel('From').selectOption({ label: 'Chequing (CAD)' });
  await dialog.getByLabel('To').selectOption({ label: 'Savings (CAD)' });
  await dialog.getByLabel('Amount (CAD)').fill('500');
  await dialog.getByRole('button', { name: 'Transfer' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.getByText('4 transactions')).toBeVisible(); // income, expense, and two transfer entries
  await snapshot(page, '02-transactions');

  // Search
  await page.getByLabel('Search').fill('grocer');
  await expect(page.getByText('1 transaction', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters' }).click();

  // Balances derived from activity: 1000 + 3000 − 84.37 − 500
  await page.goto('/accounts');
  const accountRow = (name: string) =>
    page.getByRole('main').getByRole('listitem').filter({ has: page.getByRole('link', { name, exact: true }) });
  await expect(accountRow('Chequing')).toContainText('3,415.63');
  await expect(accountRow('Savings')).toContainText('500.00');
  await expect(accountRow('Visa')).toContainText('owed');
  await snapshot(page, '03-accounts');

  // Budget: 3000 income, 20 % savings, Groceries limit 500
  await page.goto('/budget');
  await page.getByRole('button', { name: 'Create budget' }).click();
  await page.getByLabel('Planned income (CAD)').fill('3000');
  await expect(page.getByLabel('Plan preview')).toContainText('600.00');
  await page.getByRole('button', { name: 'Add category limit' }).click();
  await page.getByLabel('Category 1', { exact: true }).selectOption({ label: 'Groceries' });
  await page.getByLabel('Limit').fill('500');
  await page.getByRole('button', { name: 'Create budget' }).click();
  await expect(page.getByRole('progressbar', { name: 'Groceries spending' })).toHaveAttribute('aria-valuenow', '17');
  await expect(page.getByText('On track')).toBeVisible();
  await snapshot(page, '04-budget');

  // Savings goal with a contribution
  await page.goto('/goals');
  await page.getByRole('button', { name: 'New goal' }).click();
  await page.getByRole('dialog').getByLabel('Goal').fill('Emergency fund');
  await page.getByRole('dialog').getByLabel('Target (CAD)').fill('1000');
  await page.getByRole('dialog').getByRole('button', { name: 'Create goal' }).click();
  await page.getByRole('button', { name: 'Contribute' }).click();
  await page.getByRole('dialog').getByLabel('Amount (CAD)').fill('250');
  await page.getByRole('dialog').getByRole('button', { name: 'Add contribution' }).click();
  await expect(page.getByRole('progressbar', { name: 'Emergency fund progress' })).toHaveAttribute('aria-valuenow', '25');
  await snapshot(page, '05-goals');

  // Dashboard reflects real data; the transfer is not spending.
  await page.goto('/');
  const stat = (label: string) => page.getByRole('group', { name: label, exact: true });
  await expect(stat('Income')).toContainText('3,000.00');
  await expect(stat('Expenses')).toContainText('84.37');
  await expect(stat('Net cash flow')).toContainText('2,915.63');
  await expect(page.getByRole('list', { name: 'Category limits' })).toContainText('Groceries');
  await expect(page.getByRole('progressbar', { name: 'Emergency fund progress' })).toBeVisible();
  await snapshot(page, '06-dashboard');

  // Insights: the monthly review with the category donut.
  await page.goto('/insights');
  await expect(page.getByRole('group', { name: 'Savings rate' })).toContainText('97%'); // 2,915.63 of 3,000 kept
  await expect(page.getByRole('list', { name: 'Spending by category' })).toContainText('Groceries');

  // Calendar shows today's activity.
  await page.goto('/calendar');
  const today = page.getByRole('button', { name: /income .*3,000\.00, expenses .*84\.37/ });
  await today.click();
  await expect(page.getByText('October pay').or(page.getByText('Weekly groceries')).first()).toBeVisible();
  await snapshot(page, '07-calendar');

  // Logout protects the app again.
  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await page.goto('/accounts');
  await expect(page).toHaveURL(/\/login$/);
});

test('one user cannot open another user’s account page', async ({ page, browser }) => {
  await register(page, uniqueEmail('owner'));
  await addAccount(page, 'Private', 'CHECKING', '10');
  await page.getByRole('link', { name: 'Private' }).click();
  const url = page.url();

  const other = await browser.newPage();
  await register(other, uniqueEmail('intruder'));
  await other.goto(new URL(url).pathname);
  await expect(other.getByRole('alert')).toContainText('was not found');
  await other.close();
});
