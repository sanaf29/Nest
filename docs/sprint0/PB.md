# Nest — Product Backlog

## Project Overview

Nest is a personal finance and budgeting application. Users track their accounts, income, and expenses, plan a monthly budget, set aside savings, and work toward financial goals. Nest records money manually. It does not connect to banks or move real money.

**Priorities:** High = required for a usable MVP of that sprint · Medium = significantly improves value · Low = nice to have / stretch.

**Status values:** Not Started · In Progress · Done.

Requirement references (FR-x / NFR-x) point to [requirements.md](requirements.md).

---

## Epic 1: Authentication and Profile — Sprint 1

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 1.1 | As a user, I want to register with my email and a password, so that my financial data is private to me. | High | 1 | Done |
| 1.2 | As a user, I want to log in and stay authenticated while I use the app, so that I don't re-enter my password for every action. | High | 1 | Done |
| 1.3 | As a user, I want to view and edit my profile and preferred currency, so that new accounts default to the currency I use. | Medium | 1 | Done |

**Acceptance criteria**

- **1.1** Registration requires first name, last name, a valid email, a password of 8–72 characters containing at least one letter and one digit, and a valid ISO 4217 currency. A duplicate email (any letter case) returns `409 Conflict`. The password is stored as a BCrypt hash, and the response never contains it. (FR-1.1–1.3)
- **1.2** Valid credentials return `200` with a bearer token and its expiry. Invalid email or password returns `401` with the same message in both cases. Requests to protected endpoints without a valid token return `401`. (FR-1.4–1.5)
- **1.3** `GET /api/users/me` returns the profile of the token's owner. `PUT /api/users/me` updates first name, last name, and preferred currency. Email and password are not changed by this endpoint. (FR-1.6)

---

## Epic 2: Financial Accounts — Sprint 1

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 2.1 | As a user, I want to create checking, savings, cash, and credit card accounts, so that I can track where my money is. | High | 1 | Done |
| 2.2 | As a user, I want to see each account's current balance, so that I know how much I have (or owe). | High | 1 | Done |
| 2.3 | As a user, I want to rename an account or correct its opening balance, so that my records stay accurate. | Medium | 1 | Done |
| 2.4 | As a user, I want to close an account without losing its history, so that past reports remain correct. | Medium | 1 | Done |

**Acceptance criteria**

- **2.1** Name (1–100 chars), type, currency, and opening balance are required. SAVINGS and CASH accounts cannot start negative. Active account names must be unique for the user (`409`). (FR-2.1–2.2, FR-2.6)
- **2.2** Balance = opening balance + income + incoming transfers − expenses − outgoing transfers. A credit card with a negative balance means money is owed. Users only ever see their own accounts; another user's account ID returns `404`. (FR-2.3, NFR-1)
- **2.3** `PUT /api/accounts/{id}` changes name and opening balance only. The currency and type cannot change once the account exists. (FR-2.4)
- **2.4** `DELETE /api/accounts/{id}` sets the account inactive and returns `204`. Inactive accounts are hidden by default (`?includeInactive=true` shows them), keep their transactions, and reject new transactions with `422`. (FR-2.5)

---

## Epic 3: Categories — Sprint 1

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 3.1 | As a user, I want a ready-made set of income and expense categories, so that I can start tracking immediately. | High | 1 | Done |
| 3.2 | As a user, I want to create my own categories with a colour, so that I can organise spending my way. | Medium | 1 | Done |
| 3.3 | As a user, I want to remove categories I no longer use without breaking old transactions, so that my list stays tidy. | Medium | 1 | Done |

**Acceptance criteria**

- **3.1** Default categories are returned with `isDefault: true`. Attempts to edit or delete them return `403`. (FR-3.1)
- **3.2** Name is 1–50 chars, type is INCOME or EXPENSE, and colour is `#RRGGBB`. A duplicate name for the same type (including default names) returns `409`. (FR-3.2, FR-3.4)
- **3.3** Delete archives the category (`204`). Archived categories are hidden by default, remain attached to existing transactions, and are rejected for new or edited transactions with `422`. (FR-3.3)

---

## Epic 4: Transactions and Transfers — Sprint 1

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 4.1 | As a user, I want to record income and expenses with a category, date, and description, so that I know where my money comes from and goes. | High | 1 | Done |
| 4.2 | As a user, I want to search and filter my transactions page by page, so that I can find specific activity quickly. | High | 1 | Done |
| 4.3 | As a user, I want to edit or delete a transaction, so that I can fix mistakes. | High | 1 | Done |
| 4.4 | As a user, I want to transfer money between my accounts, so that balances stay correct without counting the move as spending. | High | 1 | Done |

**Acceptance criteria**

- **4.1** The amount must be > 0 with no more decimal places than the currency allows. The account must be the user's own and active. The category must be visible to the user, not archived, and of the same type as the transaction. If the request includes a currency, it must equal the account's currency. Violations return `400` (malformed) or `422` (business rule). (FR-4.1–4.4)
- **4.2** `GET /api/transactions` supports `accountId`, `categoryId`, `type`, `from`, `to`, `minAmount`, `maxAmount`, `q`, `page`, `size` (max 100), and `sort`. Results contain only the caller's transactions. (FR-4.5)
- **4.3** Editing re-validates every rule in 4.1. Concurrent edits are detected and return `409`. Transfer entries cannot be edited or deleted through this endpoint (`409`). (FR-4.6, FR-4.9, NFR-6)
- **4.4** `POST /api/transfers` requires two different active accounts owned by the user, in the same currency, and a positive amount. It creates one TRANSFER_OUT and one TRANSFER_IN entry atomically. Deleting a transfer removes both. Transfers never appear in income or spending totals. (FR-4.7–4.9, NFR-5)

