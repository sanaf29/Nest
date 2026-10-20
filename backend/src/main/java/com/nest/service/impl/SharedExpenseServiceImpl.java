package com.nest.service.impl;

import com.nest.dto.request.SharedExpenseRequest;
import com.nest.dto.response.SharedBalanceResponse;
import com.nest.dto.response.SharedExpenseResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.ExpenseParticipant;
import com.nest.model.Money;
import com.nest.model.SharedExpense;
import com.nest.model.User;
import com.nest.repository.SharedExpenseRepository;
import com.nest.repository.UserRepository;
import com.nest.service.SharedExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SharedExpenseServiceImpl implements SharedExpenseService {

    private final SharedExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public SharedExpenseServiceImpl(SharedExpenseRepository expenseRepository, UserRepository userRepository, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SharedExpenseResponse create(long userId, SharedExpenseRequest request) {
        User owner = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        String currency = request.currency() != null ? request.currency() : owner.getPreferredCurrency();
        SharedExpense expense = new SharedExpense(owner, request.description(), request.totalAmount(), currency,
                request.expenseDate(), request.splitMethod(),
                request.participants().stream()
                        .map(p -> new SharedExpense.ParticipantSpec(p.name(), p.email(), p.me(), p.paid(), p.value()))
                        .toList());
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharedExpenseResponse> list(long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDescIdDesc(userId).stream()
                .map(SharedExpenseServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SharedExpenseResponse get(long userId, long id) {
        return toResponse(findOwned(userId, id));
    }

    @Override
    @Transactional
    public void delete(long userId, long id) {
        expenseRepository.delete(findOwned(userId, id));
    }

    @Override
    @Transactional
    public SharedExpenseResponse settle(long userId, long id, long participantId) {
        SharedExpense expense = findOwned(userId, id);
        expense.settle(participantId, clock.instant());
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Override
    @Transactional
    public SharedExpenseResponse unsettle(long userId, long id, long participantId) {
        SharedExpense expense = findOwned(userId, id);
        expense.unsettle(participantId);
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    /**
     * From the owner's point of view: when I paid, each unsettled share is owed to me (+); when
     * someone else paid, my unsettled share is owed to them (−). People are matched by email
     * when given, otherwise by name (case-insensitive).
     */
    @Override
    @Transactional(readOnly = true)
    public List<SharedBalanceResponse> balances(long userId) {
        Map<String, Balance> balances = new LinkedHashMap<>();
        for (SharedExpense expense : expenseRepository.findByUserIdOrderByExpenseDateDescIdDesc(userId)) {
            ExpenseParticipant payer = expense.payer();
            if (payer.isMe()) {
                for (ExpenseParticipant p : expense.getParticipants()) {
                    if (p.owesPayer() && !p.isSettled()) {
                        balances.computeIfAbsent(key(p, expense.getCurrency()), k -> new Balance(p.getName(), Money.zero(expense.getCurrency())))
                                .add(p.share());
                    }
                }
            } else if (expense.me().owesPayer() && !expense.me().isSettled()) {
                balances.computeIfAbsent(key(payer, expense.getCurrency()), k -> new Balance(payer.getName(), Money.zero(expense.getCurrency())))
                        .subtract(expense.me().share());
            }
        }
        return balances.values().stream()
                .filter(b -> b.amount.amount().signum() != 0)
                .sorted(Comparator.comparing((Balance b) -> b.amount.amount().abs()).reversed())
                .map(b -> new SharedBalanceResponse(b.person, b.amount.currency(), b.amount.amount()))
                .toList();
    }

    private static String key(ExpenseParticipant p, String currency) {
        String who = p.getEmail() != null ? p.getEmail() : p.getName();
        return who.toLowerCase(Locale.ROOT) + "|" + currency;
    }

    private static final class Balance {
        private final String person;
        private Money amount;

        Balance(String person, Money amount) {
            this.person = person;
            this.amount = amount;
        }

        void add(Money m) {
            amount = amount.plus(m);
        }

        void subtract(Money m) {
            amount = amount.minus(m);
        }
    }

    private SharedExpense findOwned(long userId, long id) {
        return expenseRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Shared expense", id));
    }

    private static SharedExpenseResponse toResponse(SharedExpense e) {
        return new SharedExpenseResponse(e.getId(), e.getDescription(), e.total().amount(), e.getCurrency(), e.getExpenseDate(),
                e.getSplitMethod(), e.isSettled(), e.owedToMe().amount(), e.iOwe().amount(),
                e.getParticipants().stream()
                        .map(p -> new SharedExpenseResponse.Participant(p.getId(), p.getName(), p.getEmail(), p.isMe(), p.isPayer(),
                                p.share().amount(), p.owesPayer(), p.isSettled(), p.getSettledAt()))
                        .toList(),
                e.getCreatedAt());
    }
}
