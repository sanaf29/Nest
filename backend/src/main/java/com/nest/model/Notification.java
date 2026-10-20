package com.nest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * An in-app message. {@code dedupKey} identifies the event that caused it (e.g.
 * {@code reminder:42} or {@code budget:7:2026-10}) and is unique per user, which makes every
 * notification source idempotent.
 */
@Entity
@Table(name = "notifications")
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, updatable = false)
    private NotificationCategory category;

    @Column(name = "title", nullable = false, length = 150, updatable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "link", length = 200, updatable = false)
    private String link;

    @Column(name = "dedup_key", nullable = false, length = 120, updatable = false)
    private String dedupKey;

    @Column(name = "read_at")
    private Instant readAt;

    /** Required by JPA only. */
    protected Notification() {
    }

    public Notification(User user, NotificationCategory category, String title, String message, String link, String dedupKey) {
        this.user = Objects.requireNonNull(user, "user");
        this.category = Objects.requireNonNull(category, "category");
        this.title = truncate(Objects.requireNonNull(title, "title"), 150);
        this.message = truncate(Objects.requireNonNull(message, "message"), 500);
        this.link = link;
        this.dedupKey = Objects.requireNonNull(dedupKey, "dedupKey");
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    public Long getId() {
        return id;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Notification that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Notification.class);
    }
}
