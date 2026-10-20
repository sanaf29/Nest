# Nest — Personal Finance and Budgeting

Nest is a full-stack personal finance application. It helps you organise your accounts, understand your spending, plan a monthly budget, and work toward savings goals.

Nest is built as a layered, object-oriented Spring Boot API backed by SQL Server, with a React + TypeScript frontend. It is developed over three Agile sprints with documented planning, design, testing, and reviews.

> Nest records money manually. It does not connect to banks, process payments, or move real money.

---

## Status

| Sprint | Scope | Status |
|--------|-------|--------|
| 0 | Planning and system design | Done |
| 1 | Backend foundation: auth, accounts, categories, transactions, transfers | Done |
| 2 | Budgets, savings goals, dashboard, React frontend | Done |
| 3 | Insights, recurring transactions, reminders, expense splitting, reports | Done |

The release plan is in [`docs/sprint0/RPM.md`](docs/sprint0/RPM.md); sprint documents are in [`docs/`](docs/).

---

## Features

- **Secure accounts:** registration, login with JWT bearer tokens, BCrypt password hashing, profile and preferred currency.
- **Financial accounts:** checking, savings, cash, and credit card accounts. Balances are always derived from the opening balance plus recorded activity; they are never edited directly. Closing an account keeps its history.
- **Categories:** 21 built-in income and expense categories plus your own, with colours. Deleting a category archives it, so past transactions keep it.
- **Transactions:** income and expenses with search by account, category, type, date range, amount range, and text, with pagination.
- **Transfers:** move money between your own accounts. Recorded atomically as two linked entries and never counted as income or spending.
- **Monthly budgets:** planned income, savings as a percentage or fixed amount, category limits, and a live plan-vs-actual view with on-track / near-limit / over-limit status.
- **Savings goals:** targets with deadlines, contributions (safe to retry), automatic "achieved" status.
- **Dashboard:** balances and cash flow per currency, spending by category, 6-month trends, budget and goal progress.
- **Web app:** responsive React UI for everything above, including a monthly transaction calendar.
- **Insights:** explainable, rule-based observations (over budget, spending trend, goal behind schedule, price increases). Not financial advice.
- **Recurring transactions:** rent, pay, and subscriptions recorded automatically on schedule, exactly once, with month-end and leap-day handling.
- **Reminders and notifications:** bills, renewals, parking (location + expiry), goal deadlines, and budget alerts in an in-app notification centre.
- **Split expenses:** equal, by amount, or by percentage, with exact cents, who-owes-whom balances, and settlements (tracking only).
- **Reports and export:** monthly report with comparisons, printable view, and CSV exports protected against spreadsheet formula injection.
- **Security:** password change, login throttling, strict security headers.
- **Privacy by design:** every query is scoped to the authenticated user. Another user's data is indistinguishable from data that doesn't exist (404).

---

## Architecture

```text
React + TypeScript SPA (Sprint 2)
        │  HTTPS + JSON, Authorization: Bearer <JWT>
        ▼
Spring Security filter chain (JWT validation, CORS)
        ▼
REST controllers ── validate DTOs, resolve the current user, delegate
        ▼
Service interfaces ── business rules, ownership, @Transactional boundaries
        ▼
Domain model ── entities and value objects that enforce their own invariants
        ▼
Spring Data JPA repositories / Hibernate
        ▼
SQL Server ── schema versioned by Flyway
```

Key design decisions (full reasoning in [`docs/sprint0/system_design.md`](docs/sprint0/system_design.md)):

- **Money:** `BigDecimal` / `DECIMAL(19,4)`, a `Money` value object, and no silent rounding of input.
- **Derived data:** balances, a transaction's currency, and "is default category" are computed, not stored (3NF).
- **Errors:** RFC 7807 problem details. 400 malformed input, 401 unauthenticated, 403 forbidden action, 404 not found or not yours, 409 conflict, 422 business rule.
- **Schema:** Flyway migrations only; Hibernate validates and never generates the schema.

### Technology

| Layer | Stack |
|-------|-------|
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA / Hibernate 6, Spring Security (OAuth2 resource server, JWT), Bean Validation, springdoc-openapi |
| Database | SQL Server 2022, Flyway |
| Testing | JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc, Testcontainers, Postman / Newman |
| Frontend | React 19, TypeScript (strict), Vite, React Router, Axios, Tailwind CSS, Recharts |
| Frontend testing | Vitest, React Testing Library, Playwright (end-to-end) |

### Repository layout

```text
backend/            Spring Boot API (com.nest: config, controller, dto, exception, mapper,
                    model, repository, security, service, validation)
  src/main/resources/db/migration/   Flyway migrations V1..Vn
postman/            Postman collection + local environment
frontend/           React + TypeScript SPA (src: components, pages, services, hooks, types,
                    context, utils) and Playwright end-to-end tests (e2e/)
docs/sprint0..3/    Backlog, process, design, UML, sprint plans, reviews
docker-compose.yml  Local SQL Server
```

---

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Docker Desktop (on Apple Silicon, enable *Use Rosetta for x86/amd64 emulation*; the SQL Server image is amd64-only)
- Optional: Postman or Node.js (to run the Postman collection with `npx newman`)

### 1. Configure secrets

```bash
cp .env.example .env
# Edit .env: set a strong NEST_DB_PASSWORD and a JWT key:
openssl rand -base64 32   # paste the output as NEST_JWT_SECRET
```

