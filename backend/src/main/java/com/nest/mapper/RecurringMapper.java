package com.nest.mapper;

import com.nest.dto.response.RecurringResponse;
import com.nest.model.RecurringTransaction;
import org.springframework.stereotype.Component;

@Component
public class RecurringMapper {

    public RecurringResponse toResponse(RecurringTransaction rule) {
        return new RecurringResponse(
                rule.getId(),
                rule.getAccount().getId(),
                rule.getAccount().getName(),
                rule.getCategory().getId(),
                rule.getCategory().getName(),
                rule.getType(),
                rule.amount().amount(),
                rule.amount().currency(),
                rule.getDescription(),
                rule.getFrequency(),
                rule.getStartDate(),
                rule.getEndDate(),
                rule.getNextOccurrence(),
                rule.upcoming(3),
                rule.getStatus(),
                rule.getPauseReason(),
                rule.getRemindDaysBefore(),
                rule.getCreatedAt());
    }
}
