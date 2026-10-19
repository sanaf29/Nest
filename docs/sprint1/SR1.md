# Sprint 1 Review and Retrospective (SR1.md)

**Project:** Nest
**Sprint goal:** a secure, tested Spring Boot backend for registration, login, accounts, categories, transactions, and transfers on SQL Server.
**Result:** goal met. All 14 selected backlog items are done.

---

## Sprint Results

| Increment | Backlog | Outcome |
|-----------|---------|---------|
| 0 Planning | — | Sprint 0 documents (PR #1) |
| 1 Foundation + auth | PB 1.1–1.3 | JWT auth, BCrypt, RFC 7807 errors, Flyway V1 (PR #2) |
| 2 Accounts | PB 2.1, 2.3, 2.4 | Accounts with derived balances, `Money`, Flyway V2 (PR #3) |
| 3 Categories | PB 3.1–3.3 | 21 defaults + user categories, archive, Flyway V3 (PR #4) |
| 4 Transactions | PB 2.2, 4.1–4.3 | Income/expense, live balances, Specification search, Flyway V4 |
| 5 Transfers | PB 4.4 | Atomic two-entry transfers, entry protection, Flyway V5 |

## Demonstrated Functionality

Shown through Swagger UI and the Postman collection against the dev database:

1. Register, see a duplicate-email 409, log in, see a wrong-password 401 with the same message as an unknown email.
2. Use the token to read and update the profile; calls without a token get a 401 in RFC 7807 format.
3. Open chequing, savings, and credit card accounts. A negative savings opening balance is refused (422), and `10.555 CAD` is refused (422).
4. Record income and expenses and see `currentBalance` follow them. Search by date range, text, and amount with paging.
5. Transfer 500 from chequing to savings: both balances move, and income and expense totals are unchanged. Editing one side through `/api/transactions` is refused (409).
6. Close an account: it is hidden but kept, its history is frozen, and its name can be reused.
7. Log in as a second user and try to read, edit, delete, or spend from the first user's resources: every attempt gets a 404.

## Testing Evidence

See [testing.md](testing.md). Final numbers: **162 unit tests, 62 integration tests (real SQL Server), and 84 Postman requests with 172 assertions, all passing.** Migrations V1–V5 apply cleanly to an empty database and as upgrades.

## Technical Decisions Made During the Sprint

- **`TRANSFER` became `TRANSFER_IN`/`TRANSFER_OUT`:** each ledger entry is self-describing, so balances need no join.
- **Derived values are not stored:** the balance, a transaction's currency, and `isDefault` are computed (3NF).
- **Cross-user access returns 404, not 403:** resource IDs cannot be probed.
- **422 for business rules, 400 for malformed input, 409 for state conflicts:** clients can tell "fix your request" from "this isn't allowed now".
- **Transaction schema widened in V5 instead of being anticipated in V4:** each migration describes only what that increment needed.

## Challenges

| Challenge | Resolution |
|-----------|------------|
| SQL Server's Docker image is amd64-only, and the development Mac is Apple Silicon | Runs under Rosetta emulation; documented in the README |
| Two emulated SQL Server instances (dev + Testcontainers) exceeded Docker's memory; the dev container was killed (exit 137) | Restarted with data intact; README advises stopping the dev DB before `verify` |
| Hibernate auto-flush could write an entity mutated before a uniqueness query (category rename) | Checks now run before mutation; covered by a unit test |
| MockMvc URL templates double-encoded `%`, making a wildcard-escaping test fail falsely | Test switched to `.param()`; the code was correct |
| Another local application held port 8080 during Postman runs. Twice a cleanup step stopped a process by port without confirming it was Nest, and the second time it was an unrelated app | Postman now runs against Nest on 8081, and only a confirmed `NestApplication` process is ever stopped. Documented in the README |
| Ambiguous `delete(any())` after Spring Data 3.5 added `delete(Specification)` | Typed matcher in tests |
| The Git Flow `develop` branch duplicated `main` on a solo project | Switched to trunk-based development (documented in `process.md`) |

## Incomplete Items and Known Limitations

All Sprint 1 backlog items are done. Known limitations, deliberately out of scope:

- No refresh tokens or server-side token revocation (60-minute access tokens). To be reviewed in Sprint 3.
- No password-change or email-change endpoint. Planned for Sprint 3 (settings).
- Closed accounts cannot be reopened, and transfers cannot be edited (delete and recreate instead).
- No currency conversion: cross-currency transfers are refused.
- Optimistic-lock conflicts are detected, but there is no test with two concurrent writers yet, and clients cannot send an expected version.

## Retrospective

**Continue**
- Design first (Sprint 0 decisions made implementation straightforward).
- Domain objects enforcing their own invariants, which kept services thin and tests fast.
- Testing against real SQL Server, which caught dialect issues early.
- Recording real test numbers per increment.

**Start**
- Frontend tests from the first UI commit (Sprint 2).
- A concurrency test for optimistic locking.
- Running Postman against a dedicated port by default.

**Stop**
- Stopping processes by port number without confirming what they are.
- Running two SQL Server containers at once on a memory-limited machine.

## Plan for Sprint 2

1. Monthly budgets with category allocations and a savings allocation strategy (percentage / fixed).
2. Savings goals with contributions and progress.
3. Dashboard APIs: balances by currency, income/expense/net cash flow, spending by category, monthly trends.
4. React + TypeScript frontend for every Sprint 1 and Sprint 2 feature, including a transaction calendar.
5. React Testing Library tests; Postman additions for every new endpoint.
