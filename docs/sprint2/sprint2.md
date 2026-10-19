# Sprint 2 Planning — Nest

## Sprint Goal

Turn Nest from a transaction tracker into a budgeting application: monthly budgets with savings allocation, savings goals, dashboard analytics, and a complete React + TypeScript web interface for every Sprint 1 and Sprint 2 feature.

Sprint 2 builds on the Sprint 1 architecture; nothing is replaced.

---

## Selected Backlog Items

| PB ID | User Story | Priority | Status |
|-------|------------|----------|--------|
| 5.1 | Create a monthly budget with planned income | High | Done |
| 5.2 | Savings as a percentage or fixed amount | High | Done |
| 5.3 | Category spending limits | High | Done |
| 5.4 | Actual vs. limits and remaining budget | High | Done |
| 6.1 | Create a savings goal | High | Not Started |
| 6.2 | Record contributions and see progress | High | Not Started |
| 6.3 | Achieved goals marked automatically | Medium | Not Started |
| 7.1 | Dashboard: balances, income, expenses, net cash flow | High | Not Started |
| 7.2 | Charts: spending by category, monthly trends | High | Not Started |
| 7.3 | Monthly transaction calendar | Medium | Not Started |
| 8.1 | Register and log in through the website | High | Not Started |
| 8.2 | Manage accounts, categories, transactions, transfers in the UI | High | Not Started |
| 8.3 | Works well on a phone | Medium | Not Started |

## Implementation Steps

| # | Increment | Branch | Contents | Status |
|---|-----------|--------|----------|--------|
| 1 | Budgets | `feature/budgets` | `MonthlyBudget`, `BudgetAllocation`, `SavingsAllocationStrategy` (percentage / fixed), budget summary, V6, `/api/budgets` | Done |
| 2 | Savings goals | `feature/savings-goals` | `SavingsGoal`, `GoalContribution`, idempotent contributions, V7, `/api/savings-goals` | Not Started |
| 3 | Dashboard APIs | `feature/dashboard` | Balances by currency, cash flow, spending by category, monthly trends | Not Started |
| 4 | Frontend foundation | `feature/frontend-foundation` | Vite + React + TS + Tailwind, API client, auth context, protected routes, layout, login/register | Not Started |
| 5 | Frontend features | `feature/frontend-*` | Dashboard, accounts, transactions + transfers + categories, budget planner, goals, calendar | Not Started |
| 6 | Sprint close | `docs/sprint2-review` | Updated class diagram, sequence diagrams, testing, SR2 | Not Started |

## Technical Decisions

| Topic | Decision |
|-------|----------|
| Month assignment | A transaction belongs to the calendar month of its `transactionDate` (a `LocalDate` exactly as entered; no timezone conversion). A month is `[first day, last day]` inclusive, using `YearMonth` (28/29/30/31 days, leap years). |
| Transfers | Excluded from every budget and dashboard total. Only `INCOME` and `EXPENSE` count. |
| Refunds | Recorded as `INCOME` in the default *Refunds* category and counted as income. They are not netted against an expense category, because the original category of a refunded purchase is unknown. (This replaces the Sprint 0 note that refunds would become negative spending.) |
| Budget scope | One budget per user, month, and currency (`409` on duplicate). Actuals include only accounts in the budget's currency, open or closed. Amounts in other currencies are never mixed. |
| Savings allocation | Strategy pattern: `SavingsAllocationStrategy` with `PercentageSavingsStrategy` (0–100 %) and `FixedAmountSavingsStrategy`. The `SavingsStrategyType` enum is the factory. The savings target is derived, not stored: `HALF_EVEN` rounding to the currency's minor units. |
| Spendable amount | `plannedIncome − savingsTarget`. A fixed savings amount above planned income is refused (422). |
| Allocations | Expense categories only, visible to the user and not archived when set. One per category. Their total may not exceed the spendable amount (422). |
| Category status | `ON_TRACK` below 80 % of the limit, `NEAR_LIMIT` from 80 %, `EXCEEDED` above 100 %. |
| Savings progress | Actual savings = actual income − actual expenses (net cash flow). Progress = actual savings ÷ savings target. It can exceed 100 % or go negative, and is `null` when the target is 0. |
| Savings goals | Contributions are **tracked allocations**: they record that money is set aside for a goal, but do not move money between accounts. To move money, use a transfer. Progress = Σ contributions ÷ target. The status becomes `ACHIEVED` automatically when contributions reach the target, and returns to `ACTIVE` if the target is raised or a contribution is removed. |
| Duplicate contributions | Optional client `idempotencyKey` per contribution, unique per goal. Replaying the same key returns the original contribution (200) instead of adding it again. |
| Dashboard currencies | Every total is grouped by currency. Nothing is summed across currencies. "Net worth" is per currency, over open accounts. |
| Frontend auth | The JWT is stored in `localStorage` with its expiry so a refresh keeps you logged in. Trade-off: readable by XSS, so the app renders no user HTML and the token is short-lived. Revisited in the Sprint 3 security review. |
| Frontend API access | Vite dev server proxies `/api` to the backend (no CORS needed in development). Typed service modules mirror backend DTOs. |

---

## Completed Work and Evidence

### Increment 1: Budgets
- `SavingsAllocationStrategy` with `PercentageSavingsStrategy` and `FixedAmountSavingsStrategy`; `SavingsStrategyType` is the factory
- `MonthlyBudget` aggregate with `BudgetAllocation` children; plan and allocations revised together (`revise`), updating existing rows in place so the `(budget, category)` unique key cannot collide during flush
- `MonthlyActuals`, `BudgetSummary`, `BudgetStatus`: pure plan-versus-actual calculations
- `LedgerAnalytics`: month range from `YearMonth`, INCOME/EXPENSE only, single currency; grouped totals computed in SQL
- Flyway `V6__create_budgets.sql`; `/api/budgets` (create, list, get by month, update, delete, summary)
- Tests: `SavingsStrategyTest` (incl. HALF_EVEN cases), `MonthlyBudgetTest`, `BudgetServiceImplTest`, `BudgetIT` (month boundaries, leap day, transfers and other currencies excluded, cross-user)

| Check | Result |
|-------|--------|
| `./mvnw verify` | 198 unit, 70 integration, 0 failures |
| Postman | Budgets folder added; run together with increments 2–3 (see below) |
