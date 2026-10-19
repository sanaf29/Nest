# Nest — Development Process

## Team and Roles

**Sanae Faghfouri** is the primary developer and owns the backlog, design, implementation, and review. An AI pair-programmer (Claude Code) assists with design proposals, implementation, and review. Every change it produces goes through the same branch → review → merge process as any other contribution.

## Agile Workflow

Nest is delivered in a planning sprint (Sprint 0) followed by three development sprints. See [RPM.md](RPM.md).

Each sprint follows this cycle:

1. **Sprint planning:** select backlog items from [PB.md](PB.md), break them into tasks, and record them in `docs/sprintN/sprintN.md`.
2. **Feature workflow:** every feature goes through the same nine steps:
   1. Requirements: user story and acceptance criteria
   2. OOP design: classes, responsibilities, relationships, and the principles applied
   3. Database design: tables, constraints, and a Flyway migration
   4. Backend implementation, in dependency order: model → repository → service → DTO/mapper → controller
   5. Unit and integration tests
   6. Postman verification (valid and invalid requests)
   7. Frontend integration (from Sprint 2)
   8. Documentation: UML, sprint docs, OpenAPI annotations
   9. Review: what was done, its limitations, and what comes next
3. **Sprint review:** demonstrate working features and present test evidence in `SRN.md`.
4. **Retrospective:** note what to continue, start, and stop. This is recorded in `SRN.md`.

A sprint ends only when its Definition of Done (below and in each sprint plan) is met. Unfinished items return to the backlog and are listed explicitly in `SRN.md`.

## Branching Strategy

Nest is a solo project, so it uses trunk-based development with short-lived branches:

```text
main         ← always builds and passes every test; the only long-lived branch
feature/*    ← one branch per increment, e.g. feature/transactions
docs/*       ← documentation-only changes, e.g. docs/sprint1-review
fix/*        ← bug fixes found after a feature has merged
```

- Create a branch from the latest `main` when work starts, never in advance.
- Integrate by fast-forwarding `main` onto the finished branch, then push (see *Integrating Into main* below).
- At the end of each sprint, after the review, tag `main` as `sprint-N`.
- Delete feature branches once they are merged.

**Change from the original plan.** Sprint 0 planned Git Flow, with a `develop` integration branch and a pull request for every feature. With one developer, `develop` only duplicated `main`, and PR review had no second reviewer. The first four increments went through PRs #1–#4; from increment 4 (transactions) onward, branches are fast-forwarded into `main` directly. The quality gate is unchanged, because the checks below must pass locally before anything reaches `main`.

## Commit Conventions

Nest uses [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <imperative summary, ≤ 72 chars>

<optional body: what and why, not how>
```

| Type | Use for |
|------|---------|
| `feat` | New user-visible behaviour |
| `fix` | Bug fix |
| `test` | Adding or correcting tests only |
| `refactor` | Code change with no behaviour change |
| `docs` | Documentation only |
| `build` | Maven/npm/Docker configuration |
| `chore` | Tooling or housekeeping |

Scopes match the domain: `auth`, `users`, `accounts`, `categories`, `transactions`, `transfers`, `budgets`, and so on.

Example: `feat(transfers): record transfers as linked TRANSFER_OUT/TRANSFER_IN entries`

**Never commit:** `.env` files, passwords, JWT secrets, `target/` or `node_modules/` build output, IDE settings, or real financial data. `.gitignore` enforces the common cases.

## Integrating Into main

1. Rebase the branch on the latest `main` if `main` has moved (`git fetch && git rebase origin/main`).
2. Run every check locally. All must pass:
   - `./mvnw verify` (backend unit + integration tests)
   - the Postman collection via Newman against the dev database
   - `npm test` (frontend, from Sprint 2)
3. Review your own diff against the *Code Review Checklist* below (`git diff main...HEAD`).
4. Record the increment in the sprint plan: status, checklist, and test evidence with the actual numbers.
5. Fast-forward and push:
   ```bash
   git checkout main
   git merge --ff-only feature/<name>
   git push origin main
   ```
   `--ff-only` refuses to create a merge commit, which keeps history linear and guarantees `main` is exactly the commit that was tested.
6. Each commit message carries what a PR description would have: backlog IDs (e.g. "Implements PB 4.1"), the design decisions, and anything a reviewer should know.

A pull request is still worth opening when an outside reviewer (an instructor, a teammate, or an AI reviewer) should look at a change before it lands.

## Code Review Checklist

Before integrating, the author (or a reviewer, when there is one) checks:

- **Correctness:** acceptance criteria are met, edge cases are handled, and money uses `BigDecimal`.
- **Security:** every query on user-owned data is scoped by the authenticated user, there are no secrets in code, and DTOs expose no sensitive fields.
- **Design:** controllers are thin, business rules live in services or domain objects, and there are no needless abstractions.
- **Tests:** new business rules have unit tests, and new endpoints have integration or Postman coverage.
- **Readability:** names are clear, and the reason behind any non-obvious decision is documented.
- **Migrations:** schema changes are a new Flyway file. A migration that has already been applied is never edited.

## Testing Expectations

| Level | Tooling | What it covers |
|-------|---------|----------------|
| Domain unit tests | JUnit 5 | Entity invariants, value objects, calculations (no Spring) |
| Service unit tests | JUnit 5 + Mockito | Business rules, authorization decisions, orchestration |
| Integration tests | Spring Boot Test + Testcontainers (SQL Server) + MockMvc | Migrations, repositories, security filter chain, HTTP contract |
| API tests | Postman / Newman | End-to-end request flows with assertions |
| Frontend tests (Sprint 2+) | Vitest + React Testing Library | Component behaviour, forms, data mapping |

Test results are never reported unless they were actually run. Sprint reviews include the command and its output summary.

## Definition of Done

A backlog item is **Done** when:

1. **Code complete:** implemented on a feature branch following the layered architecture; no TODO placeholders or commented-out code.
2. **Validated:** all external input is validated with DTO constraints, and business rules are enforced in the service or domain layer.
3. **Secure:** ownership checks are in place and covered by at least one cross-user test.
4. **Tested:** unit tests for business rules and integration tests for persistence and HTTP. `./mvnw verify` passes.
5. **API verified:** Postman requests (happy path + failure cases) are added to the collection, and the endpoint appears in Swagger UI.
6. **Database:** the schema change is a new Flyway migration that applies cleanly to an empty SQL Server database.
7. **Documented:** UML/ERD and sprint docs are updated, and any non-obvious decision is explained.
8. **Reviewed and integrated:** the diff passes the review checklist, the branch is fast-forwarded into `main`, and `main` builds green.
