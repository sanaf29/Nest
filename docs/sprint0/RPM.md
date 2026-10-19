# Release Planning (RPM.md)

**Project:** Nest — Personal Finance and Budgeting
**Developer:** Sanae Faghfouri

---

## Release Goal

Deliver Nest, a secure web application for managing personal finances. Users track accounts and transactions, plan monthly budgets with automatic savings allocation, work toward savings goals, and receive explainable insights and reminders. The application is backed by a tested Spring Boot API and a SQL Server database.

## System Scope (Epics)

| Epic | Description | Sprint |
|------|-------------|--------|
| 1. Authentication and Profile | Register, log in, JWT-secured API, profile and preferred currency | 1 |
| 2. Financial Accounts | Checking, savings, cash, and credit card accounts with derived balances | 1 |
| 3. Categories | Default and user-defined income/expense categories, archiving | 1 |
| 4. Transactions and Transfers | Income/expense records, filtering, and atomic transfers | 1 |
| 5. Monthly Budgets | Monthly plans, category allocations, savings strategy | 2 |
| 6. Savings Goals | Goals, contributions, progress | 2 |
| 7. Dashboard and Calendar | Aggregated summaries, charts, transaction calendar | 2 |
| 8. Web Frontend | React + TypeScript UI for everything above | 2 |
| 9. Financial Insights | Rule-based, explainable insights engine | 3 |
| 10. Recurring Transactions | Scheduled, idempotent generation of recurring entries | 3 |
| 11. Reminders | In-app reminders and notification centre, including parking | 3 |
| 12. Expense Splitting | Shared expenses, equal/custom splits, settlements | 3 |
| 13. Reporting and Export | Monthly reports, CSV export | 3 |
| 14. Security and Release | Hardening, end-to-end test plan, release notes | 3 |

---

## Sprint 0: Planning and System Design

**Goal:** Define what Nest will do and how it will be built before any code is written.

**Deliverables**
- Requirements specification (`requirements.md`)
- Product backlog with epics, stories, acceptance criteria, and priorities (`PB.md`)
- Development process and Definition of Done (`process.md`)
- Release plan (this document)
- System design, including architecture, OOP/SOLID approach, security, and risks (`system_design.md`)
- CRC cards, UML class diagram, and ERD for the Sprint 1 domain

---

## Sprint 1: Backend Foundation and Core Financial Operations

**Goal:** A working, secure Spring Boot API in which users can register, log in, and manage their accounts, categories, transactions, and transfers. Data is persisted in SQL Server through Flyway migrations.

**Backlog items:** Epics 1–4 (PB 1.1–4.4)

**Deliverables**
- Spring Boot 3 / Java 21 / Maven project with `dev` and `test` configuration
- SQL Server via Docker Compose; Flyway migrations
- JWT authentication with BCrypt password hashing
- REST endpoints for auth, users, accounts, categories, transactions, and transfers
- Global RFC 7807 error handling; OpenAPI/Swagger UI
- JUnit 5 + Mockito unit tests; Testcontainers integration tests
- Postman collection and environment
- Sprint docs: `sprint1.md`, `SR1.md`, UML class and sequence diagrams, CRC cards, `testing.md`

**Not included:** frontend UI and budgets.

---

## Sprint 2: Budgeting, Savings Goals, and React Frontend

**Goal:** Turn the transaction tracker into a budgeting application with a complete web interface.

**Backlog items:** Epics 5–8

**Deliverables**
- `MonthlyBudget`, `BudgetAllocation`, and the savings allocation strategy (percentage / fixed)
- `SavingsGoal` and contributions
- Dashboard APIs (balances by currency, cash flow, category spending, trends)
- React + TypeScript + Vite + Tailwind frontend with protected routes, dashboard charts (Recharts), accounts, transactions, transfers, budget planner, goals, and calendar
- Unit, integration, and React Testing Library tests; Postman additions
- Sprint docs: `sprint2.md`, `SR2.md`, updated diagrams, `testing.md`

---

## Sprint 3: Insights, Automation, Collaboration, and Release

**Goal:** Complete Nest with advanced features, harden security, and prepare a stable, documented release.

**Backlog items:** Epics 9–14

**Deliverables**
- Insight engine (`FinancialInsightRule` implementations)
- Recurring transactions with idempotent scheduled generation
- Reminder system and in-app notification centre (including parking reminders)
- Expense splitting with exact rounding and settlement tracking
- Monthly reports and CSV export
- Frontend pages for all of the above; accessibility and empty-state polish
- Security review, end-to-end test plan, release notes
- Sprint docs: `sprint3.md`, `SR3.md`, final diagrams, `testing.md`, `release_notes.md`

---

## Release Criteria

Nest is ready to release when every sprint's Definition of Done is met and these conditions hold:
- All automated tests pass, and the Postman collection runs green against a fresh database.
- Flyway migrations rebuild the schema from empty.
- A new developer can run the full application by following the README.
- The documentation describes the implementation as built.
