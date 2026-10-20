# Sprint 2 Review and Retrospective (SR2.md)

**Sprint goal:** budgets with savings allocation, savings goals, dashboard analytics, and a complete web UI.
**Result:** goal met. All 13 selected backlog items are done.

---

## Sprint Results

| Increment | Backlog | Outcome |
|-----------|---------|---------|
| 1 Budgets | PB 5.1–5.4 | Monthly budgets, savings Strategy pattern, category limits, plan-vs-actual summary (V6) |
| 2 Savings goals | PB 6.1–6.3 | Goals with derived progress, automatic ACHIEVED, idempotent contributions (V7) |
| 3 Dashboard APIs | PB 7.1–7.2 (data) | Balances and cash flow per currency, category spending, monthly trends |
| 4 Frontend foundation | PB 8.1 | React app, design system, auth, routing, dashboard |
| 5 Frontend features | PB 7.1–7.3, 8.2, 8.3 | Accounts, transactions, transfers, categories, budget planner, goals, calendar |
| 6 Sprint close | — | Diagrams, testing, this review |

## Demonstrated Functionality

The `sprint2-journey` end-to-end test runs this same script against the real stack:

1. Register in the browser and land on an empty dashboard that points to the Accounts page.
2. Add chequing, savings, and a credit card. A negative savings balance is refused.
3. Record pay and groceries, then transfer 500 to savings. The account list shows derived balances.
4. Create the month's budget: 3,000 income at 20 % savings previews 600 saved and 2,400 spendable. Add a groceries limit, which then shows "On track".
5. Create a goal, contribute 250, and watch the bar reach 25 %. Retrying the same contribution does not double-count it.
6. On the dashboard, income 3,000 and expenses 84.37 match the budget. The transfer is not counted as spending.
7. In the calendar, click today to see the day's transactions.
8. On a phone, the menu opens with every section and nothing scrolls sideways.

## Testing Evidence

See [testing.md](testing.md). Totals:
- **Backend:** 219 unit and 81 integration tests.
- **Postman:** 111 requests, 224 assertions.
- **Frontend:** 40 unit and component tests, plus 3 end-to-end tests.

All pass. V6 and V7 apply cleanly to an empty database and as upgrades to the dev database.

## Decisions Made During the Sprint

- **Refunds count as income**, in the default *Refunds* category. The original category of a refunded purchase is unknown, so they are not netted against an expense category.
- **Budget actuals include closed accounts** in the budget's currency, because history counts. The dashboard's net worth uses open accounts only.
- **Plan and allocations are revised in one domain call**, after a real bug was spotted in review. Clearing and re-adding allocations would make Hibernate insert before deleting, which violates the unique key.
- **React Router 7 and TypeScript 5.9** were chosen over the newer majors (8 and 7.0) for proven APIs and tooling compatibility.
- **The JWT is stored in `localStorage`** to survive refreshes, with the documented XSS trade-off. It will be reviewed in Sprint 3.

## Challenges

| Challenge | Resolution |
|-----------|------------|
| Hibernate flush order (inserts before orphan deletes) would collide with `UQ(budget_id, category_id)` on update | Single `revise(...)` that updates kept rows in place; covered by `BudgetIT.updateReplacesPlan…` |
| `CHECK` with a boolean comparison is not valid T-SQL (`(status = 'ACHIEVED') = …`) | Rewritten as explicit OR branches before the first run |
| A Postman dashboard assertion expected October spending that an earlier folder deletes | The assertion was wrong; fixed and the whole collection rerun |
| Mockito strict stubs flagged a shared stub unused by a pure test | `lenient()` on that stub only |
| End-to-end selectors were ambiguous (nav "Savings goals" vs account "Savings", "Category 1" vs "Remove category 1", chart legend vs stat card) | Exact or scoped selectors; stat cards became named groups (an accessibility gain) |
| Charts were captured mid-animation | Charts respect `prefers-reduced-motion`; E2E runs with reduced motion |

## Incomplete Items and Limitations

All Sprint 2 backlog items are done. Limitations:
- No concurrency (`@Version`) collision test yet.
- The 800 kB frontend bundle is not code-split.
- The dashboard defaults to the UTC month when no month is passed. The web app always passes the user's local month.
- Budgets don't roll unspent amounts over between months, and there is no "copy last month's budget" (a possible Sprint 3 nicety).

## Retrospective

**Continue:** pure domain calculations with I/O at the edges, testing on real SQL Server, end-to-end runs with a visual screenshot review, recording real numbers.

**Start:** route-level code splitting; a concurrency test; seeding a demo dataset for presentations.

**Stop:** writing end-to-end selectors without checking for duplicate accessible names. Use exact names or scope to a region.

## Plan for Sprint 3

Insights engine, recurring transactions, reminders and notifications (including parking), expense splitting, reports and CSV export, frontend pages for all of them, a security review (JWT storage, headers, password change), a concurrency test, an end-to-end test plan, and release notes.
