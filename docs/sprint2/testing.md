# Sprint 2 — Testing

## Summary

| Layer | Tooling | Count | Result |
|-------|---------|------:|--------|
| Backend unit | JUnit 5, Mockito | 219 (57 new) | All pass |
| Backend integration | Spring Boot Test, MockMvc, Testcontainers SQL Server | 81 (19 new) | All pass |
| API | Postman + Newman | 111 requests, 224 assertions | All pass |
| Frontend unit and component | Vitest, React Testing Library | 40 | All pass |
| End-to-end | Playwright (Chromium), real backend and SQL Server | 3 | All pass |

## How to Run

```bash
# Backend (stop the dev DB first on memory-limited machines)
cd backend && ./mvnw verify

# API (backend running)
npx newman run postman/Nest.postman_collection.json -e postman/Nest.local.postman_environment.json

# Frontend
cd frontend && npm test && npm run build

# End-to-end (backend running; first time: npx playwright install chromium)
cd frontend && npm run e2e                       # backend on :8080
E2E_API_URL=http://localhost:8081 npm run e2e    # backend elsewhere
```

## New Backend Tests

| Class | Tests | Covers |
|-------|------:|--------|
| `SavingsStrategyTest` | 16 | Specification example (3000 × 20 % = 600), HALF_EVEN cases (50.025 → 50.02, 50.075 → 50.08, JPY), range/precision, fixed > income, factory, status thresholds |
| `MonthlyBudgetTest` | 18 | Derived savings/spendable, allocation rules (expense only, visible, unarchived, unique, positive, ≤ spendable), atomic revise keeps rows, summary arithmetic and statuses, zero income/savings, overspending, wrong currency |
| `BudgetServiceImplTest` | 5 | Preferred currency, duplicate month 409, invisible category 404, summary wiring, cross-user 404 |
| `SavingsGoalTest` | 11 | Creation rules, half-even progress, achievement and reopening (target raised, contribution removed), contribution rules, key reuse |
| `SavingsGoalServiceImplTest` | 6 | Preferred currency, duplicate name, default date, idempotent replay (no save), unknown contribution, cross-user |
| `DashboardServiceImplTest` | 5 | Per-currency grouping (never summed), trends with gaps and across years, category ordering and %, zero total |
| `BudgetIT` | 8 | **Month boundaries** (Oct 31 in, Nov 1 and Sep 30 out), **leap day** (2028-02-29 in, 03-01 out), transfers and other currencies excluded, per-month-and-currency uniqueness, invalid plans (400/422), update keeps rows, delete keeps transactions, cross-user |
| `SavingsGoalIT` | 6 | Progress to ACHIEVED, same key counted once (DB row count), reopen by removal and by raising the target, validation (past date, future contribution, precision, bad key), cross-user, cascade delete |
| `DashboardIT` | 5 | Two currencies, credit card payment transfer excluded, budget snapshot with an exceeded category, gaps as zero, isolation, invalid parameters |

## Frontend Tests

| File | Covers |
|------|--------|
| `utils/money.test.ts` | Locale formatting, minor units, exact cent sums (0.1 + 0.2), amount validation per currency |
| `utils/dates.test.ts` | Leap years, year rollover, month ranges, calendar grid layout |
| `utils/chartData.test.ts` | Trend rows; donut "Other" merging with exact sums |
| `utils/transactions.test.ts` | Day totals exclude transfers and other currencies; sign convention |
| `services/tokenStorage.test.ts` | Expired and corrupted sessions are discarded |
| `pages/LoginPage.test.tsx` | Client validation, server 401 message, successful login stores the session and redirects |
| `components/layout/ProtectedRoute.test.tsx` | Redirect to login with the return path; guest-only routes |
| `pages/RegisterPage.test.tsx` | Password policy mirrors the server |
| `pages/DashboardPage.test.tsx` | Preferred-currency totals, other currencies listed separately, budget status, legend order, month navigation |
| `pages/CalendarPage.test.tsx` | Day totals in the accessible label, day detail, month range request |
| `components/forms/TransactionForm.test.tsx` | Category options by type and archived state, currency precision (JPY), exact payload, server 422 shown |
| `components/forms/BudgetForm.test.tsx` | Preview matches server rounding (HALF_EVEN), over-allocation blocked, request payload |
| `components/forms/AccountForm.test.tsx` | Negative opening balance rule per type; type and currency locked on edit |
| `components/forms/GoalForm.test.tsx` | A retry reuses the same idempotency key |

## End-to-End Tests (Playwright)

| Spec | Flow |
|------|------|
| `sprint2-journey.spec.ts` | Register → empty dashboard → 3 accounts (savings negative refused) → income, expense, transfer → search → derived balances (3,415.63) → budget with preview and a category limit (17 %, on track) → goal with contribution (25 %) → dashboard totals (transfer not counted) → calendar day → logout and redirect |
| `sprint2-journey.spec.ts` | A second user opening the first user's `/accounts/{id}` sees "not found" |
| `mobile.spec.ts` (Pixel 7) | Menu navigation; no horizontal overflow at phone width |

Screenshots from the run were reviewed for layout and visual quality. This review found that charts were captured mid-animation, which led to reduced-motion support.

## Edge Cases Covered

Month transitions (`BudgetIT`), leap years (`BudgetIT`, `dates.test.ts`), zero income (`MonthlyBudgetTest`), rounding (`SavingsStrategyTest`, `BudgetForm.test.tsx`, `money.test.ts`), duplicate requests (`SavingsGoalIT`, `GoalForm.test.tsx`), invalid currencies (`DashboardIT`), unauthorized access (every `*IT`, Postman, E2E), and multi-currency totals (`DashboardIT`, `DashboardPage.test.tsx`).

## Known Gaps

- There is still no test that forces two concurrent writers to collide on `@Version`. Carried to Sprint 3.
- The frontend bundle is about 800 kB (Recharts). Route-level code splitting is planned for Sprint 3 polish.
