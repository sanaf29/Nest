package com.nest.service.impl;

import com.nest.dto.request.CreateAccountRequest;
import com.nest.dto.request.UpdateAccountRequest;
import com.nest.dto.response.AccountResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.AccountMapper;
import com.nest.model.FinancialAccount;
import com.nest.model.Money;
import com.nest.model.User;
import com.nest.model.TransactionType;
import com.nest.repository.AccountTypeTotal;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransactionRepository;
import com.nest.repository.UserRepository;
import com.nest.service.FinancialAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialAccountServiceImpl implements FinancialAccountService {

    private final FinancialAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    public FinancialAccountServiceImpl(FinancialAccountRepository accountRepository,
                                       UserRepository userRepository,
                                       TransactionRepository transactionRepository,
                                       AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional
    public AccountResponse create(long userId, CreateAccountRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        requireUniqueActiveName(userId, request.name(), null);
        String currency = request.currency() != null ? request.currency() : owner.getPreferredCurrency();

        FinancialAccount account = new FinancialAccount(
                owner, request.name(), request.accountType(), currency, request.openingBalance());
        return toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> list(long userId, boolean includeInactive) {
        List<FinancialAccount> accounts = includeInactive
                ? accountRepository.findByUserIdOrderByNameAsc(userId)
                : accountRepository.findByUserIdAndActiveTrueOrderByNameAsc(userId);
        Map<Long, Map<TransactionType, BigDecimal>> totals = totalsFor(accounts.stream().map(FinancialAccount::getId).toList());
        return accounts.stream()
                .map(account -> accountMapper.toResponse(account,
                        account.calculateBalance(totals.getOrDefault(account.getId(), Map.of()))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse get(long userId, long accountId) {
        return toResponse(findOwned(userId, accountId));
    }

    @Override
    @Transactional
    public AccountResponse update(long userId, long accountId, UpdateAccountRequest request) {
        FinancialAccount account = findOwned(userId, accountId);
        requireUniqueActiveName(userId, request.name(), accountId);
        account.rename(request.name());
        account.correctOpeningBalance(request.openingBalance());
        // Flush so the version check and @PreUpdate timestamp happen before the response is built.
        return toResponse(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional
    public void deactivate(long userId, long accountId) {
        findOwned(userId, accountId).deactivate();
    }

    private FinancialAccount findOwned(long userId, long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    private void requireUniqueActiveName(long userId, String name, Long excludedAccountId) {
        String trimmed = name.strip();
        boolean taken = excludedAccountId == null
                ? accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId, trimmed)
                : accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(userId, trimmed, excludedAccountId);
        if (taken) {
            throw new ConflictException("You already have an open account named '%s'.".formatted(trimmed));
        }
    }

    private AccountResponse toResponse(FinancialAccount account) {
        Map<TransactionType, BigDecimal> totals = account.getId() == null
                ? Map.of()
                : totalsFor(List.of(account.getId())).getOrDefault(account.getId(), Map.of());
        return accountMapper.toResponse(account, account.calculateBalance(totals));
    }

    /** Per-type totals for the given accounts, loaded with one GROUP BY query however many accounts there are. */
    private Map<Long, Map<TransactionType, BigDecimal>> totalsFor(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<TransactionType, BigDecimal>> totals = new HashMap<>();
        for (AccountTypeTotal row : transactionRepository.sumByAccountAndType(accountIds)) {
            totals.computeIfAbsent(row.accountId(), id -> new EnumMap<>(TransactionType.class))
                    .put(row.type(), row.total());
        }
        return totals;
    }
}
