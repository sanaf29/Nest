#!/usr/bin/env node
// Fills a demo account with three months of realistic CAD activity, for trying the app and
// reviewing the design with real numbers. Uses only the public API, so every business rule applies.
//
//   NEST_API_URL=http://localhost:8081 npm run seed:demo
//
// Creates (or reuses) demo@nest.local. Pass NEST_DEMO_EMAIL / NEST_DEMO_PASSWORD to change them.
// Running it twice adds a second copy of the activity, so use a fresh email for a clean slate.

const API = `${(process.env.NEST_API_URL ?? 'http://localhost:8080').replace(/\/$/, '')}/api`;
const EMAIL = process.env.NEST_DEMO_EMAIL ?? 'demo@nest.local';
const PASSWORD = process.env.NEST_DEMO_PASSWORD ?? 'NestDemo2026';

let token = null;

async function call(method, path, body) {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const error = new Error(`${method} ${path} → ${response.status}: ${data?.detail ?? data?.title ?? text}`);
    error.status = response.status;
    throw error;
  }
  return data;
}

// Dates are local yyyy-MM-dd; nothing is recorded in the future.
const pad = (n) => String(n).padStart(2, '0');
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const today = new Date();
today.setHours(12, 0, 0, 0);
const daysAgo = (n) => new Date(today.getFullYear(), today.getMonth(), today.getDate() - n, 12);
const monthStart = (offset) => new Date(today.getFullYear(), today.getMonth() + offset, 1, 12);

// Deterministic "random" so every run looks the same.
let seed = 42;
const random = () => ((seed = (seed * 16807) % 2147483647) - 1) / 2147483646;
const between = (min, max) => Math.round((min + random() * (max - min)) * 100) / 100;

