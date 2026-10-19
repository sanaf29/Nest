# Sprint 1 — Testing

## Strategy

| Level | Tools | Purpose | Runs with |
|-------|-------|---------|-----------|
| Domain unit tests | JUnit 5, AssertJ | Entity invariants, `Money`, balance formula, transfer factory. No Spring, no database. | `./mvnw test` |
| Service unit tests | JUnit 5, Mockito | Business rules and ownership decisions in services, with repositories mocked | `./mvnw test` |
| Integration tests (`*IT`) | Spring Boot Test, MockMvc, Testcontainers SQL Server 2022 | Real HTTP contract, security filter chain, Flyway migrations, Hibernate schema validation, SQL constraints | `./mvnw verify` |
| API tests | Postman collection + Newman | End-to-end flows against the running app and dev database, including cross-user requests | `npx newman run …` |

The integration tests run against **real SQL Server**, not H2, so the dialect, filtered unique indexes, `CHECK` constraints, `DATETIMEOFFSET` mapping, and migration SQL are all exercised. Each test registers fresh users with random emails, so tests are independent and need no cleanup.

## How to Run

```bash
cd backend
./mvnw test       # 162 unit tests, ~10 s, no Docker needed
./mvnw verify     # + 62 integration tests (starts a throwaway SQL Server container)

# Postman (backend running against the dev database):
npx newman run ../postman/Nest.postman_collection.json -e ../postman/Nest.local.postman_environment.json
```

On Apple Silicon, stop the dev database (`docker compose stop`) before `verify` if Docker Desktop has limited memory: two emulated SQL Server instances need about 4 GB.

## Inventory

### Unit tests: 162

| Class | Tests | Covers |
|-------|------:|--------|
| `UserTest` | 16 | Email normalisation, name trimming, blank and invalid values, atomic profile update, password verification, `toString` hides the hash |
| `CurrenciesTest` | 19 | Supported, malformed, and pseudo ISO codes; minor units (CAD 2, JPY 0, BHD 3) |
| `MoneyTest` | 16 | Scale normalisation, rejection of excess precision, exact arithmetic (0.10 + 0.20), currency mixing, sign |
| `FinancialAccountTest` | 16 | Opening-balance rule per type, precision, name rules, closed-account immutability, balance formula, credit card convention, DB-scale totals |
| `CategoryTest` | 15 | Colours, defaults read-only (403), archive idempotency, visibility, type compatibility |
| `TransactionTest` | 18 | Positive amounts, currency precision, wrong / archived / foreign category, transfer types rejected, closed account, revision atomicity, frozen history |
| `TransactionTypeTest` | 3 | Balance signs, transfer flags, required category types |
| `TransferTest` | 13 | Balanced legs, net zero, default descriptions, credit card payment, distinct / same-owner / same-currency / open rules, entry protection (409) |
| `JwtTokenServiceTest` | 5 | Claims round trip, expiry, foreign key, foreign issuer, fail-fast secret validation |
| `AuthServiceImplTest` | 6 | Hashing, normalised duplicates, valid login, wrong password, unknown email with timing equaliser, no token before verification |
| `UserServiceImplTest` | 4 | Profile read/update, not found, invalid update not saved |
| `FinancialAccountServiceImplTest` | 11 | Default currency, duplicate names, cross-user 404, list filters, update, deactivate, single aggregate query for balances |
| `CategoryServiceImplTest` | 8 | Create, duplicates vs defaults, 403 on defaults without name query, rename conflict before mutation, cross-user 404 |
| `TransactionServiceImplTest` | 7 | Create, foreign account / invisible category 404, currency claim, delete rules |
| `TransferServiceImplTest` | 5 | Create, foreign account (nothing saved), delete rules, cross-user 404 |

### Integration tests: 62

| Class | Tests | Covers |
|-------|------:|--------|
| `AuthIT` | 8 | Register (hash stored, never returned), case-insensitive duplicate, field-level validation, malformed JSON, login, identical 401s |
| `UserProfileIT` | 8 | Profile read/update, token-to-user binding, missing / malformed / expired token, Swagger public |
| `AccountIT` | 10 | Lifecycle, exact decimal round trip, 400/409/422 cases, close and reuse name, cross-user 404 on every verb, DB constraints |
| `CategoryIT` | 8 | 21 seeded defaults, type filter, lifecycle, uniqueness incl. defaults, defaults read-only, cross-user isolation |
| `TransactionIT` | 19 | Live balances through create/update/delete, all 422 rules, frozen closed accounts, cross-user isolation, every filter, paging, sorting, LIKE-wildcard escaping |
| `TransferIT` | 9 | Balances, no income/spending created, entries protected (409), no partial writes, credit card payment, cross-user isolation, DB shape constraints |

### Postman: 84 requests, 172 assertions

Folders: Authentication, Users, Accounts, Categories, Transactions, Transfers. Every request asserts its status code. Error responses also assert the RFC 7807 shape (`application/problem+json`, `status`, `title`, `timestamp`, and no stack trace). A second user is registered to exercise cross-user access for every resource type.

## Definition of Done Evidence

| Sprint 1 DoD item | Evidence |
|-------------------|----------|
| Backend builds | `./mvnw verify` → BUILD SUCCESS |
| SQL Server connection works | Dev profile against Docker Compose SQL Server; integration tests against Testcontainers SQL Server |
| Migrations execute | V1–V5 applied to an empty database (every `verify`) and as upgrades to the dev database (one per increment) |
| Authentication works | `AuthIT`, `UserProfileIT`, Postman Authentication/Users |
| Accounts and transactions persist | `AccountIT`, `TransactionIT`, `TransferIT` |
| APIs exercisable in Postman | Collection run: 84 requests, 0 failures |
| Ownership restrictions work | A cross-user test for every resource (accounts, categories, transactions, transfers), in both the integration tests and Postman |
| Unit and integration tests pass | 162 + 62, 0 failures |
| Swagger accessible | `UserProfileIT.swaggerDocumentationIsPubliclyAccessible`; `/swagger-ui.html` → 200 |
| UML and documentation complete | `uml_class_diagram.md`, `sequence_diagrams.md`, this file, `SR1.md` |

## Not Covered (Known Gaps)

- **Concurrent edits:** optimistic locking (`@Version`) is configured and mapped to 409, but no test forces two concurrent writers. Planned for the Sprint 3 final test pass.
- **Load and performance:** no tests. The balance query is a single `GROUP BY` per request and the main lookups are indexed, but neither was measured.
- **Frontend:** none in Sprint 1 (backend-only sprint).
