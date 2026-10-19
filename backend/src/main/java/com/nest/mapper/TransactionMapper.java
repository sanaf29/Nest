package com.nest.mapper;

import com.nest.dto.response.TransactionResponse;
import com.nest.model.Category;
import com.nest.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        Category category = transaction.getCategory();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                transaction.getType(),
                transaction.money().amount(),
                transaction.currency(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.isTransferLeg() ? transaction.getTransfer().getId() : null,
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
