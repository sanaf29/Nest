// TypeScript mirrors of the backend DTOs (com.nest.dto). Monetary values arrive as JSON numbers
// with at most the currency's minor units; they are displayed with Intl and never summed
// across currencies on the client.

export type ISODate = string; // yyyy-MM-dd
export type ISOInstant = string; // 2026-10-19T14:03:22.114Z

export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  timestamp?: ISOInstant;
  errors?: { field: string; message: string }[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---- Auth & users ----------------------------------------------------------------------------
export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  preferredCurrency: string;
  createdAt: ISOInstant;
  updatedAt: ISOInstant;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: ISOInstant;
  user: UserResponse;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  preferredCurrency: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  preferredCurrency: string;
}

// ---- Accounts --------------------------------------------------------------------------------
export type AccountType = 'CHECKING' | 'SAVINGS' | 'CASH' | 'CREDIT_CARD';

export interface AccountResponse {
  id: number;
  name: string;
  accountType: AccountType;
  currency: string;
  openingBalance: number;
  currentBalance: number;
  active: boolean;
  createdAt: ISOInstant;
  updatedAt: ISOInstant;
}

export interface CreateAccountRequest {
  name: string;
  accountType: AccountType;
  currency?: string;
  openingBalance: number;
}

export interface UpdateAccountRequest {
  name: string;
  openingBalance: number;
}

// ---- Categories ------------------------------------------------------------------------------
export type CategoryType = 'INCOME' | 'EXPENSE';

export interface CategoryResponse {
  id: number;
  name: string;
  categoryType: CategoryType;
  color: string;
  isDefault: boolean;
  archived: boolean;
}

export interface CategoryRequest {
  name: string;
  categoryType?: CategoryType;
  color?: string;
}

// ---- Transactions & transfers ----------------------------------------------------------------
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export interface TransactionResponse {
  id: number;
  accountId: number;
  accountName: string;
  categoryId: number | null;
  categoryName: string | null;
  type: TransactionType;
  amount: number;
  currency: string;
  description: string | null;
  transactionDate: ISODate;
  transferId: number | null;
  createdAt: ISOInstant;
  updatedAt: ISOInstant;
}

export interface TransactionRequest {
  accountId: number;
  categoryId: number;
  type: 'INCOME' | 'EXPENSE';
  amount: number;
  description?: string;
  transactionDate: ISODate;
}