---

## Epic 5: Monthly Budgets and Income Allocation — Sprint 2

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 5.1 | As a user, I want to create a budget for a month with my planned income, so that I can plan my spending. | High | 2 | Not Started |
| 5.2 | As a user, I want to set aside savings as a percentage or a fixed amount, so that I pay myself first. | High | 2 | Not Started |
| 5.3 | As a user, I want to assign spending limits to categories, so that I can control where my money goes. | High | 2 | Not Started |
| 5.4 | As a user, I want to see actual spending vs. my limits and what remains, so that I can adjust before overspending. | High | 2 | Not Started |

**Acceptance criteria (summary)** One budget per user, month, and currency (`409` on duplicate). Allocations total no more than the income available after savings. Calculations use `BigDecimal` with HALF_EVEN rounding to the currency's minor units. A transaction belongs to the month of its `transactionDate`. Transfers are excluded from all totals.

## Epic 6: Savings Goals — Sprint 2

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 6.1 | As a user, I want to create a savings goal with a target amount and date, so that I have something concrete to work toward. | High | 2 | Not Started |
| 6.2 | As a user, I want to record contributions and see my progress percentage, so that I stay motivated. | High | 2 | Not Started |
| 6.3 | As a user, I want achieved goals to be marked automatically, so that I can celebrate and review them. | Medium | 2 | Not Started |

**Acceptance criteria (summary)** Contributions are positive, and a contribution cannot be recorded twice (idempotency key). Progress = contributions ÷ target, capped at 100% for display. The goal becomes ACHIEVED when contributions ≥ target.

## Epic 7: Dashboard and Calendar — Sprint 2

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 7.1 | As a user, I want a dashboard with balances, income, expenses, and net cash flow, so that I understand my finances at a glance. | High | 2 | Not Started |
| 7.2 | As a user, I want charts of spending by category and monthly trends, so that I can spot habits. | High | 2 | Not Started |
| 7.3 | As a user, I want a monthly calendar of my transactions, so that I can see when money moves. | Medium | 2 | Not Started |

## Epic 8: Web Frontend — Sprint 2

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 8.1 | As a user, I want to register and log in through the website, so that I can use Nest without technical tools. | High | 2 | Not Started |
| 8.2 | As a user, I want to manage accounts, categories, transactions, and transfers in the UI, so that I can do everything from one place. | High | 2 | Not Started |
| 8.3 | As a user, I want the app to work well on my phone, so that I can log expenses on the go. | Medium | 2 | Not Started |

## Epic 9: Financial Insights — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 9.1 | As a user, I want to be told when a category is near or over its budget, so that I can react in time. | High | 3 | Not Started |
| 9.2 | As a user, I want to know when my spending rises compared with last month, so that I can notice trends. | Medium | 3 | Not Started |
| 9.3 | As a user, I want to know when a savings goal is behind schedule, so that I can adjust contributions. | Medium | 3 | Not Started |

## Epic 10: Recurring Transactions — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 10.1 | As a user, I want to define recurring income and bills (rent, subscriptions, utilities), so that I don't enter them every month. | High | 3 | Not Started |
| 10.2 | As a user, I want due recurring transactions generated automatically exactly once, so that my records are complete without duplicates. | High | 3 | Not Started |
| 10.3 | As a user, I want to pause, edit, or end a recurring transaction, so that my plans can change. | Medium | 3 | Not Started |

## Epic 11: Reminders and Notifications — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 11.1 | As a user, I want reminders before bills and subscription renewals, so that I never pay late. | High | 3 | Not Started |
| 11.2 | As a user, I want a parking reminder with location and expiry time, so that I avoid parking tickets. | Medium | 3 | Not Started |
| 11.3 | As a user, I want an in-app notification centre, so that I can see and dismiss alerts in one place. | High | 3 | Not Started |

## Epic 12: Expense Splitting — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 12.1 | As a user, I want to split a shared expense equally or by custom amounts, so that everyone pays their fair share. | High | 3 | Not Started |
| 12.2 | As a user, I want to see who owes whom and mark debts settled, so that I can keep track of shared costs. | High | 3 | Not Started |

## Epic 13: Reporting and Export — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 13.1 | As a user, I want a monthly report of income vs. expenses and spending by category, so that I can review my month. | High | 3 | Not Started |
| 13.2 | As a user, I want to export my transactions and reports as CSV, so that I can use them in a spreadsheet. | Medium | 3 | Not Started |

## Epic 14: Security, Quality, and Release — Sprint 3

| ID | User Story | Priority | Sprint | Status |
|----|------------|----------|--------|--------|
| 14.1 | As a user, I want my data protected by sound security practices, so that I can trust Nest with my finances. | High | 3 | Not Started |
| 14.2 | As a developer, I want an end-to-end test plan and reproducible setup, so that the release is stable. | High | 3 | Not Started |
