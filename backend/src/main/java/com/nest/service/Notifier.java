package com.nest.service;

import com.nest.model.Notification;
import com.nest.model.NotificationCategory;
import com.nest.model.User;
import com.nest.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/**
 * Creates in-app notifications idempotently. The same {@code dedupKey} for the same user is only
 * ever stored once; the unique (user_id, dedup_key) constraint backs this up under races.
 * Runs in the caller's transaction, so a notification commits or rolls back together with the
 * change that caused it.
 */
@Component
public class Notifier {

    private final NotificationRepository notificationRepository;

    public Notifier(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** @return true if a new notification was created, false if this event was already notified */
    public boolean notifyOnce(User user, NotificationCategory category, String title, String message, String link, String dedupKey) {
        if (notificationRepository.existsByUserIdAndDedupKey(user.getId(), dedupKey)) {
            return false;
        }
        notificationRepository.save(new Notification(user, category, title, message, link, dedupKey));
        return true;
    }
}
