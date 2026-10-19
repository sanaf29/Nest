package com.nest.service.impl;

import com.nest.dto.request.TransactionFilter;
import com.nest.dto.request.TransactionRequest;
import com.nest.dto.response.PageResponse;
import com.nest.dto.response.TransactionResponse;
import com.nest.exception.BusinessRuleException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.TransactionMapper;
import com.nest.model.Category;
import com.nest.model.FinancialAccount;
import com.nest.model.Transaction;
import com.nest.repository.CategoryRepository;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransactionRepository;
import com.nest.service.TransactionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nest.repository.TransactionSpecifications.amountAtLeast;
import static com.nest.repository.TransactionSpecifications.amountAtMost;
import static com.nest.repository.TransactionSpecifications.descriptionContains;
import static com.nest.repository.TransactionSpecifications.inAccount;
import static com.nest.repository.TransactionSpecifications.inCategory;
import static com.nest.repository.TransactionSpecifications.ofType;
import static com.nest.repository.TransactionSpecifications.onOrAfter;
import static com.nest.repository.TransactionSpecifications.onOrBefore;
import static com.nest.repository.TransactionSpecifications.ownedBy;

/**
 * Coordinates lookups and persistence. The rules themselves (positive amount, currency scale,
 * category compatibility, open account) live in {@link Transaction}. This service resolves
 * <em>which</em> account and category the caller may use, scoped to the caller.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final FinancialAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  FinancialAccountRepository accountRepository,
                                  CategoryRepository categoryRepository,
                                  TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    @Transactional
    public TransactionResponse create(long userId, TransactionRequest request) {
        FinancialAccount account = findOwnedAccount(userId, request.accountId());
        requireMatchingCurrency(account, request.currency());
        Category category = findVisibleCategory(userId, request.categoryId());

        Transaction transaction = new Transaction(account, category, request.type(), request.amount(),
                request.description(), request.transactionDate());
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(long userId, TransactionFilter filter, Pageable pageable) {
        Specification<Transaction> spec = Specification.allOf(
                ownedBy(userId),
                inAccount(filter.accountId()),
                inCategory(filter.categoryId()),
                ofType(filter.type()),
                onOrAfter(filter.from()),
                onOrBefore(filter.to()),
                amountAtLeast(filter.minAmount()),
                amountAtMost(filter.maxAmount()),
                descriptionContains(filter.q()));
        return PageResponse.from(transactionRepository.findAll(spec, pageable), transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse get(long userId, long transactionId) {
        return transactionMapper.toResponse(findOwnedTransaction(userId, transactionId));
    }

    @Override
    @Transactional
    public TransactionResponse update(long userId, long transactionId, TransactionRequest request) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        FinancialAccount account = findOwnedAccount(userId, request.accountId());
        requireMatchingCurrency(account, request.currency());
        Category category = findVisibleCategory(userId, request.categoryId());

        transaction.revise(account, category, request.type(), request.amount(),
                request.description(), request.transactionDate());
        return transactionMapper.toResponse(transactionRepository.saveAndFlush(transaction));
    }

    @Override
    @Transactional
    public void delete(long userId, long transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        transaction.requireRemovable();
        transactionRepository.delete(transaction);
    }

    private Transaction findOwnedTransaction(long userId, long transactionId) {
        return transactionRepository.findByIdAndAccountUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));
    }

    private FinancialAccount findOwnedAccount(long userId, long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    private Category findVisibleCategory(long userId, long categoryId) {
        return categoryRepository.findVisible(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    /** The currency is never taken from the client. If one is supplied, it must agree with the account. */
    private static void requireMatchingCurrency(FinancialAccount account, String claimedCurrency) {
        if (claimedCurrency != null && !claimedCurrency.equals(account.getCurrency())) {
            throw new BusinessRuleException("Account '%s' is in %s, not %s."
                    .formatted(account.getName(), account.getCurrency(), claimedCurrency));
        }
    }
}
