package com.nest.service.impl;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.response.MonthlyReportResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.Money;
import com.nest.model.MonthlyActuals;
import com.nest.model.Transaction;
import com.nest.model.TransactionType;
import com.nest.repository.CategoryTotal;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.TransactionRepository;
import com.nest.repository.UserRepository;
import com.nest.service.LedgerAnalytics;
import com.nest.service.ReportService;
import com.nest.service.report.CsvWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static com.nest.repository.TransactionSpecifications.amountAtLeast;
import static com.nest.repository.TransactionSpecifications.amountAtMost;
import static com.nest.repository.TransactionSpecifications.descriptionContains;
import static com.nest.repository.TransactionSpecifications.inAccount;
import static com.nest.repository.TransactionSpecifications.inCategory;
import static com.nest.repository.TransactionSpecifications.inCurrency;
import static com.nest.repository.TransactionSpecifications.ofType;
import static com.nest.repository.TransactionSpecifications.onOrAfter;
import static com.nest.repository.TransactionSpecifications.onOrBefore;
import static com.nest.repository.TransactionSpecifications.ownedBy;

/** Reports reuse the same user-scoped queries as the dashboard, so exports can never include another user's data. */
@Service
public class ReportServiceImpl implements ReportService {

    static final int MAX_EXPORT_ROWS = 50_000;
    private static final int PAGE = 1_000;

    private final LedgerAnalytics ledgerAnalytics;
    private final TransactionRepository transactionRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(LedgerAnalytics ledgerAnalytics, TransactionRepository transactionRepository,
                             MonthlyBudgetRepository budgetRepository, SavingsGoalRepository goalRepository,
                             UserRepository userRepository) {
        this.ledgerAnalytics = ledgerAnalytics;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(long userId, YearMonth month, String currency) {
        String cur = currency != null ? currency
                : userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId)).getPreferredCurrency();
        MonthlyActuals actuals = ledgerAnalytics.monthlyActuals(userId, month, cur);
        MonthlyActuals previous = ledgerAnalytics.monthlyActuals(userId, month.minusMonths(1), cur);

        List<CategoryTotal> expenses = ledgerAnalytics.expensesByCategory(userId, month, cur);
        List<CategoryTotal> income = transactionRepository.sumByCategory(userId, cur, month.atDay(1), month.atEndOfMonth(),
                TransactionType.INCOME);

        MonthlyReportResponse.Budget budget = budgetRepository
                .findByUserIdAndYearAndMonthAndCurrency(userId, month.getYear(), month.getMonthValue(), cur)
                .map(b -> b.summarize(actuals))
                .map(s -> new MonthlyReportResponse.Budget(s.plannedIncome().amount(), s.savingsTarget().amount(),
                        s.spendable().amount(), s.actualExpenses().amount(), s.remaining().amount(),
                        s.categories().stream()
                                .map(l -> new MonthlyReportResponse.BudgetLine(l.category().getName(), l.limit().amount(),
                                        l.spent().amount(), l.percentUsed(), l.status()))
                                .toList()))
                .orElse(null);

        List<MonthlyReportResponse.Goal> goals = goalRepository.findByUserIdOrderByStatusDescTargetDateAscNameAsc(userId).stream()
                .map(g -> new MonthlyReportResponse.Goal(g.getName(), g.getCurrency(), g.savedAmount().amount(),
                        g.target().amount(), g.progressPercent(), g.getStatus()))
                .toList();

