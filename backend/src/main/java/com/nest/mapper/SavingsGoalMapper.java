package com.nest.mapper;

import com.nest.dto.response.SavingsGoalResponse;
import com.nest.model.SavingsGoal;
import org.springframework.stereotype.Component;

@Component
public class SavingsGoalMapper {

    public SavingsGoalResponse toResponse(SavingsGoal goal) {
        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getCurrency(),
                goal.target().amount(),
                goal.savedAmount().amount(),
                goal.remainingAmount().amount(),
                goal.progressPercent(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getAchievedAt(),
                goal.getContributions().stream()
                        .map(c -> new SavingsGoalResponse.Contribution(c.getId(), c.amount().amount(),
                                c.getContributionDate(), c.getNote(), c.getCreatedAt()))
                        .toList(),
                goal.getCreatedAt());
    }
}