export interface TransactionFilter {
  accountId?: number;
  categoryId?: number;
  type?: TransactionType;
  from?: ISODate;
  to?: ISODate;
  minAmount?: number;
  maxAmount?: number;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface TransferRequest {
  sourceAccountId: number;
  destinationAccountId: number;
  amount: number;
  description?: string;
  transferDate: ISODate;
}

export interface TransferResponse {
  id: number;
  sourceAccountId: number;
  sourceAccountName: string;
  destinationAccountId: number;
  destinationAccountName: string;
  amount: number;
  currency: string;
  transferDate: ISODate;
  outgoingTransactionId: number;
  incomingTransactionId: number;
  createdAt: ISOInstant;
}

// ---- Budgets ---------------------------------------------------------------------------------
export type SavingsStrategy = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type BudgetStatus = 'ON_TRACK' | 'NEAR_LIMIT' | 'EXCEEDED';

export interface AllocationRequest {
  categoryId: number;
  limit: number;
}

export interface CreateBudgetRequest {
  year: number;
  month: number;
  currency?: string;
  plannedIncome: number;
  savingsStrategy: SavingsStrategy;
  savingsValue: number;
  allocations: AllocationRequest[];
}

export type UpdateBudgetRequest = Omit<CreateBudgetRequest, 'year' | 'month' | 'currency'>;

export interface BudgetResponse {
  id: number;
  year: number;
  month: number;
  currency: string;
  plannedIncome: number;
  savingsStrategy: SavingsStrategy;
  savingsValue: number;
  savingsTarget: number;
  spendableAmount: number;
  allocatedAmount: number;
  unallocatedAmount: number;
  allocations: {
    id: number;
    categoryId: number;
    categoryName: string;
    color: string;
    categoryArchived: boolean;
    limit: number;
  }[];
  createdAt: ISOInstant;
  updatedAt: ISOInstant;
}

export interface BudgetSummaryResponse {
  budgetId: number;
  year: number;
  month: number;
  currency: string;
  plannedIncome: number;
  savingsTarget: number;
  spendableAmount: number;
  allocatedAmount: number;
  actualIncome: number;
  actualExpenses: number;
  netCashFlow: number;
  remaining: number;
  percentUsed: number | null;
  savingsProgressPercent: number | null;
  unbudgetedSpending: number;
  categories: {
    categoryId: number;
    categoryName: string;
    color: string;
    limit: number;
    spent: number;
    remaining: number;
    percentUsed: number;
    status: BudgetStatus;
  }[];
}

// ---- Savings goals ---------------------------------------------------------------------------
export type GoalStatus = 'ACTIVE' | 'ACHIEVED';

export interface SavingsGoalResponse {
  id: number;
  name: string;
  currency: string;
  targetAmount: number;
  savedAmount: number;
  remainingAmount: number;
  progressPercent: number;
  targetDate: ISODate | null;
  status: GoalStatus;
  achievedAt: ISOInstant | null;
  contributions: { id: number; amount: number; contributionDate: ISODate; note: string | null; createdAt: ISOInstant }[];
  createdAt: ISOInstant;
}

export interface CreateGoalRequest {
  name: string;
  targetAmount: number;
  currency?: string;
  targetDate?: ISODate | null;
}

export interface UpdateGoalRequest {
  name: string;
  targetAmount: number;
  targetDate: ISODate | null;
}

export interface ContributionRequest {
  amount: number;
  contributionDate?: ISODate;
  note?: string;
  idempotencyKey?: string;
}

// ---- Dashboard -------------------------------------------------------------------------------
export interface DashboardSummaryResponse {
  year: number;
  month: number;
  preferredCurrency: string;
  balances: { currency: string; total: number; accountCount: number }[];
  cashFlow: { currency: string; income: number; expenses: number; net: number }[];
  budget: {
    budgetId: number;
    currency: string;
    spendable: number;
    spent: number;
    remaining: number;
    percentUsed: number | null;
    categoriesNearLimit: number;
    categoriesExceeded: number;
  } | null;
  activeGoals: {
    id: number;
    name: string;
    currency: string;
    savedAmount: number;
    targetAmount: number;
    progressPercent: number;
    targetDate: ISODate | null;
    status: GoalStatus;
  }[];
  achievedGoalCount: number;
}

export interface CategorySpendingResponse {
  year: number;
  month: number;
  currency: string;
  total: number;
  categories: { categoryId: number; categoryName: string; color: string; amount: number; percentOfTotal: number }[];
}

export interface MonthlyTrendsResponse {
  currency: string;
  months: { year: number; month: number; income: number; expenses: number; net: number }[];
}

// ---- Insights --------------------------------------------------------------------------------
export type InsightSeverity = 'ALERT' | 'WARNING' | 'INFO' | 'POSITIVE';

export interface InsightsResponse {
  year: number;
  month: number;
  currency: string;
  asOf: ISODate;
  disclaimer: string;
  insights: { code: string; severity: InsightSeverity; title: string; message: string }[];
}

// ---- Recurring transactions ------------------------------------------------------------------
export type Frequency = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type RecurringStatus = 'ACTIVE' | 'PAUSED' | 'ENDED';

export interface RecurringRequest {
  accountId: number;
  categoryId: number;
  type: 'INCOME' | 'EXPENSE';
  amount: number;
  description: string;
  frequency: Frequency;
  startDate: ISODate;
  endDate?: ISODate | null;
  remindDaysBefore?: number | null;
}

export type UpdateRecurringRequest = Omit<RecurringRequest, 'type' | 'frequency' | 'startDate'>;

export interface RecurringResponse {
  id: number;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
  type: 'INCOME' | 'EXPENSE';
  amount: number;
  currency: string;
  description: string;
  frequency: Frequency;
  startDate: ISODate;
  endDate: ISODate | null;
  nextOccurrence: ISODate | null;
  upcoming: ISODate[];
  status: RecurringStatus;
  pauseReason: string | null;
  remindDaysBefore: number | null;
  createdAt: ISOInstant;
}

// ---- Reminders & notifications ---------------------------------------------------------------
export type ReminderType = 'BILL_PAYMENT' | 'BUDGET_THRESHOLD' | 'SUBSCRIPTION_RENEWAL' | 'SAVINGS_GOAL_DEADLINE' | 'PARKING' | 'CUSTOM';
export type ReminderStatus = 'SCHEDULED' | 'SENT' | 'DISMISSED';

export interface ReminderRequest {
  type: ReminderType;
  title: string;
  notes?: string;
  dueAt?: ISOInstant | null;
  offsetMinutes?: number;
  location?: string;
  savingsGoalId?: number | null;
  categoryId?: number | null;
  thresholdPercent?: number | null;
}

export interface ReminderResponse {
  id: number;
  type: ReminderType;
  title: string;
  notes: string | null;
  dueAt: ISOInstant | null;
  offsetMinutes: number;
  remindAt: ISOInstant | null;
  status: ReminderStatus;
  sentAt: ISOInstant | null;
  location: string | null;
  savingsGoalId: number | null;
  savingsGoalName: string | null;
  categoryId: number | null;
  categoryName: string | null;
  thresholdPercent: number | null;
  createdAt: ISOInstant;
}

export type NotificationCategory = 'REMINDER' | 'BUDGET' | 'RECURRING' | 'GOAL' | 'SYSTEM';

export interface NotificationResponse {
  id: number;
  category: NotificationCategory;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: ISOInstant;
}

// ---- Shared expenses -------------------------------------------------------------------------
export type SplitMethod = 'EQUAL' | 'CUSTOM' | 'PERCENTAGE';

export interface SharedExpenseRequest {
  description: string;
  totalAmount: number;
  currency?: string;
  expenseDate: ISODate;
  splitMethod: SplitMethod;
  participants: { name: string; email?: string; me: boolean; paid: boolean; value?: number }[];
}

export interface SharedExpenseResponse {
  id: number;
  description: string;
  totalAmount: number;
  currency: string;
  expenseDate: ISODate;
  splitMethod: SplitMethod;
  settled: boolean;
  owedToYou: number;
  youOwe: number;
  participants: {
    id: number;
    name: string;
    email: string | null;
    me: boolean;
    paid: boolean;
    share: number;
    owes: boolean;
    settled: boolean;
    settledAt: ISOInstant | null;
  }[];
  createdAt: ISOInstant;
}

export interface SharedBalanceResponse {
  person: string;
  currency: string;
  amount: number;
}

// ---- Reports ---------------------------------------------------------------------------------
export interface MonthlyReportResponse {
  year: number;
  month: number;
  currency: string;
  totals: { income: number; expenses: number; net: number };
  previousMonth: { income: number; expenses: number; net: number };
  expensesByCategory: { category: string; color: string; amount: number; percentOfTotal: number }[];
  incomeByCategory: { category: string; color: string; amount: number; percentOfTotal: number }[];
  budget: {
    plannedIncome: number;
    savingsTarget: number;
    spendable: number;
    spent: number;
    remaining: number;
    categories: { category: string; limit: number; spent: number; percentUsed: number; status: BudgetStatus }[];
  } | null;
  goals: { name: string; currency: string; saved: number; target: number; progressPercent: number; status: GoalStatus }[];
  topExpenses: { date: ISODate; description: string | null; category: string | null; account: string; amount: number }[];
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