`.env` is git-ignored. Docker Compose and the backend's `dev` profile both read it.

### 2. Start SQL Server

```bash
docker compose up -d          # starts SQL Server 2022 and creates the `nest` database
docker compose ps             # wait until nest-sqlserver is "healthy"
```

### 3. Run the backend

```bash
cd backend
./mvnw spring-boot:run        # Flyway creates or updates the schema on startup
```

Background jobs (recurring transactions hourly, reminders every minute) start with the backend; set `nest.scheduling.enabled=false` to turn them off.

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html (log in, then click **Authorize** and paste the `accessToken`)

### 4. Run the tests

```bash
cd backend
./mvnw test      # unit tests (no Docker needed)
./mvnw verify    # unit + integration tests (Testcontainers starts a throwaway SQL Server)
```

`verify` starts a second SQL Server in Docker. On Apple Silicon each emulated instance needs about 2 GB of memory, so on a machine with limited Docker memory, run `docker compose stop` first and `docker compose start` afterwards.

### 5. Exercise the API with Postman

Import `postman/Nest.postman_collection.json` and `postman/Nest.local.postman_environment.json`, select the **Nest Local** environment, and run the collection. From the command line:

```bash
npx newman run postman/Nest.postman_collection.json -e postman/Nest.local.postman_environment.json
```

If port 8080 is taken by another application, start the backend with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and add `--env-var baseUrl=http://localhost:8081` to the Newman command, or change `baseUrl` in the Postman environment.

### 6. Run the web app

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173 (proxies /api to the backend on :8080)
```

If the backend runs on another port, create `frontend/.env.local` with `VITE_API_PROXY_TARGET=http://localhost:8081`.

```bash
npm test             # unit and component tests (Vitest + React Testing Library)
npm run build        # type-check and production build
npm run e2e          # end-to-end tests in Chromium against the running backend
                     # (first time: npx playwright install chromium;
                     #  backend on another port: E2E_API_URL=http://localhost:8081 npm run e2e)
```

To try the app with realistic data, seed a demo user with three months of CAD activity (accounts, budget, goals, recurring bills, reminders):

```bash
NEST_API_URL=http://localhost:8080 npm run seed:demo   # then log in as demo@nest.local / NestDemo2026
```

The interface is mobile-first: a dark midnight theme by default (light and system themes in Profile → Appearance), a bottom tab bar with a raised + button on phones, and a sidebar on wide screens.

---

## API Overview

All endpoints except register and login require `Authorization: Bearer <token>`. Full, interactive documentation is in Swagger UI.

| Resource | Endpoints |
|----------|-----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Profile | `GET /api/users/me`, `PUT /api/users/me` |
| Accounts | `POST /api/accounts`, `GET /api/accounts[?includeInactive]`, `GET/PUT/DELETE /api/accounts/{id}` |
| Categories | `POST /api/categories`, `GET /api/categories[?type&includeArchived]`, `GET/PUT/DELETE /api/categories/{id}` |
| Transactions | `POST /api/transactions`, `GET /api/transactions[?accountId&categoryId&type&from&to&minAmount&maxAmount&q&page&size&sort]`, `GET/PUT/DELETE /api/transactions/{id}` |
| Transfers | `POST /api/transfers`, `GET/DELETE /api/transfers/{id}` |
| Budgets | `POST/GET /api/budgets`, `GET /api/budgets/{year}/{month}[?currency]`, `PUT/DELETE /api/budgets/{id}`, `GET /api/budgets/{id}/summary` |
| Savings goals | `POST/GET /api/savings-goals[?status]`, `GET/PUT/DELETE /api/savings-goals/{id}`, `POST /api/savings-goals/{id}/contributions`, `DELETE /api/savings-goals/{id}/contributions/{contributionId}` |
| Dashboard | `GET /api/dashboard/summary`, `GET /api/dashboard/spending-by-category`, `GET /api/dashboard/monthly-trends` |
| Insights | `GET /api/insights[?year&month&currency]` |
| Recurring | `POST/GET /api/recurring-transactions`, `GET/PUT/DELETE /api/recurring-transactions/{id}`, `POST …/{id}/pause`, `POST …/{id}/resume`, `POST /api/recurring-transactions/run` |
| Reminders | `POST/GET /api/reminders`, `GET/PUT/DELETE /api/reminders/{id}`, `POST /api/reminders/{id}/dismiss` |
| Notifications | `GET /api/notifications[?unreadOnly]`, `GET /api/notifications/unread-count`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` |
| Shared expenses | `POST/GET /api/shared-expenses`, `GET /api/shared-expenses/balances`, `GET/DELETE /api/shared-expenses/{id}`, `POST …/{id}/participants/{participantId}/settle` and `/unsettle` |
| Reports | `GET /api/reports/monthly`, `GET /api/reports/monthly.csv`, `GET /api/reports/transactions.csv` |
| Profile | `PUT /api/users/me/password` |

---

## Development Process

- Agile sprints with a product backlog, sprint plans, and reviews ([`docs/sprint0/PB.md`](docs/sprint0/PB.md), [`docs/sprint1/`](docs/sprint1/)).
- Trunk-based Git workflow with short-lived feature branches and Conventional Commits ([`docs/sprint0/process.md`](docs/sprint0/process.md)).
- Every increment is verified with unit, integration (real SQL Server), and Postman tests before reaching `main`.

---

## Author

Sana Faghfouri
Computer Science Student, York University
