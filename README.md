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

### Backend Setup

```bash
git clone <repository-url>
cd backend
mvn spring-boot:run
```

### Frontend Setup

```bash
cd frontend
npm install
npm start
```

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
