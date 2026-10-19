# Nest — Requirements Specification

This document lists the functional and non-functional requirements for Nest. User stories and acceptance criteria that implement these requirements are in [PB.md](PB.md). Design decisions that satisfy them are in [system_design.md](system_design.md).

Requirement IDs are referenced from backlog items, tests, and sprint documents.

---

## 1. Scope

Nest is a personal finance and budgeting web application. Users record their own financial activity manually. The MVP does **not** connect to banks, process payments, or move real money. Expense splitting and reminders are tracking features only.

---

## 2. Functional Requirements

### FR-1 Identity and Profile

| ID | Requirement |
|----|-------------|
| FR-1.1 | A visitor can register with first name, last name, email, password, and preferred currency. |
| FR-1.2 | Email addresses are unique (case-insensitive) across all users. |
| FR-1.3 | Passwords are stored only as BCrypt hashes. |
| FR-1.4 | A registered user can log in with email and password and receives an access token. |
| FR-1.5 | Invalid login attempts receive a generic error that does not reveal whether the email exists. |
| FR-1.6 | An authenticated user can view and update their own profile (name, preferred currency). |

### FR-2 Financial Accounts

| ID | Requirement |
|----|-------------|
| FR-2.1 | A user can create accounts of type CHECKING, SAVINGS, CASH, or CREDIT_CARD. |
| FR-2.2 | Each account has a name, ISO 4217 currency, and opening balance. The currency cannot change after creation. |
| FR-2.3 | An account's current balance is **derived** from its opening balance and its recorded transactions; it cannot be set directly. |
| FR-2.4 | A user can rename an account or correct its opening balance. |
| FR-2.5 | Deleting an account deactivates it. Its history is preserved, and new transactions cannot be recorded against it. |
| FR-2.6 | Active account names are unique per user. |

### FR-3 Categories

| ID | Requirement |
|----|-------------|
| FR-3.1 | The system provides default INCOME and EXPENSE categories visible to all users and editable by none. |
| FR-3.2 | A user can create, rename, and recolour their own categories. |
| FR-3.3 | Deleting a user category archives it. Past transactions keep their category, and archived categories cannot be used for new transactions. |
| FR-3.4 | Category names are unique per user and type, and may not duplicate a default category name of the same type. |

### FR-4 Transactions and Transfers

| ID | Requirement |
|----|-------------|
| FR-4.1 | A user can record INCOME and EXPENSE transactions against their own active accounts. |
| FR-4.2 | Amounts are strictly positive; direction is expressed by the transaction type. |
| FR-4.3 | Amount precision may not exceed the currency's minor units (e.g. 2 for CAD, 0 for JPY). |
| FR-4.4 | The category type must match the transaction type (EXPENSE ↔ EXPENSE category). |
| FR-4.5 | A user can list their transactions with pagination and filters (account, category, type, date range, amount range, text). |
| FR-4.6 | A user can edit and delete their INCOME and EXPENSE transactions. |
| FR-4.7 | A user can transfer an amount between two of their own active accounts in the same currency. A transfer is recorded atomically as two linked entries. |
| FR-4.8 | Transfers never count as income or spending in any summary. |
| FR-4.9 | Transfer entries cannot be edited or deleted individually. Deleting a transfer removes both entries. |

### FR-5 Budgets and Savings (Sprint 2)

| ID | Requirement |
|----|-------------|
| FR-5.1 | A user can create one budget per calendar month per currency with planned income and a savings strategy. |
| FR-5.2 | Savings can be allocated as a percentage of income or as a fixed amount. |
| FR-5.3 | A user can allocate spending limits per expense category. |
| FR-5.4 | The system computes actual income, actual spending, remaining budget, and percentage used per category. |
| FR-5.5 | A user can create savings goals, record contributions, and see completion percentage. |

### FR-6 Dashboard and Frontend (Sprint 2)

| ID | Requirement |
|----|-------------|
| FR-6.1 | The dashboard shows balances by currency, monthly income/expenses, net cash flow, budget remaining, goal progress, spending by category, and monthly trends. |
| FR-6.2 | Monetary values in different currencies are never summed together. |
| FR-6.3 | All Sprint 1–2 features are usable from a responsive React web UI, including a monthly transaction calendar. |

### FR-7 Advanced Features (Sprint 3)

| ID | Requirement |
|----|-------------|
| FR-7.1 | Rule-based financial insights are generated from the user's own data and explain why they were triggered. |
| FR-7.2 | Recurring transactions generate due entries exactly once, even if the job reruns. |
| FR-7.3 | Users receive in-app reminders (bills, budget thresholds, subscriptions, goal deadlines, parking, custom) without duplicates. |
| FR-7.4 | Users can split a shared expense equally or by custom amounts. Allocated shares always sum exactly to the total. |
| FR-7.5 | Users can export monthly reports and transactions as CSV, protected from formula injection. |

---

## 3. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-1 | Security | Every user-owned resource is accessible only by its owner. Ownership is derived from the authenticated token, never from client-supplied user IDs. |
| NFR-2 | Security | Secrets (DB password, JWT key) are supplied by environment variables and never committed. |
| NFR-3 | Security | Error responses never expose stack traces, SQL, or internal class names. |
| NFR-4 | Correctness | All monetary values use `BigDecimal` in Java and `DECIMAL(19,4)` in SQL Server. `float`/`double` are never used for money. |
| NFR-5 | Correctness | Multi-row financial writes (transfers) are atomic. |
| NFR-6 | Correctness | Concurrent edits to the same record are detected (optimistic locking) rather than silently overwritten. |
| NFR-7 | Maintainability | Layered architecture (controller → service → repository); controllers contain no business rules. |
| NFR-8 | Maintainability | The database schema is versioned with Flyway; Hibernate only validates it. |
| NFR-9 | Testability | Financial calculations are pure functions or domain methods that can be unit-tested without Spring or a database. |
| NFR-10 | Testability | Persistence and security are covered by integration tests against real SQL Server (Testcontainers). |
| NFR-11 | Documentation | All endpoints are documented with OpenAPI/Swagger and exercised by a Postman collection. |
| NFR-12 | Usability | The UI is responsive (mobile and desktop) and accessible (labelled inputs, keyboard navigation). |
