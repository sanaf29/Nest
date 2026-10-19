package com.nest.service.impl;

import com.nest.dto.request.CreateAccountRequest;
import com.nest.dto.request.UpdateAccountRequest;
import com.nest.dto.response.AccountResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.AccountMapper;
import com.nest.model.AccountType;
import com.nest.model.FinancialAccount;
import com.nest.model.User;
import com.nest.model.TransactionType;
import com.nest.repository.AccountTypeTotal;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransactionRepository;
import com.nest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long ACCOUNT_ID = 10L;

    @Mock
    private FinancialAccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private FinancialAccountServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new FinancialAccountServiceImpl(accountRepository, userRepository, transactionRepository, new AccountMapper());
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "EUR");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
    }

    private FinancialAccount existingAccount() {
        FinancialAccount account = new FinancialAccount(owner, "Chequing", AccountType.CHECKING, "EUR", new BigDecimal("100"));
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    @Nested
    class Create {

        @Test
        void defaultsCurrencyToTheUsersPreferredCurrency() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(accountRepository.save(any(FinancialAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            AccountResponse response = service.create(USER_ID,
                    new CreateAccountRequest("Wallet", AccountType.CASH, null, new BigDecimal("40")));

            assertThat(response.currency()).isEqualTo("EUR");
            assertThat(response.currentBalance()).isEqualByComparingTo("40.00");
            assertThat(response.active()).isTrue();
        }

        @Test
        void explicitCurrencyWins() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(accountRepository.save(any(FinancialAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            service.create(USER_ID, new CreateAccountRequest("US Card", AccountType.CREDIT_CARD, "USD", BigDecimal.ZERO));

            ArgumentCaptor<FinancialAccount> saved = ArgumentCaptor.forClass(FinancialAccount.class);
            verify(accountRepository).save(saved.capture());
            assertThat(saved.getValue().getCurrency()).isEqualTo("USD");
            assertThat(saved.getValue().isOwnedBy(USER_ID)).isTrue();
        }

        @Test
        void rejectsDuplicateOpenAccountName() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Wallet")).thenReturn(true);

            assertThatThrownBy(() -> service.create(USER_ID,
                    new CreateAccountRequest(" Wallet ", AccountType.CASH, null, BigDecimal.ZERO)))
                    .isInstanceOf(ConflictException.class);
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class Read {

        @Test
        void accountOfAnotherUserIsNotFound() {
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(2L, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ACCOUNT_ID));
        }

        @Test
        void listHidesClosedAccountsUnlessAsked() {
            when(accountRepository.findByUserIdAndActiveTrueOrderByNameAsc(USER_ID)).thenReturn(List.of(existingAccount()));

            assertThat(service.list(USER_ID, false)).hasSize(1);
            verify(accountRepository, never()).findByUserIdOrderByNameAsc(any());
        }

        @Test
        void listIncludesClosedAccountsOnRequest() {
            FinancialAccount closed = existingAccount();
            closed.deactivate();
            when(accountRepository.findByUserIdOrderByNameAsc(USER_ID)).thenReturn(List.of(existingAccount(), closed));

            assertThat(service.list(USER_ID, true)).extracting(AccountResponse::active).containsExactly(true, false);
        }
    }

    @Nested
    class Update {

        @Test
        void renamesAndCorrectsOpeningBalance() {
            FinancialAccount account = existingAccount();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.saveAndFlush(account)).thenReturn(account);

            AccountResponse response = service.update(USER_ID, ACCOUNT_ID,
                    new UpdateAccountRequest("Main", new BigDecimal("80.5")));

            assertThat(response.name()).isEqualTo("Main");
            assertThat(response.openingBalance()).isEqualByComparingTo("80.50");
            assertThat(response.currentBalance()).isEqualByComparingTo("80.50");
        }

        @Test
        void keepingTheSameNameIsNotAConflict() {
            FinancialAccount account = existingAccount();
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
            when(accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(USER_ID, "Chequing", ACCOUNT_ID))
                    .thenReturn(false);
            when(accountRepository.saveAndFlush(account)).thenReturn(account);

            service.update(USER_ID, ACCOUNT_ID, new UpdateAccountRequest("Chequing", new BigDecimal("1")));

            verify(accountRepository).existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(USER_ID, "Chequing", ACCOUNT_ID);
        }

        @Test
        void cannotUpdateAnotherUsersAccount() {
            when(accountRepository.findByIdAndUserId(ACCOUNT_ID, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(2L, ACCOUNT_ID, new UpdateAccountRequest("Hacked", BigDecimal.ZERO)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(accountRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    void deactivateClosesTheOwnedAccount() {
        FinancialAccount account = existingAccount();
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));

        service.deactivate(USER_ID, ACCOUNT_ID);

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void listComputesEveryBalanceFromASingleAggregateQuery() {
        FinancialAccount chequing = existingAccount();
        FinancialAccount savings = new FinancialAccount(owner, "Savings", AccountType.SAVINGS, "EUR", new BigDecimal("1000"));
        ReflectionTestUtils.setField(savings, "id", 11L);
        when(accountRepository.findByUserIdAndActiveTrueOrderByNameAsc(USER_ID)).thenReturn(List.of(chequing, savings));
        when(transactionRepository.sumByAccountAndType(List.of(ACCOUNT_ID, 11L))).thenReturn(List.of(
                new AccountTypeTotal(ACCOUNT_ID, TransactionType.INCOME, new BigDecimal("50.0000")),
                new AccountTypeTotal(ACCOUNT_ID, TransactionType.EXPENSE, new BigDecimal("20.2500")),
                new AccountTypeTotal(11L, TransactionType.EXPENSE, new BigDecimal("100.0000"))));

        List<AccountResponse> accounts = service.list(USER_ID, false);

        assertThat(accounts).extracting(AccountResponse::currentBalance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("129.75"), new BigDecimal("900.00"));
        verify(transactionRepository).sumByAccountAndType(List.of(ACCOUNT_ID, 11L));
    }
}
