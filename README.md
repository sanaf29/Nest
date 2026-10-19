# Nest — Personal Finance and Expense Tracking System

## Overview

**Nest** is a full-stack personal finance application designed to help users manage their financial activities, track expenses, and maintain control over their monthly budget.

The system provides users with a centralized platform to:

* Manage multiple financial accounts
* Record and categorize expenses
* Monitor monthly spending and remaining budget
* Analyze financial behavior through structured data

The application is developed using a modular architecture and follows an Agile, sprint-based development process.

---

## Features

### User Management

* User registration and authentication
* Secure password storage (hashing)
* User-defined financial settings:

  * Monthly allowance
  * Monthly paycheck
  * Savings percentage

---

### Account Management

* Create and manage multiple accounts (e.g., checking, savings)
* Update account balances
* Retrieve all user accounts
* Calculate total balance across all accounts

---

### Expense Management

* Record expenses with amount and date
* Assign categories and subcategories:

  * Car (insurance, parking, gas)
  * House (utilities)
  * Groceries
  * Subscriptions
  * Others
* Retrieve expenses by month

---

### Monthly Budgeting

* Calculate total monthly spending
* Determine remaining budget
* Compare actual spending against planned allowance
* Generate monthly financial summaries

---

### Dashboard (Frontend)

* Display total balance
* Display monthly spending
* Display remaining budget
* Visualize expense distribution using charts

---

### Security

* Authentication system
* Password hashing

---

## Development Roadmap

### Phase 1 — Core Backend (MVP)

**Objective:** Implement essential backend functionality.

**Sprints:**

* Sprint 1: Authentication and User Profile
* Sprint 2: Account Management
* Sprint 3: Expense Tracking and Monthly Calculations

**Key Deliverables:**

* Functional REST APIs
* Data persistence and validation
* Core financial calculations

---

### Phase 1.5 — Frontend and Integration

**Objective:** Develop user interface and integrate with backend services.

**Sprints:**

* Sprint 4: Frontend setup and dashboard UI
* Sprint 5: API integration

**Key Deliverables:**

* Interactive user interface
* End-to-end functionality

---

### Phase 2 — Feature Enhancements

**Objective:** Introduce intelligent and automated features.

**Planned Features:**

* Spending insights
* Goal tracking
* Recurring expenses
* Monthly trends and analytics

---

### Phase 3 — Advanced Capabilities

**Objective:** Improve usability, security, and reporting.

**Planned Features:**

* Budget alerts and notifications
* Export functionality (PDF/CSV)
* Advanced security features (e.g., multi-factor authentication)

---

## Core Entities

### User

* id
* name
* email
* password
* monthlyAllowance
* monthlyPaycheck
* savingPercentage

---

### Account

* id
* userId
* accountName
* balance

---

### Expense

* id
* userId
* amount
* category
* subcategory
* date

---

### MonthlySummary

* id
* userId
* month
* totalSpent
* remainingBudget
* totalBalance

---

## Technology Stack

### Backend

* Java
* Spring Boot
* RESTful APIs
* JPA / Hibernate
* SQL database

---

### Frontend

* React
* JavaScript or TypeScript
* Charting libraries

---

### Tools

* Git and GitHub
* Postman (API testing)

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

### Frontend

The React frontend is planned for Sprint 2.

---

## API Overview

### Authentication

* POST /api/auth/signup
* POST /api/auth/login

### Accounts

* GET /api/accounts
* POST /api/accounts
* PUT /api/accounts/{id}

### Expenses

* POST /api/expenses
* GET /api/expenses/month

---

## Development Approach

* Agile methodology with sprint-based iterations
* MVP-first development strategy
* Backend-first implementation followed by frontend integration
* Modular and scalable design

---

## Project Objectives

* Develop a real-world full-stack application
* Apply software engineering principles (REST, MVC, modular design)
* Gain experience in both backend and frontend development
* Implement structured financial data management

---

## Author

Sana Faghfouri
Computer Science Student, York University

---
