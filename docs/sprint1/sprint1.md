# Sprint 1 Planning — Nest

## Sprint Goal

Deliver a secure, tested Spring Boot backend. A user can register, log in, and manage financial accounts, categories, transactions, and transfers. All data is persisted in SQL Server through Flyway migrations and exercised through Swagger and Postman.

Sprint 1 is backend-only. The React UI starts in Sprint 2.

---

## Selected Backlog Items

| PB ID | User Story | Priority | Status |
|-------|------------|----------|--------|
| 1.1 | Register with email and password | High | Done |
| 1.2 | Log in and stay authenticated | High | Done |
| 1.3 | View and edit profile / preferred currency | Medium | Done |
| 2.1 | Create checking, savings, cash, credit card accounts | High | Done |
| 2.2 | See each account's current balance | High | In Progress (balance shows opening balance until transactions land in increment 4) |
| 2.3 | Rename account / correct opening balance | Medium | Done |
| 2.4 | Close an account without losing history | Medium | Done |
| 3.1 | Default income and expense categories | High | Done (in review) |
| 3.2 | Create own categories with colour | Medium | Done (in review) |
| 3.3 | Archive categories safely | Medium | Done (in review) |
| 4.1 | Record income and expenses | High | Not Started |
| 4.2 | Search and filter transactions with pagination | High | Not Started |
| 4.3 | Edit or delete a transaction | High | Not Started |
| 4.4 | Transfer between own accounts | High | Not Started |

Acceptance criteria for each item are in [PB.md](../sprint0/PB.md).

---

## Implementation Steps

The sprint is built in independently reviewable increments. Each increment has its own feature branch and pull request into `develop`.