async function main() {
  try {
    await call('POST', '/auth/register', { firstName: 'Maya', lastName: 'Thompson', email: EMAIL, password: PASSWORD, preferredCurrency: 'CAD' });
    console.log(`Registered ${EMAIL}`);
  } catch (error) {
    if (error.status !== 409) throw error;
    console.log(`${EMAIL} already exists; adding data to it`);
  }
  token = (await call('POST', '/auth/login', { email: EMAIL, password: PASSWORD })).accessToken;

  const account = async (name, accountType, openingBalance) => (await call('POST', '/accounts', { name, accountType, openingBalance })).id;
  const chequing = await account('Everyday Chequing', 'CHECKING', 1850);
  const savings = await account('High-Interest Savings', 'SAVINGS', 7400);
  const cash = await account('Wallet', 'CASH', 240);
  const visa = await account('Visa Infinite', 'CREDIT_CARD', -215.4);

  const categories = await call('GET', '/categories');
  const cat = (name) => {
    const found = categories.find((c) => c.name === name);
    if (!found) throw new Error(`Missing default category ${name}`);
    return found.id;
  };

  let count = 0;
  const tx = async (date, accountId, category, type, amount, description) => {
    if (date > today) return;
    await call('POST', '/transactions', { accountId, categoryId: cat(category), type, amount, description, transactionDate: iso(date) });
    count++;
  };

  // Three months: this month so far and the two before it.
  for (let offset = -2; offset <= 0; offset++) {
    const start = monthStart(offset);
    const day = (d) => new Date(start.getFullYear(), start.getMonth(), d, 12);
    await tx(day(1), chequing, 'Housing', 'EXPENSE', 1650, 'Rent');
    await tx(day(2), visa, 'Subscriptions', 'EXPENSE', 16.99, 'Netflix');
    await tx(day(3), visa, 'Subscriptions', 'EXPENSE', 11.99, 'Spotify');
    await tx(day(4), chequing, 'Utilities', 'EXPENSE', between(68, 92), 'Hydro');
    await tx(day(6), chequing, 'Utilities', 'EXPENSE', 65, 'Internet');
    await tx(day(15), chequing, 'Salary', 'INCOME', 2350, 'Paycheque');
    await tx(day(28), chequing, 'Salary', 'INCOME', 2350, 'Paycheque');
    if (offset === -1) await tx(day(20), chequing, 'Freelance', 'INCOME', 480, 'Logo design project');
    for (const d of [3, 10, 17, 24]) await tx(day(d), visa, 'Groceries', 'EXPENSE', between(85, 145), d % 2 ? 'Loblaws' : 'Farmers market');
    for (const d of [5, 12, 19, 26]) await tx(day(d), visa, 'Dining Out', 'EXPENSE', between(18, 62), ['Sushi night', 'Coffee & brunch', 'Thai takeout', 'Pizza with friends'][d % 4]);
    for (const d of [7, 21]) await tx(day(d), chequing, 'Transportation', 'EXPENSE', between(48, 66), 'Gas');
    await tx(day(1), chequing, 'Transportation', 'EXPENSE', 156, 'Transit pass');
    await tx(day(9), visa, 'Shopping', 'EXPENSE', between(45, 180), offset === 0 ? 'Winter jacket' : 'Home goods');
    await tx(day(14), visa, 'Entertainment', 'EXPENSE', between(24, 70), offset === -1 ? 'Concert tickets' : 'Movie night');
    await tx(day(18), cash, 'Healthcare', 'EXPENSE', between(15, 42), 'Pharmacy');
    if (offset !== -2) await tx(day(22), visa, 'Education', 'EXPENSE', 49, 'Online course');
    await tx(day(25), cash, 'Personal Care', 'EXPENSE', 35, 'Haircut');
  }

  // Move money to savings and pay the card.
  let transfers = 0;
  const transfer = async (date, from, to, amount, description) => {
    if (date > today) return;
    transfers++;
    await call('POST', '/transfers', { sourceAccountId: from, destinationAccountId: to, amount, description, transferDate: iso(date) });
  };
  for (let offset = -2; offset <= 0; offset++) {
    const start = monthStart(offset);
    await transfer(new Date(start.getFullYear(), start.getMonth(), 16, 12), chequing, savings, 500, 'Monthly savings');
    await transfer(new Date(start.getFullYear(), start.getMonth(), 27, 12), chequing, visa, 650, 'Card payment');
  }

  // This month's budget: save 20 % of planned income, limits for the main categories.
  const { year, month } = { year: today.getFullYear(), month: today.getMonth() + 1 };
  const limits = { Housing: 1650, Groceries: 520, 'Dining Out': 160, Transportation: 260, Shopping: 200, Entertainment: 80, Subscriptions: 40, Healthcare: 60, Utilities: 170, Education: 60 };
  try {
    await call('POST', '/budgets', {
      year, month, plannedIncome: 4700, savingsStrategy: 'PERCENTAGE', savingsValue: 20,
      allocations: Object.entries(limits).map(([name, limit]) => ({ categoryId: cat(name), limit })),
    });
  } catch (error) {
    if (error.status !== 409) throw error;
  }

  // Savings goals with some history.
  const nextYear = today.getFullYear() + 1;
  const goal = async (name, targetAmount, targetDate, contributions) => {
    const created = await call('POST', '/savings-goals', { name, targetAmount, targetDate });
    for (const [ago, amount, note] of contributions) {
      await call('POST', `/savings-goals/${created.id}/contributions`, { amount, contributionDate: iso(daysAgo(ago)), note });
    }
  };
  await goal('Emergency Fund', 10000, `${nextYear}-06-30`, [[80, 4000, 'Starting balance'], [50, 1200, null], [20, 1000, 'Bonus']]);
  await goal('New Laptop', 1800, `${today.getFullYear()}-12-15`, [[45, 600, null], [10, 350, null]]);
  await goal('Vacation in Portugal', 3500, `${nextYear}-08-01`, [[60, 800, null], [25, 350, null]]);
  await goal('Tuition', 6000, `${nextYear}-09-01`, [[70, 1500, null], [30, 600, null]]);

  // Upcoming plans: the next paydays, rent, and subscriptions (all start in the future, so nothing is duplicated).
  const next = (d) => {
    const date = new Date(today.getFullYear(), today.getMonth(), d, 12);
    return iso(date > today ? date : new Date(today.getFullYear(), today.getMonth() + 1, d, 12));
  };
  const recurring = (body) => call('POST', '/recurring-transactions', { remindDaysBefore: 1, ...body });
  await recurring({ accountId: chequing, categoryId: cat('Salary'), type: 'INCOME', amount: 2350, description: 'Paycheque', frequency: 'BIWEEKLY', startDate: next(28) });
  await recurring({ accountId: chequing, categoryId: cat('Housing'), type: 'EXPENSE', amount: 1650, description: 'Rent', frequency: 'MONTHLY', startDate: next(1) });
  await recurring({ accountId: visa, categoryId: cat('Subscriptions'), type: 'EXPENSE', amount: 16.99, description: 'Netflix', frequency: 'MONTHLY', startDate: next(2) });
  await recurring({ accountId: visa, categoryId: cat('Subscriptions'), type: 'EXPENSE', amount: 11.99, description: 'Spotify', frequency: 'MONTHLY', startDate: next(3) });

  // Reminders.
  const inDays = (n, hour) => new Date(today.getFullYear(), today.getMonth(), today.getDate() + n, hour).toISOString();
  await call('POST', '/reminders', { type: 'BILL_PAYMENT', title: 'Pay the Visa bill', dueAt: inDays(4, 9), offsetMinutes: 60 });
  await call('POST', '/reminders', { type: 'SUBSCRIPTION_RENEWAL', title: 'Gym membership renews', dueAt: inDays(9, 10), offsetMinutes: 1440 });

  console.log(`Seeded ${count} transactions, ${transfers} transfers, a budget, 4 goals, 4 recurring rules, and 2 reminders.`);
  console.log(`Log in as ${EMAIL} / ${PASSWORD}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
