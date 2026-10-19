package com.nest.service.impl;

import com.nest.dto.request.TransferRequest;
import com.nest.dto.response.TransferResponse;
import com.nest.exception.BusinessRuleException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.TransferMapper;
import com.nest.model.AccountType;
import com.nest.model.FinancialAccount;
import com.nest.model.Transfer;
import com.nest.model.User;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransferRepository;
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
class TransferServiceImplTest {

    private static final long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 19);

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private FinancialAccountRepository accountRepository;

    private TransferServiceImpl service;
    private FinancialAccount chequing;
    private FinancialAccount savings;

    @BeforeEach
    void setUp() {
        service = new TransferServiceImpl(transferRepository, accountRepository, new TransferMapper());
        User owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
        chequing = new FinancialAccount(owner, "Chequing", AccountType.CHECKING, "CAD", BigDecimal.ZERO);
        ReflectionTestUtils.setField(chequing, "id", 10L);
        savings = new FinancialAccount(owner, "Savings", AccountType.SAVINGS, "CAD", BigDecimal.ZERO);
        ReflectionTestUtils.setField(savings, "id", 11L);
    }

    @Test
    void createsTransferBetweenTheUsersAccounts() {
        when(accountRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(chequing));
        when(accountRepository.findByIdAndUserId(11L, USER_ID)).thenReturn(Optional.of(savings));
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = service.create(USER_ID, new TransferRequest(10L, 11L, new BigDecimal("500"), null, DATE));

        assertThat(response.sourceAccountName()).isEqualTo("Chequing");
        assertThat(response.destinationAccountName()).isEqualTo("Savings");
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.currency()).isEqualTo("CAD");
    }

    @Test
    void accountOfAnotherUserIsNotFoundAndNothingIsSaved() {
        when(accountRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(chequing));
        when(accountRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, new TransferRequest(10L, 99L, BigDecimal.TEN, null, DATE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteIsRefusedWhenAnAccountIsClosed() {
        Transfer transfer = Transfer.between(chequing, savings, BigDecimal.TEN, DATE, null);
        savings.deactivate();
        when(transferRepository.findOwned(7L, USER_ID)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> service.delete(USER_ID, 7L)).isInstanceOf(BusinessRuleException.class);
        verify(transferRepository, never()).delete(any(Transfer.class));
    }

    @Test
    void deleteRemovesTheWholeTransfer() {
        Transfer transfer = Transfer.between(chequing, savings, BigDecimal.TEN, DATE, null);
        when(transferRepository.findOwned(7L, USER_ID)).thenReturn(Optional.of(transfer));

        service.delete(USER_ID, 7L);

        verify(transferRepository).delete(transfer);
    }

    @Test
    void transferOfAnotherUserIsNotFound() {
        when(transferRepository.findOwned(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, 7L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
