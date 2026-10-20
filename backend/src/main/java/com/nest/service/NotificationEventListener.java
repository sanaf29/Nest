package com.nest.service;

import com.nest.model.NotificationCategory;
import com.nest.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into notifications. Listeners run synchronously in the publisher's
 * transaction, so the notification and the change that caused it commit together.
 */
@Component
public class NotificationEventListener {

    private final Notifier notifier;
    private final UserRepository userRepository;

    public NotificationEventListener(Notifier notifier, UserRepository userRepository) {
        this.notifier = notifier;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onRecurringPaused(RecurringPausedEvent event) {
        notifier.notifyOnce(userRepository.getReferenceById(event.userId()), NotificationCategory.RECURRING,
                "'%s' was paused".formatted(event.description()), event.reason(), "/recurring",
                "recurring-paused:%d:%s".formatted(event.recurringTransactionId(), event.date()));
    }

    @EventListener
    public void onGoalAchieved(GoalAchievedEvent event) {
        notifier.notifyOnce(userRepository.getReferenceById(event.userId()), NotificationCategory.GOAL,
                "Goal achieved: %s 🎉".formatted(event.goalName()),
                "You've saved %s for '%s'. Well done!".formatted(event.target(), event.goalName()), "/goals",
                "goal-achieved:%d".formatted(event.goalId()));
    }
}
