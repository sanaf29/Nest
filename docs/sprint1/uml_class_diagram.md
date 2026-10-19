# Sprint 1 — UML Class Diagram (As Built)

This diagram reflects the code at the end of Sprint 1. Differences from the Sprint 0 design ([class_diagram.md](../sprint0/class_diagram.md)) are listed at the end.

## 1. Domain Model (`com.nest.model`)

```mermaid
classDiagram
    direction LR

    class AuditableEntity {
        <<abstract>>
        -Instant createdAt
        -Instant updatedAt
        #onCreate()
        #onUpdate()
        +getCreatedAt() Instant
        +getUpdatedAt() Instant
    }

    class User {
        -Long id
        -String firstName
        -String lastName
        -String email
        -String passwordHash
        -String preferredCurrency
        +normalizeEmail(String)$ String
        +updateProfile(firstName, lastName, currency)
        +changePasswordHash(hash)
        +passwordMatches(raw, PasswordEncoder) boolean
    }

    class FinancialAccount {
        -Long id
        -User user
        -String name
        -AccountType accountType
        -String currency
        -BigDecimal openingBalance
        -boolean active
        -Long version
        +rename(name)
        +correctOpeningBalance(amount)
        +deactivate()
        +isOwnedBy(userId) boolean
        +ownerId() Long
        +requireOpenForActivity()
        +calculateBalance(Map~TransactionType,BigDecimal~) Money
        +openingBalance() Money
    }

    class Category {
        -Long id
        -User user
        -String name
        -CategoryType categoryType
        -String color
        -boolean archived
        -Long version
        +isDefault() boolean
        +isOwnedBy(userId) boolean
        +isVisibleTo(userId) boolean
        +update(name, color)
        +archive()
        +supports(TransactionType) boolean
        +requireUsableFor(TransactionType)
    }

    class Transaction {
        -Long id
        -FinancialAccount account
        -Category category
        -Transfer transfer
        -TransactionType type
        -BigDecimal amount
        -String description
        -LocalDate transactionDate
        -Long version
        ~transferLeg(transfer, account, type, Money, description, date)$ Transaction
        +revise(account, category, type, amount, description, date)
        +requireRemovable()
        +isTransferLeg() boolean
        +currency() String
        +money() Money
        +signedAmount() BigDecimal
    }

    class Transfer {
        -Long id
        -List~Transaction~ legs
        +between(source, destination, amount, date, description)$ Transfer
        +requireRemovable()
        +outgoing() Transaction
        +incoming() Transaction
        +source() FinancialAccount
        +destination() FinancialAccount
        +amount() Money
        +date() LocalDate
    }

    class Money {
        <<record>>
        +BigDecimal amount
        +String currency
        +ROUNDING$ RoundingMode
        +of(amount, currency)$ Money
        +zero(currency)$ Money
        +plus(Money) Money
        +minus(Money) Money
        +isNegative() boolean
        +isPositive() boolean
    }

    class Currencies {
        <<utility>>
        +isSupported(code)$ boolean
        +requireSupported(code)$ String
        +minorUnits(code)$ int
    }

    class AccountType {
        <<enumeration>>
        CHECKING
        SAVINGS
        CASH
        CREDIT_CARD
        +allowsNegativeOpeningBalance() boolean
    }

    class CategoryType {
        <<enumeration>>
        INCOME
        EXPENSE
    }

    class TransactionType {
        <<enumeration>>
        INCOME
        EXPENSE
        TRANSFER_IN
        TRANSFER_OUT
        +balanceSign() int
        +isTransfer() boolean
        +requiredCategoryType() Optional~CategoryType~
    }

    AuditableEntity <|-- User
    AuditableEntity <|-- FinancialAccount
    AuditableEntity <|-- Category
    AuditableEntity <|-- Transaction
    AuditableEntity <|-- Transfer

    FinancialAccount "0..*" --> "1" User : owner
    Category "0..*" --> "0..1" User : owner (null = default)
    Transaction "0..*" --> "1" FinancialAccount : posted to
    Transaction "0..*" --> "0..1" Category : classified by
    Transfer "1" *-- "2" Transaction : legs (cascade)
    FinancialAccount ..> AccountType
    FinancialAccount ..> Money
    Transaction ..> TransactionType
    Transaction ..> Money
    Category ..> CategoryType
    TransactionType ..> CategoryType
    Money ..> Currencies
```

## 2. Application Layers

