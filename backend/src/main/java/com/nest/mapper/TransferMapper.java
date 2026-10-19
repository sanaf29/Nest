package com.nest.mapper;

import com.nest.dto.response.TransferResponse;
import com.nest.model.Transfer;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.source().getId(),
                transfer.source().getName(),
                transfer.destination().getId(),
                transfer.destination().getName(),
                transfer.amount().amount(),
                transfer.amount().currency(),
                transfer.date(),
                transfer.outgoing().getId(),
                transfer.incoming().getId(),
                transfer.getCreatedAt());
    }
}