| # | Increment | Branch | Contents | Status |
|---|-----------|--------|----------|--------|
| 0 | Planning | `docs/sprint0-planning` | Sprint 0 documents, this plan | Done (PR #1) |
| 1 | Backend foundation + authentication | `feature/authentication` | Maven project, config profiles, Docker Compose for SQL Server, Flyway V1 (`users`), error handling, JWT security, `AuthService`, `UserService`, `/api/auth/*`, `/api/users/me`, tests, Postman (Auth + Users) | Done (PR #2) |
| 2 | Account management | `feature/account-management` | `FinancialAccount`, `Money`, balance calculation, V2 migration, `/api/accounts`, tests, Postman | Done (PR #3) |
| 3 | Categories | `feature/categories` | `Category`, default seed, archive rules, V3 migration, `/api/categories`, tests, Postman | In Review |
| 4 | Transactions | `feature/transactions` | `Transaction`, `TransactionType`, Specifications, V4 migration, `/api/transactions`, tests, Postman | Not Started |
| 5 | Transfers | `feature/transfers` | `Transfer` factory, atomic service, `/api/transfers`, tests, Postman | Not Started |
| 6 | Sprint close | `docs/sprint1-review` | As-built UML, sequence diagrams, CRC, `testing.md`, `SR1.md`, README | Not Started |

## Task Breakdown

### Increment 1: Backend foundation and authentication
- [x] Maven project (Java 21, Spring Boot 3.5.x), Maven wrapper, package structure
- [x] `application.yml` with `dev` and `test` profiles; secrets supplied through environment variables
- [x] `docker-compose.yml` for SQL Server 2022; `.env.example`
- [x] Flyway `V1__create_users.sql`
- [x] `AuditableEntity`, `User`
- [x] `UserRepository`
- [x] Exception hierarchy + `GlobalExceptionHandler` (RFC 7807)
- [x] JWT configuration, `TokenService`, `CurrentUserProvider`, security filter chain, JSON 401/403 handlers
- [x] `AuthService`, `UserService` + implementations
- [x] Request/response DTOs, `UserMapper`, `@ValidCurrency`
- [x] `AuthController`, `UserController`
- [x] OpenAPI configuration with bearer auth
- [x] Unit tests: `User`, `AuthServiceImpl`, `UserServiceImpl`, `JwtTokenService`
- [x] Integration tests: auth and profile flows over HTTP against SQL Server (Testcontainers)
- [x] Postman collection: Authentication, Users folders + environment

---

## Technical Decisions

These are summarised here; [system_design.md](../sprint0/system_design.md) gives the full reasoning.

| Decision | Choice |
|----------|--------|
| Authentication | Stateless JWT (HS256) through Spring OAuth2 Resource Server. `sub` = user id. Lifetime 60 minutes by default. |
| Cross-user access | User-scoped queries; a resource owned by someone else returns 404 |
| Error format | RFC 7807 `ProblemDetail` with `timestamp` and a field `errors` list |
| Money | `BigDecimal` / `DECIMAL(19,4)`; reject excess precision; `HALF_EVEN` for derived values |
| Balance | Derived from the opening balance and transactions; never stored |
| Transfers | `Transfer` + two legs (`TRANSFER_OUT`, `TRANSFER_IN`), created atomically |
| Deletion | Accounts are deactivated, categories archived, transactions hard-deleted |
| Concurrency | `@Version` optimistic locking → 409 |
| Schema | Flyway migrations; Hibernate `ddl-auto=validate` |

---

## Sprint 1 Definition of Done

- [ ] Backend builds successfully (`./mvnw verify`)
- [ ] SQL Server connection works (Docker Compose)
- [ ] Migrations execute successfully on an empty database
- [ ] Authentication works
- [ ] Accounts and transactions persist
- [ ] REST APIs can be exercised in Postman
- [ ] Ownership restrictions work (cross-user tests)
- [ ] Unit and integration tests pass
- [ ] Swagger UI is accessible at `/swagger-ui.html`
- [ ] UML and sprint documentation are complete

---

## Completed Work

Updated as each increment is merged.

| Increment | Summary | Evidence |
|-----------|---------|----------|
| 0 | Sprint 0 planning documents | `docs/sprint0/` |
| 1 | Backend foundation + authentication (PB 1.1–1.3) | See "Increment 1 test evidence" below |
| 2 | Account management (PB 2.1, 2.3, 2.4; 2.2 partially) | See "Increment 2 test evidence" below |
| 3 | Categories (PB 3.1–3.3) | See "Increment 3 test evidence" below |

### Increment 2: Account management
- [x] `Money` value object (scale rules, same-currency arithmetic), `AccountType`, `TransactionType`
- [x] `FinancialAccount` entity: fixed type/currency, opening-balance rule per type, closed accounts immutable, `calculateBalance`
- [x] Flyway `V2__create_financial_accounts.sql` with CHECK constraints and a filtered unique index on open account names
- [x] `FinancialAccountRepository` (owner-scoped finders only)
- [x] `FinancialAccountService` + implementation; currency defaults to the user's preferred currency
- [x] `FinancialAccountController`: `POST/GET/GET {id}/PUT/DELETE /api/accounts`
- [x] Unit tests: `MoneyTest`, `TransactionTypeTest`, `FinancialAccountTest`, `FinancialAccountServiceImplTest`
- [x] Integration test `AccountIT`: lifecycle, 400/409/422 cases, cross-user 404s, DB constraints
- [x] Postman: Accounts folder (including cross-user requests)

**Known interim limitation:** until increment 4 adds transactions, `currentBalance` equals the opening balance. The balance formula (`FinancialAccount.calculateBalance`) is implemented and unit-tested. Increment 4 only supplies the per-type totals from the database.

### Increment 3: Categories
- [x] `CategoryType`; `TransactionType.requiredCategoryType()` (prepares the increment 4 compatibility check)
- [x] `Category` entity: defaults identified by `user == null` (derived, not stored), defaults read-only (403), archive instead of delete, type fixed at creation, `requireUsableFor(TransactionType)`
- [x] Flyway `V3__create_categories.sql`: CHECK constraints, filtered unique indexes, 21 seeded defaults (6 income, 15 expense)
- [x] `CategoryRepository` with visibility-scoped JPQL queries (defaults + own only)
- [x] `CategoryService` + implementation; names unique per type across own and default categories, ignoring case
- [x] `CategoryController`: `GET (filter by type, includeArchived) / GET {id} / POST / PUT / DELETE /api/categories`
- [x] Unit tests: `CategoryTest`, `CategoryServiceImplTest`, `TransactionTypeTest` additions
- [x] Integration test `CategoryIT`: seed data, filters, lifecycle, 403 on defaults, cross-user 404s, uniqueness
- [x] Postman: Categories folder

### Increment 1 test evidence

Run on 2026-10-19 with JDK 21.0.8, Docker Desktop 28.5.1, and SQL Server 2022 (amd64 under emulation on Apple Silicon).

| Check | Command | Result |
|-------|---------|--------|
| Unit tests | `./mvnw test` | 50 run, 0 failures (`UserTest`, `CurrenciesTest`, `AuthServiceImplTest`, `UserServiceImplTest`, `JwtTokenServiceTest`) |
| Integration tests | `./mvnw verify` | 16 run, 0 failures (`AuthIT`, `UserProfileIT`) against a Testcontainers SQL Server; Flyway V1 applied; Hibernate `validate` passed |
| Dev stack | `docker compose up -d` + `./mvnw spring-boot:run` | `nest` database created; V1 applied; app started on :8080 |
| Swagger | `GET /swagger-ui.html` | 302 → `/swagger-ui/index.html` 200 |
| Postman | `npx newman run postman/...` | 11 requests, 31 assertions, 0 failures |

### Increment 2 test evidence

Run on 2026-10-19 with the same environment as increment 1.

| Check | Command | Result |
|-------|---------|--------|
| Unit tests | `./mvnw test` | 94 run, 0 failures |
| Integration tests | `./mvnw verify` | 26 run, 0 failures (`AuthIT` 8, `UserProfileIT` 8, `AccountIT` 10); V1 and V2 applied to an empty database |
| Upgrade path | `./mvnw spring-boot:run` on the existing dev database | V2 applied on top of V1 |
| Postman | `npx newman run postman/...` | 30 requests, 69 assertions, 0 failures |

### Increment 3 test evidence

Run on 2026-10-19 with the same environment.

| Check | Command | Result |
|-------|---------|--------|
| Unit tests | `./mvnw test` | 118 run, 0 failures |
| Integration tests | `./mvnw verify` | 34 run, 0 failures (`CategoryIT` 8 new); V1–V3 applied to an empty database |
| Upgrade path | `./mvnw spring-boot:run` on the existing dev database | V3 applied on top of V2 |
| Postman | `npx newman run postman/...` | 44 requests, 97 assertions, 0 failures |
