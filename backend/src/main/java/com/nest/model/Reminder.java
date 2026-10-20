package com.nest.model;

import com.nest.exception.BusinessRuleException;
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
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Something the user wants to be told about.
 *
 * <ul>
 *   <li><b>Timed</b> reminders (bill, subscription, goal deadline, parking, custom) fire once at
 *       {@code dueAt − offset} and become SENT.</li>
 *   <li><b>Budget thresholds</b> stay SCHEDULED and notify at most once per month when spending
 *       (overall, or in one category) reaches {@code thresholdPercent} of the budget.</li>
 * </ul>
 * Parking reminders require a location; goal-deadline reminders belong to a goal.
 */
@Entity
@Table(name = "reminders")
public class Reminder extends AuditableEntity {

    public static final int MAX_OFFSET_MINUTES = 30 * 24 * 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30, updatable = false)
    private ReminderType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "offset_minutes", nullable = false)
    private int offsetMinutes;

    /** Computed by the database (due_at − offset); read-only here, used for querying. */
    @Column(name = "remind_at", insertable = false, updatable = false)
    private Instant remindAtColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ReminderStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "location", length = 200)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_goal_id")
    private SavingsGoal savingsGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "threshold_percent")
    private Integer thresholdPercent;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected Reminder() {
    }

    /** Everything a reminder can be configured with; which fields are required depends on the type. */
    public record Details(String title, String notes, Instant dueAt, int offsetMinutes, String location,
                          SavingsGoal savingsGoal, Category category, Integer thresholdPercent) {
    }

    public Reminder(User owner, ReminderType type, Details details, Instant now) {
        this.user = Objects.requireNonNull(owner, "owner");
        this.type = Objects.requireNonNull(type, "type");
        apply(details, now);
        this.status = ReminderStatus.SCHEDULED;
    }

    /** Re-validates and reschedules: an edited reminder will fire again at its new time. */
    public void update(Details details, Instant now) {
        apply(details, now);
        this.status = ReminderStatus.SCHEDULED;
        this.sentAt = null;
    }

    private void apply(Details d, Instant now) {
        if (d.title() == null || d.title().isBlank()) {
            throw new BusinessRuleException("Give the reminder a title.");
        }
        if (d.title().strip().length() > 150) {
            throw new BusinessRuleException("Title must be at most 150 characters.");
        }
        if (d.offsetMinutes() < 0 || d.offsetMinutes() > MAX_OFFSET_MINUTES) {
            throw new BusinessRuleException("A reminder can be sent from 0 minutes up to 30 days before.");
        }
        if (type.isTimed()) {
            if (d.dueAt() == null) {
                throw new BusinessRuleException("%s reminders need a date and time.".formatted(type.label()));
            }
            if (d.dueAt().isBefore(now)) {
                throw new BusinessRuleException("The date and time must be in the future.");
            }
            if (d.thresholdPercent() != null) {
                throw new BusinessRuleException("Only budget alerts have a threshold.");
            }
        } else {
            if (d.thresholdPercent() == null || d.thresholdPercent() < 1 || d.thresholdPercent() > 200) {
                throw new BusinessRuleException("Choose a budget threshold between 1 and 200 percent.");
            }
            if (d.dueAt() != null) {
                throw new BusinessRuleException("Budget alerts are checked continuously and have no date.");
            }
            if (d.category() != null && d.category().getCategoryType() != CategoryType.EXPENSE) {
                throw new BusinessRuleException("Budget alerts watch expense categories.");
            }
        }
        if (type == ReminderType.PARKING && (d.location() == null || d.location().isBlank())) {
            throw new BusinessRuleException("Add where you parked, e.g. 'Level 2, spot B14'.");
        }
        if (type == ReminderType.SAVINGS_GOAL_DEADLINE && d.savingsGoal() == null) {
            throw new BusinessRuleException("Choose the goal this deadline belongs to.");
        }
        this.title = d.title().strip();
        this.notes = d.notes() == null || d.notes().isBlank() ? null : d.notes().strip();
        this.dueAt = d.dueAt();
        this.offsetMinutes = d.offsetMinutes();
        this.location = d.location() == null || d.location().isBlank() ? null : d.location().strip();
        this.savingsGoal = d.savingsGoal();
        this.category = d.category();
        this.thresholdPercent = d.thresholdPercent();
    }

    /** When the notification is due; {@code null} for budget thresholds. */
    public Instant remindAt() {
        return dueAt == null ? null : dueAt.minus(Duration.ofMinutes(offsetMinutes));
    }

    public boolean isDue(Instant now) {
        return status == ReminderStatus.SCHEDULED && type.isTimed() && !remindAt().isAfter(now);
    }

    public void markSent(Instant now) {
        status = ReminderStatus.SENT;
        sentAt = now;
    }

    public void dismiss() {
        status = ReminderStatus.DISMISSED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ReminderType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public int getOffsetMinutes() {
        return offsetMinutes;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getLocation() {
        return location;
    }

    public SavingsGoal getSavingsGoal() {
        return savingsGoal;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getThresholdPercent() {
        return thresholdPercent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Reminder that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Reminder.class);
    }
}
