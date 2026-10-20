# Sprint 2 — Updated Class Diagram

These are the classes added in Sprint 2. Sprint 1 classes are unchanged ([as-built Sprint 1 diagram](../sprint1/uml_class_diagram.md)), except for one additive change: `TransactionRepository` gained the analytics queries.

## 1. Budgeting Domain

```mermaid
classDiagram
    direction LR

    class MonthlyBudget {
        -Long id
        -User user
        -int year
        -int month
        -String currency
        -BigDecimal plannedIncome
        -SavingsStrategyType savingsStrategy
        -BigDecimal savingsValue
        -List~BudgetAllocation~ allocations
        -Long version
        +revisePlan(income, strategy, value)
        +revise(income, strategy, value, List~AllocationSpec~)
        +replaceAllocations(List~AllocationSpec~)
        +summarize(MonthlyActuals) BudgetSummary
        +savingsTarget() Money
        +spendable() Money
        +allocatedTotal() Money
        +period() YearMonth
    }

    class BudgetAllocation {
        -Long id
        -MonthlyBudget budget
        -Category category
        -BigDecimal limitAmount
        +limit() Money
    }

    class SavingsAllocationStrategy {
        <<interface>>
        +savingsFor(Money income) Money
    }
    class PercentageSavingsStrategy {
        -BigDecimal percent
        +savingsFor(Money) Money
    }
    class FixedAmountSavingsStrategy {
        -BigDecimal amount
        +savingsFor(Money) Money
    }
    class SavingsStrategyType {
        <<enumeration>>
        PERCENTAGE
        FIXED_AMOUNT
        +create(BigDecimal value) SavingsAllocationStrategy
    }

    class MonthlyActuals {
        <<record>>
        +Money income
        +Map~Long,Money~ expensesByCategory
        +totalExpenses() Money
        +expensesIn(categoryId) Money
        +netCashFlow() Money
    }
    class BudgetSummary {
        <<record>>
        +Money plannedIncome
        +Money savingsTarget
        +Money spendable
        +Money actualIncome
        +Money actualExpenses
        +Money netCashFlow
        +Money remaining
        +BigDecimal percentUsed
        +BigDecimal savingsProgressPercent
        +Money unbudgetedSpending
        +List~CategoryLine~ categories
    }
    class BudgetStatus {
        <<enumeration>>
        ON_TRACK
        NEAR_LIMIT
        EXCEEDED
        +of(percentUsed)$ BudgetStatus
    }

    MonthlyBudget "1" *-- "0..*" BudgetAllocation : allocations
    BudgetAllocation --> Category
    MonthlyBudget --> User : owner
    MonthlyBudget ..> SavingsStrategyType
    SavingsStrategyType ..> SavingsAllocationStrategy : creates
    PercentageSavingsStrategy ..|> SavingsAllocationStrategy
    FixedAmountSavingsStrategy ..|> SavingsAllocationStrategy
    MonthlyBudget ..> MonthlyActuals : summarize()
    MonthlyBudget ..> BudgetSummary : produces
    BudgetSummary ..> BudgetStatus
```

## 2. Savings Goals

```mermaid
classDiagram
    direction LR

    class SavingsGoal {
        -Long id
        -User user
        -String name
        -String currency
        -BigDecimal targetAmount
        -LocalDate targetDate
        -GoalStatus status
        -Instant achievedAt
        -List~GoalContribution~ contributions
        -Long version
        +update(name, target, targetDate, now)
        +contribute(amount, date, note, idempotencyKey, today, now) GoalContribution
        +contributionWithKey(key) Optional~GoalContribution~
        +removeContribution(id, now) boolean
        +savedAmount() Money
        +remainingAmount() Money
        +progressPercent() BigDecimal
    }
    class GoalContribution {
        -Long id
        -SavingsGoal goal
        -BigDecimal amount
        -LocalDate contributionDate
        -String note
        -String idempotencyKey
        +amount() Money
    }
    class GoalStatus {
        <<enumeration>>
        ACTIVE
        ACHIEVED
    }

    SavingsGoal "1" *-- "0..*" GoalContribution : contributions
    SavingsGoal --> User : owner
    SavingsGoal ..> GoalStatus
```

## 3. Services and Controllers

```mermaid
classDiagram
    direction TB

    class BudgetService { <<interface>> }
    class SavingsGoalService { <<interface>> }
    class DashboardService { <<interface>> }
    class LedgerAnalytics {
        +monthlyActuals(userId, YearMonth, currency) MonthlyActuals
        +expensesByCategory(userId, YearMonth, currency) List~CategoryTotal~
    }

    BudgetController --> BudgetService
    SavingsGoalController --> SavingsGoalService
    DashboardController --> DashboardService

    BudgetServiceImpl ..|> BudgetService
    SavingsGoalServiceImpl ..|> SavingsGoalService
    DashboardServiceImpl ..|> DashboardService

    BudgetServiceImpl --> MonthlyBudgetRepository
    BudgetServiceImpl --> CategoryRepository
    BudgetServiceImpl --> LedgerAnalytics
    SavingsGoalServiceImpl --> SavingsGoalRepository
    SavingsGoalServiceImpl --> Clock
    DashboardServiceImpl --> FinancialAccountService : live balances
    DashboardServiceImpl --> LedgerAnalytics
    DashboardServiceImpl --> TransactionRepository : grouped totals
    DashboardServiceImpl --> MonthlyBudgetRepository
    DashboardServiceImpl --> SavingsGoalRepository
    LedgerAnalytics --> TransactionRepository
```

## Design Notes

- **Strategy and factory:** `SavingsAllocationStrategy` is the Strategy pattern. Percentage and fixed amount are interchangeable algorithms behind one interface. `SavingsStrategyType` is a factory: the database stores the type and value, and the enum rebuilds the behaviour. Adding a strategy (for example "percentage above a floor") means adding a class and an enum constant. `MonthlyBudget` doesn't change (open/closed).
- **Pure calculation:** `MonthlyBudget.summarize(MonthlyActuals)` performs no I/O. `LedgerAnalytics` performs the queries and hands over a value object. The arithmetic is unit-tested without a database, and the queries are integration-tested separately.
- **Composition:** allocations and contributions are owned by their aggregate and are saved and deleted through it. Neither is ever modified on its own.
- **`LedgerAnalytics` is a class, not an interface:** it has one implementation and no reason to vary, so an interface would only add indirection. This follows the "no unnecessary abstractions" rule.
