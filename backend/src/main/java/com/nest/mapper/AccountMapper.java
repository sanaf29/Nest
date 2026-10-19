package com.nest.mapper;

import com.nest.dto.response.AccountResponse;
import com.nest.model.FinancialAccount;
import com.nest.model.Money;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    /** The balance is passed in because computing it needs a repository query, which a mapper must not run. */
    public AccountResponse toResponse(FinancialAccount account, Money currentBalance) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getAccountType(),
                account.getCurrency(),
                account.openingBalance().amount(),
                currentBalance.amount(),
                account.isActive(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
