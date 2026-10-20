# Sprint 2 — Sequence Diagrams

## 1. Budget Summary (Plan vs. Actual)

```mermaid
sequenceDiagram
    actor User
    participant UI as BudgetPage (React)
    participant C as BudgetController
    participant S as BudgetServiceImpl
    participant BR as MonthlyBudgetRepository
    participant LA as LedgerAnalytics
    participant TR as TransactionRepository
    participant B as MonthlyBudget

    User->>UI: open October 2026
    UI->>C: GET /api/budgets/2026/10?currency=CAD
    C-->>UI: BudgetResponse (or 404 → "Create budget")
    UI->>C: GET /api/budgets/{id}/summary
    C->>S: summary(userId, id)
    S->>BR: findByIdAndUserId(id, userId)
    S->>LA: monthlyActuals(userId, 2026-10, CAD)
    LA->>TR: sumByType(user, CAD, 2026-10-01..2026-10-31, [INCOME])
    LA->>TR: sumByCategory(user, CAD, same range, EXPENSE)
    Note over LA,TR: transfers and other currencies are excluded by the query
    LA-->>S: MonthlyActuals(income, expensesByCategory)
    S->>B: summarize(actuals)
    Note over B: pure: spendable, remaining, % used,<br/>savings progress, per-category status
    B-->>S: BudgetSummary
    S-->>UI: BudgetSummaryResponse
    UI-->>User: cards, progress bars, ON_TRACK / NEAR_LIMIT / EXCEEDED badges
```

## 2. Revise a Budget (Plan and Allocations Together)

```mermaid
sequenceDiagram
    participant S as BudgetServiceImpl
    participant CR as CategoryRepository
    participant B as MonthlyBudget
    participant ST as SavingsStrategyType
    participant R as MonthlyBudgetRepository

    S->>CR: findVisible(categoryId, userId) for each allocation (404 if not visible)
    S->>B: revise(income, strategy, value, specs)
    B->>ST: create(value).savingsFor(income)
    Note over B: validate EVERYTHING first:<br/>savings ≤ income, EXPENSE categories,<br/>no duplicates, Σ limits ≤ spendable
    alt invalid
        B-->>S: BusinessRuleException (422), budget unchanged
    else valid
        B->>B: apply plan; update kept rows in place; add new; orphan-remove dropped
        S->>R: saveAndFlush(budget)
        Note over R: updating rows in place avoids an insert-before-delete<br/>collision on UQ(budget_id, category_id)
    end
```

## 3. Idempotent Goal Contribution

```mermaid
sequenceDiagram
    actor User
    participant F as ContributionForm
    participant C as SavingsGoalController
    participant S as SavingsGoalServiceImpl
    participant G as SavingsGoal
    participant DB as SQL Server

    Note over F: key = crypto.randomUUID() once per form
    User->>F: Add contribution (250)
    F->>C: POST /api/savings-goals/3/contributions {amount, idempotencyKey}
    C->>S: contribute(userId, 3, request)
    S->>G: contributionWithKey(key)
    alt key already used (retry or double click)
        S-->>C: ContributionResult(goal, created=false)
        C-->>F: 200 OK (nothing recorded twice)
    else new key
        S->>G: contribute(amount, date, note, key, today, now)
        G->>G: refreshStatus(): ACHIEVED when saved ≥ target
        S->>DB: saveAndFlush (UX_goal_contributions_idempotency guards concurrent replays)
        C-->>F: 201 Created
    end
```

## 4. Dashboard Load (Frontend to Backend)

```mermaid
sequenceDiagram
    actor User
    participant P as DashboardPage
    participant H as useAsync
    participant API as apiClient (Axios)
    participant D as DashboardController
    participant S as DashboardServiceImpl
    participant AS as FinancialAccountService

    User->>P: open dashboard / change month
    par three independent requests
        P->>H: summary(year, month)
        H->>API: GET /api/dashboard/summary
        API->>D: Authorization: Bearer (interceptor)
        D->>S: summary(userId, month)
        S->>AS: list(userId) (same balance logic as the Accounts page)
        S-->>P: balances & cash flow per currency, budget snapshot, goals
    and
        P->>H: spendingByCategory(year, month, currency)
    and
        P->>H: monthlyTrends(year, month, 6, currency)
    end
    Note over H: responses from an older month are discarded (latest-request guard)
    P-->>User: stat cards, donut, trends chart, goal bars
```

## 5. Session Handling in the Browser

```mermaid
sequenceDiagram
    actor User
    participant L as LoginPage
    participant A as AuthProvider
    participant T as tokenStorage
    participant API as apiClient

    User->>L: email + password
    L->>A: login()
    A->>API: POST /api/auth/login
    API-->>A: {accessToken, expiresAt, user}
    A->>T: write(session) (localStorage)
    A->>A: setTimeout(logout, expiresAt − now)
    L-->>User: redirect to the originally requested page

    Note over API: later, any request
    API->>API: request interceptor adds Bearer token
    alt server answers 401 (expired, revoked, invalid)
        API->>A: onUnauthorized() → logout()
        A->>T: clear()
        A-->>User: ProtectedRoute redirects to /login
    end
```
