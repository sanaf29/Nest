# Sprint 1 — Sequence Diagrams

These diagrams show how the main Sprint 1 use cases flow through the layers. Each one shows where validation, ownership checks, and transaction boundaries happen.

## 1. Register

```mermaid
sequenceDiagram
    actor Client
    participant AC as AuthController
    participant AS as AuthServiceImpl
    participant UR as UserRepository
    participant PE as PasswordEncoder (BCrypt)
    participant U as User
    participant DB as SQL Server

    Client->>AC: POST /api/auth/register {names, email, password, currency}
    AC->>AC: @Valid RegisterRequest (400 on failure)
    AC->>AS: register(request)
    activate AS
    Note over AS: @Transactional
    AS->>U: normalizeEmail(email)
    AS->>UR: existsByEmail(normalized)
    UR->>DB: SELECT
    alt email taken
        AS-->>AC: ConflictException
        AC-->>Client: 409 problem+json
    else new email
        AS->>PE: encode(password)
        AS->>U: new User(..., hash, currency) [validates invariants]
        AS->>UR: save(user)
        UR->>DB: INSERT users
        Note over DB: UQ_users_email backs up the check under races
        AS-->>AC: UserResponse (no hash)
        deactivate AS
        AC-->>Client: 201 Created, Location: /api/users/me
    end
```

## 2. Login and Token Issue

```mermaid
sequenceDiagram
    actor Client
    participant AC as AuthController
    participant AS as AuthServiceImpl
    participant UR as UserRepository
    participant U as User
    participant PE as PasswordEncoder
    participant TS as JwtTokenService
    participant JE as JwtEncoder (HS256)

    Client->>AC: POST /api/auth/login {email, password}
    AC->>AS: login(request)
    AS->>UR: findByEmail(normalized)
    alt unknown email
        AS->>PE: matches(password, timingEqualizerHash)
        Note right of AS: comparable response time
        AS-->>Client: 401 "Invalid email or password."
    else user found
        AS->>U: passwordMatches(password, encoder)
        U->>PE: matches(password, passwordHash)
        alt wrong password
            AS-->>Client: 401 "Invalid email or password."
        else correct
            AS->>TS: issue(user)
            TS->>JE: encode(sub=userId, iss=nest, iat, exp, email)
            TS-->>AS: AccessToken(value, expiresAt)
            AS-->>Client: 200 {accessToken, tokenType: Bearer, expiresAt, user}
        end
    end
```

## 3. Authenticated Request With Ownership Check

```mermaid
sequenceDiagram
    actor Client
    participant F as BearerTokenAuthenticationFilter
    participant JD as JwtDecoder
    participant EP as ProblemDetailAuthenticationEntryPoint
    participant C as FinancialAccountController
    participant CU as CurrentUserProvider
    participant S as FinancialAccountServiceImpl
    participant R as FinancialAccountRepository

    Client->>F: GET /api/accounts/42  (Authorization: Bearer …)
    F->>JD: decode(token)
    alt missing / invalid / expired / wrong issuer
        F->>EP: commence()
        EP-->>Client: 401 problem+json, WWW-Authenticate: Bearer
    else valid
        JD-->>F: Jwt(sub=7)
        F->>C: get(42)
        C->>CU: currentUserId()
        CU-->>C: 7 (from the token, never from the request)
        C->>S: get(7, 42)
        S->>R: findByIdAndUserId(42, 7)
        alt account 42 belongs to someone else
            R-->>S: empty
            S-->>Client: 404 "Account 42 was not found." (same as nonexistent)
        else owned
            R-->>S: account
            S-->>Client: 200 AccountResponse
        end
    end
```

## 4. Record an Expense

```mermaid
sequenceDiagram
    actor Client
    participant C as TransactionController
    participant S as TransactionServiceImpl
    participant AR as FinancialAccountRepository
    participant CR as CategoryRepository
    participant T as Transaction
    participant FA as FinancialAccount
    participant CAT as Category
    participant M as Money
    participant TR as TransactionRepository

    Client->>C: POST /api/transactions {accountId, categoryId, EXPENSE, 84.37, date}
    C->>C: @Valid TransactionRequest (amount > 0, required fields)
    C->>S: create(userId, request)
    Note over S: @Transactional
    S->>AR: findByIdAndUserId(accountId, userId)
    AR-->>S: account (or 404)
    S->>S: requireMatchingCurrency(account, request.currency) (422)
    S->>CR: findVisible(categoryId, userId)
    CR-->>S: category (default or own, or 404)
    S->>T: new Transaction(account, category, EXPENSE, amount, ...)
    T->>FA: requireOpenForActivity() (422 if closed)
    T->>CAT: isVisibleTo(owner); requireUsableFor(EXPENSE) (422 if archived or wrong type)
    T->>M: Money.of(amount, account.currency) (422 if too many decimals)
    S->>TR: save(transaction)
    S-->>Client: 201 TransactionResponse (currency = account currency)
```

## 5. Create a Transfer (Atomic)

```mermaid
sequenceDiagram
    actor Client
    participant C as TransferController
    participant S as TransferServiceImpl
    participant AR as FinancialAccountRepository
    participant TF as Transfer
    participant T as Transaction
    participant R as TransferRepository
    participant DB as SQL Server

    Client->>C: POST /api/transfers {source, destination, 500.00, date}
    C->>S: create(userId, request)
    activate S
    Note over S: @Transactional: all or nothing
    S->>AR: findByIdAndUserId(source, userId)
    S->>AR: findByIdAndUserId(destination, userId)
    S->>TF: Transfer.between(source, destination, 500.00, date, description)
    TF->>TF: check distinct, same owner, same currency, amount > 0
    TF->>T: transferLeg(TRANSFER_OUT, source)
    TF->>T: transferLeg(TRANSFER_IN, destination)
    Note over TF: both legs are open-account checked
    S->>R: saveAndFlush(transfer)
    R->>DB: INSERT transfers
    R->>DB: INSERT transactions (OUT) (cascade)
    R->>DB: INSERT transactions (IN) (cascade)
    Note over DB: CK_transactions_shape and UX_transactions_transfer_type enforce the pair
    deactivate S
    S-->>Client: 201 TransferResponse {outgoingTransactionId, incomingTransactionId}
```

## 6. List Accounts With Balances (One Aggregate Query)

```mermaid
sequenceDiagram
    actor Client
    participant S as FinancialAccountServiceImpl
    participant AR as FinancialAccountRepository
    participant TR as TransactionRepository
    participant FA as FinancialAccount

    Client->>S: list(userId, includeInactive=false)
    S->>AR: findByUserIdAndActiveTrueOrderByNameAsc(userId)
    AR-->>S: [chequing, savings, visa]
    S->>TR: sumByAccountAndType([ids])
    Note right of TR: SELECT account_id, type, SUM(amount)<br/>GROUP BY account_id, type
    TR-->>S: rows (accountId, type, total)
    loop each account
        S->>FA: calculateBalance(totalsByType)
        FA-->>S: opening + Σ sign(type) × total
    end
    S-->>Client: 200 [AccountResponse with currentBalance]
```
