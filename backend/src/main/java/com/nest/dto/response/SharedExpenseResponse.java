package com.nest.dto.response;

import com.nest.model.SplitMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SharedExpenseResponse(
        Long id,
        String description,
        BigDecimal totalAmount,
        String currency,
        LocalDate expenseDate,
        SplitMethod splitMethod,
        boolean settled,
        @Schema(description = "Still owed to you on this expense (you paid)") BigDecimal owedToYou,
        @Schema(description = "You still owe the payer") BigDecimal youOwe,
        List<Participant> participants,
        Instant createdAt
) {
    public record Participant(Long id, String name, String email, boolean me, boolean paid, BigDecimal share,
                              @Schema(description = "Owes the payer this share (false for the payer and zero shares)") boolean owes,
                              boolean settled, Instant settledAt) {
    }
}
