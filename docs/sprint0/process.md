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

Nest uses a simplified Git Flow:

```text
main         ← released, demo-ready code only (merged from develop at the end of a sprint)
develop      ← integration branch; always builds and passes tests
feature/*    ← one branch per backlog feature, e.g. feature/authentication
docs/*       ← documentation-only changes, e.g. docs/sprint0-planning
fix/*        ← bug fixes found after a feature has merged
```

- Branch from `develop`. Merge back into `develop` with a pull request.
- Create branches when work starts, not in advance.
- `develop` → `main` at the end of each sprint, after the sprint review, tagged `sprint-N`.
- Never commit directly to `main`.

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

## Pull Request Process

1. Before opening a PR, run `./mvnw verify` (backend) and `npm test` (frontend, from Sprint 2) locally. Both must pass.
2. Write the PR description with:
   - linked backlog item IDs (e.g. "Implements PB 1.1, 1.2")
   - a summary of the design decisions
   - how to test it (commands and Postman requests)
   - screenshots for UI changes
3. Keep each PR small enough to review in one sitting. Split large features into model/persistence and API PRs if needed.
4. Squash-merge into `develop` with a Conventional Commit title.

## Code Review Checklist

The reviewer checks:

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
8. **Reviewed and merged:** the PR is approved and merged into `develop`, and `develop` builds green.