```mermaid
classDiagram
    direction TB

    namespace controller {
        class AuthController
        class UserController
        class FinancialAccountController
        class CategoryController
        class TransactionController
        class TransferController
    }

    namespace service {
        class AuthService {<<interface>>}
        class UserService {<<interface>>}
        class FinancialAccountService {<<interface>>}
        class CategoryService {<<interface>>}
        class TransactionService {<<interface>>}
        class TransferService {<<interface>>}
    }

    namespace security {
        class TokenService {<<interface>>}
        class JwtTokenService
        class CurrentUserProvider {<<interface>>}
        class SecurityContextCurrentUserProvider
    }

    namespace repository {
        class UserRepository {<<interface>>}
        class FinancialAccountRepository {<<interface>>}
        class CategoryRepository {<<interface>>}
        class TransactionRepository {<<interface>>}
        class TransferRepository {<<interface>>}
        class TransactionSpecifications {<<utility>>}
    }

    AuthController --> AuthService
    UserController --> UserService
    FinancialAccountController --> FinancialAccountService
    CategoryController --> CategoryService
    TransactionController --> TransactionService
    TransferController --> TransferService
    UserController --> CurrentUserProvider
    FinancialAccountController --> CurrentUserProvider
    CategoryController --> CurrentUserProvider
    TransactionController --> CurrentUserProvider
    TransferController --> CurrentUserProvider

    AuthServiceImpl ..|> AuthService
    UserServiceImpl ..|> UserService
    FinancialAccountServiceImpl ..|> FinancialAccountService
    CategoryServiceImpl ..|> CategoryService
    TransactionServiceImpl ..|> TransactionService
    TransferServiceImpl ..|> TransferService
    JwtTokenService ..|> TokenService
    SecurityContextCurrentUserProvider ..|> CurrentUserProvider

    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> TokenService
    UserServiceImpl --> UserRepository
    FinancialAccountServiceImpl --> FinancialAccountRepository
    FinancialAccountServiceImpl --> TransactionRepository : balance totals
    FinancialAccountServiceImpl --> UserRepository
    CategoryServiceImpl --> CategoryRepository
    CategoryServiceImpl --> UserRepository
    TransactionServiceImpl --> TransactionRepository
    TransactionServiceImpl --> FinancialAccountRepository
    TransactionServiceImpl --> CategoryRepository
    TransactionServiceImpl ..> TransactionSpecifications
    TransferServiceImpl --> TransferRepository
    TransferServiceImpl --> FinancialAccountRepository
```

Cross-cutting classes not shown above:
- **Errors:** `GlobalExceptionHandler`, `ProblemDetails`, and the `NestException` hierarchy (`ResourceNotFoundException`, `ConflictException`, `BusinessRuleException`, `ForbiddenOperationException`, `InvalidCredentialsException`)
- **Configuration:** `SecurityConfig`, `JwtConfig`, `OpenApiConfig`, `ClockConfig`, `SecurityProperties`, `CorsProperties`, `JwtProperties`
- **Security:** `ProblemDetailAuthenticationEntryPoint`, `ProblemDetailAccessDeniedHandler`
- **Validation:** `@ValidCurrency` / `CurrencyCodeValidator`
- **Mappers:** one per resource
- **Request DTOs:** `RegisterRequest`, `LoginRequest`, `UpdateProfileRequest`, `CreateAccountRequest`, `UpdateAccountRequest`, `CreateCategoryRequest`, `UpdateCategoryRequest`, `TransactionRequest`, `TransactionFilter`, `TransferRequest`
- **Response DTOs:** `UserResponse`, `AuthResponse`, `AccountResponse`, `CategoryResponse`, `TransactionResponse`, `TransferResponse`, `PageResponse<T>`

## 3. Exception Hierarchy

```mermaid
classDiagram
    RuntimeException <|-- NestException
    NestException <|-- ResourceNotFoundException : 404
    NestException <|-- ConflictException : 409
    NestException <|-- BusinessRuleException : 422
    NestException <|-- ForbiddenOperationException : 403
    NestException <|-- InvalidCredentialsException : 401
```

## Changes Since the Sprint 0 Design

| Area | Sprint 0 design | As built | Reason |
|------|-----------------|----------|--------|
| `User` password check | `passwordHash()` getter | `passwordMatches(raw, encoder)`; no getter | The hash never leaves the entity |
| `FinancialAccount` | — | `ownerId()`, `requireOpenForActivity()` | Transactions and transfers need these checks |
| `Category` | `supports()` | plus `requireUsableFor()`, `isOwnedBy()` | One method guards archived state and type compatibility |
| `Transaction` | public constructor only | plus package-private `transferLeg()` factory, `requireRemovable()` | Only `Transfer` may create balanced pairs |
| `Transfer` | `between`, `outgoing`, `incoming` | plus `requireRemovable()`, `source()`, `destination()`, `amount()`, `date()` | Used by the service and mapper |
| `Currencies` | not planned | utility class | Shared by `Money`, `User`, and `@ValidCurrency` |
| `TransactionSpecifications` | planned | built as planned | Specification pattern for search |
