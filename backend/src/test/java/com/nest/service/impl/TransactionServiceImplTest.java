package com.nest.service.impl;

import com.nest.dto.request.TransactionRequest;
import com.nest.dto.response.TransactionResponse;
import com.nest.exception.BusinessRuleException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.TransactionMapper;
import com.nest.model.AccountType;
import com.nest.model.Category;
import com.nest.model.CategoryFixtures;
import com.nest.model.CategoryType;
import com.nest.model.FinancialAccount;
import com.nest.model.Transaction;
import com.nest.model.TransactionType;
import com.nest.model.User;
import com.nest.repository.CategoryRepository;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 18);

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FinancialAccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;

    private TransactionServiceImpl service;
    private FinancialAccount account;
    private Category groceries;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(transactionRepository, accountRepository, categoryRepository, new TransactionMapper());
        User owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
        account = new FinancialAccount(owner, "Chequing", AccountType.CHECKING, "CAD", BigDecimal.ZERO);
        ReflectionTestUtils.setField(account, "id", 10L);
        groceries = CategoryFixtures.defaultCategory(100L, "Groceries", CategoryType.EXPENSE);
    }

    private static TransactionRequest request(String currency) {
        return new TransactionRequest(10L, 100L, TransactionType.EXPENSE, new BigDecimal("42.50"), currency, "Food", DATE);
    }

    @Test
    void createsTransactionOnTheUsersAccount() {
        when(accountRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findVisible(100L, USER_ID)).thenReturn(Optional.of(groceries));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(USER_ID, request(null));

        assertThat(response.accountId()).isEqualTo(10L);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.currency()).isEqualTo("CAD");
        assertThat(response.amount()).isEqualByComparingTo("42.50");
    }

    @Test
    void accountOfAnotherUserIsNotFound() {
        when(accountRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(2L, request(null))).isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void invisibleCategoryIsNotFound() {
        when(accountRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findVisible(100L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, request(null))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void currencyClaimMustMatchTheAccount() {
        when(accountRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.create(USER_ID, request("USD")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CAD");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteRemovesOwnedTransaction() {
        Transaction transaction = new Transaction(account, groceries, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE);
        when(transactionRepository.findByIdAndAccountUserId(5L, USER_ID)).thenReturn(Optional.of(transaction));

        service.delete(USER_ID, 5L);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteOnClosedAccountIsRejected() {
        Transaction transaction = new Transaction(account, groceries, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE);
        account.deactivate();
        when(transactionRepository.findByIdAndAccountUserId(5L, USER_ID)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> service.delete(USER_ID, 5L)).isInstanceOf(BusinessRuleException.class);
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void transactionOfAnotherUserIsNotFound() {
        when(transactionRepository.findByIdAndAccountUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, 5L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(2L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