        Specification<Transaction> topSpec = Specification.allOf(ownedBy(userId), inCurrency(cur), ofType(TransactionType.EXPENSE),
                onOrAfter(month.atDay(1)), onOrBefore(month.atEndOfMonth()));
        List<MonthlyReportResponse.TopExpense> top = transactionRepository
                .findAll(topSpec, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "amount").and(Sort.by("id"))))
                .map(t -> new MonthlyReportResponse.TopExpense(t.getTransactionDate(), t.getDescription(),
                        t.getCategory() == null ? null : t.getCategory().getName(), t.getAccount().getName(), t.money().amount()))
                .getContent();

        return new MonthlyReportResponse(month.getYear(), month.getMonthValue(), cur, totals(actuals), totals(previous),
                byCategory(expenses, cur), byCategory(income, cur), budget, goals, top);
    }

    private static MonthlyReportResponse.Totals totals(MonthlyActuals a) {
        return new MonthlyReportResponse.Totals(a.income().amount(), a.totalExpenses().amount(), a.netCashFlow().amount());
    }

    private static List<MonthlyReportResponse.CategoryAmount> byCategory(List<CategoryTotal> totals, String currency) {
        Money sum = totals.stream().map(t -> Money.of(t.total(), currency)).reduce(Money.zero(currency), Money::plus);
        return totals.stream()
                .sorted(Comparator.comparing(CategoryTotal::total).reversed().thenComparing(CategoryTotal::categoryName))
                .map(t -> new MonthlyReportResponse.CategoryAmount(t.categoryName(), t.color(), Money.of(t.total(), currency).amount(),
                        sum.isPositive() ? t.total().multiply(BigDecimal.valueOf(100)).divide(sum.amount(), 2, RoundingMode.HALF_EVEN)
                                : BigDecimal.ZERO.setScale(2)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] monthlyCsv(long userId, YearMonth month, String currency) {
        MonthlyReportResponse r = monthly(userId, month, currency);
        CsvWriter csv = new CsvWriter().row("Section", "Item", "Amount", "Currency", "Percent");
        csv.row("Summary", "Income", r.totals().income(), r.currency(), null);
        csv.row("Summary", "Expenses", r.totals().expenses(), r.currency(), null);
        csv.row("Summary", "Net cash flow", r.totals().net(), r.currency(), null);
        csv.row("Previous month", "Income", r.previousMonth().income(), r.currency(), null);
        csv.row("Previous month", "Expenses", r.previousMonth().expenses(), r.currency(), null);
        csv.row("Previous month", "Net cash flow", r.previousMonth().net(), r.currency(), null);
        r.expensesByCategory().forEach(c -> csv.row("Expenses by category", c.category(), c.amount(), r.currency(), c.percentOfTotal()));
        r.incomeByCategory().forEach(c -> csv.row("Income by category", c.category(), c.amount(), r.currency(), c.percentOfTotal()));
        if (r.budget() != null) {
            csv.row("Budget", "Planned income", r.budget().plannedIncome(), r.currency(), null);
            csv.row("Budget", "Savings target", r.budget().savingsTarget(), r.currency(), null);
            csv.row("Budget", "Remaining", r.budget().remaining(), r.currency(), null);
            r.budget().categories().forEach(l -> csv.row("Budget limit", l.category(), l.spent(), r.currency(), l.percentUsed()));
        }
        r.goals().forEach(g -> csv.row("Savings goal", g.name(), g.saved(), g.currency(), g.progressPercent()));
        return csv.toBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] transactionsCsv(long userId, TransactionFilter f) {
        Specification<Transaction> spec = Specification.allOf(ownedBy(userId), inAccount(f.accountId()), inCategory(f.categoryId()),
                ofType(f.type()), onOrAfter(f.from()), onOrBefore(f.to()), amountAtLeast(f.minAmount()),
                amountAtMost(f.maxAmount()), descriptionContains(f.q()));
        CsvWriter csv = new CsvWriter().row("Date", "Type", "Account", "Category", "Description", "Amount", "Currency", "Transfer ID");
        Sort order = Sort.by("transactionDate").and(Sort.by("id"));
        int written = 0;
        for (int page = 0; written < MAX_EXPORT_ROWS; page++) {
            Page<Transaction> batch = transactionRepository.findAll(spec, PageRequest.of(page, PAGE, order));
            for (Transaction t : batch.getContent()) {
                if (written++ >= MAX_EXPORT_ROWS) break;
                csv.row(t.getTransactionDate(), t.getType().name(), t.getAccount().getName(),
                        t.getCategory() == null ? null : t.getCategory().getName(), t.getDescription(),
                        t.signedAmount(), t.currency(), t.isTransferLeg() ? t.getTransfer().getId() : null);
            }
            if (!batch.hasNext()) break;
        }
        return csv.toBytes();
    }
}
