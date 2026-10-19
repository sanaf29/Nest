# Nest — CRC Cards (Sprint 1 Domain)

Class–Responsibility–Collaborator cards for the core classes planned for Sprint 1. Each card lists what the class *knows* and *does*, and which classes it needs to do it. Sprint 1's own CRC update (`docs/sprint1/crc_cards.md`) will reflect the implementation as built.

---

### User (Entity)

| Responsibilities | Collaborators |
|------------------|---------------|
| Knows first name, last name, normalised email, password hash, preferred currency | `AuditableEntity` |
| Validates and normalises its profile data (non-blank names, lower-case email, ISO currency) | |
| Changes its profile through explicit methods (`updateProfile`) | |
| Acts as the owner of accounts and user-defined categories | `FinancialAccount`, `Category` |

### FinancialAccount (Entity, aggregate root)

| Responsibilities | Collaborators |
|------------------|---------------|
| Knows its owner, name, type, currency, opening balance, active flag | `User`, `AccountType` |
| Enforces the opening-balance rule for its type | `AccountType` |
| Calculates its balance from per-type totals | `TransactionType`, `Money` |
| Renames itself, corrects its opening balance, deactivates itself | |
| Answers whether it can accept new transactions (`isActive`) and whether it belongs to a user (`isOwnedBy`) | |

### Category (Entity)

| Responsibilities | Collaborators |
|------------------|---------------|
| Knows its name, type (INCOME/EXPENSE), colour, owner (null for defaults), archived flag | `User`, `CategoryType` |
| Answers `isDefault()` and `isVisibleTo(user)` | |
| Answers whether it is compatible with a transaction type | `TransactionType` |
| Renames, recolours, and archives itself; refuses changes if it is a default category | |

### Transaction (Entity)

| Responsibilities | Collaborators |
|------------------|---------------|
| Knows its account, category, type, amount, description, date, and optional transfer | `FinancialAccount`, `Category`, `TransactionType`, `Transfer` |
| Rejects non-positive amounts and amounts with too many decimals for the account's currency | `Money` |
| Requires a compatible category for INCOME/EXPENSE and forbids one for transfer legs | `Category`, `TransactionType` |
| Refuses to be created on, or moved to, an inactive account | `FinancialAccount` |
| Revises its details while keeping every invariant (`revise(...)`) | |
| Reports its signed effect on the account balance | `TransactionType` |

### Transfer (Entity, aggregate root, factory)

| Responsibilities | Collaborators |
|------------------|---------------|
| Creates a balanced pair of entries between two accounts (`Transfer.between`) | `Transaction`, `FinancialAccount` |
| Ensures source ≠ destination, same owner, same currency, both active, positive amount | `FinancialAccount`, `Money` |
| Exposes its outgoing and incoming legs | `Transaction` |

### Money (Value object)

| Responsibilities | Collaborators |
|------------------|---------------|
| Pairs an amount with a currency; immutable | `java.util.Currency`, `BigDecimal` |
| Adds and subtracts only same-currency values | |
| Validates that the scale fits the currency's minor units; normalises the representation | |

### TransactionType / AccountType / CategoryType (Enums with behaviour)

| Responsibilities | Collaborators |
|------------------|---------------|
| `TransactionType`: knows its balance sign, whether it is a transfer, and which category type it requires | `CategoryType` |
| `AccountType`: knows whether a negative opening balance is allowed | |
| `CategoryType`: names the two category families | |

---

### AuthService / AuthServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Registers a user: normalises the email, rejects duplicates, hashes the password | `UserRepository`, `PasswordEncoder`, `UserMapper` |
| Authenticates credentials with a generic failure and similar timing | `UserRepository`, `PasswordEncoder` |
| Issues an access token for an authenticated user | `TokenService` |

### UserService / UserServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Returns the current user's profile | `UserRepository`, `UserMapper` |
| Updates name and preferred currency | `User` |

### FinancialAccountService / FinancialAccountServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Creates accounts for the current user; enforces unique active names | `FinancialAccountRepository`, `UserRepository` |
| Lists and reads only the user's accounts, each with its computed balance | `TransactionRepository`, `FinancialAccount`, `AccountMapper` |
| Updates the name and opening balance; deactivates on delete | `FinancialAccount` |

### CategoryService / CategoryServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Lists default and own categories, optionally including archived ones | `CategoryRepository`, `CategoryMapper` |
| Creates, updates, and archives own categories; forbids changing defaults | `Category` |
| Enforces name uniqueness, including against default names | `CategoryRepository` |

### TransactionService / TransactionServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Creates, revises, and deletes INCOME/EXPENSE entries for the user's own accounts | `FinancialAccountRepository`, `CategoryRepository`, `TransactionRepository` |
| Validates the currency claim, ownership, and category visibility | `Transaction`, `Category`, `FinancialAccount` |
| Searches with combined filters and pagination | `TransactionSpecifications`, `TransactionMapper` |
| Refuses to modify transfer legs | `TransactionType` |

### TransferService / TransferServiceImpl

| Responsibilities | Collaborators |
|------------------|---------------|
| Creates transfers atomically between the user's accounts | `Transfer`, `FinancialAccountRepository`, `TransferRepository` |
| Reads and deletes transfers (both legs) owned by the user | `TransferRepository`, `TransferMapper` |

---

### TokenService / JwtTokenService

| Responsibilities | Collaborators |
|------------------|---------------|
| Issues signed access tokens carrying the user id and expiry | `JwtEncoder`, `JwtProperties`, `Clock` |

### CurrentUserProvider / SecurityContextCurrentUserProvider

| Responsibilities | Collaborators |
|------------------|---------------|
| Resolves the authenticated user's id from the security context | `SecurityContextHolder`, `Jwt` |

### GlobalExceptionHandler

| Responsibilities | Collaborators |
|------------------|---------------|
| Translates domain, validation, and security exceptions into RFC 7807 responses with the correct status | `NestException` hierarchy, `ProblemDetail` |
| Hides internal details of unexpected errors | Logger |
