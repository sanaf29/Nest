package com.nest.model;

/** Kinds of reminder. Each knows which fields it needs and how its notification is phrased. */
public enum ReminderType {
    BILL_PAYMENT("Bill due"),
    BUDGET_THRESHOLD("Budget alert"),
    SUBSCRIPTION_RENEWAL("Subscription renews"),
    SAVINGS_GOAL_DEADLINE("Goal deadline"),
    PARKING("Parking expires"),
    CUSTOM("Reminder");

    private final String label;

    ReminderType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Budget thresholds are checked continuously; every other type fires once at a moment in time. */
    public boolean isTimed() {
        return this != BUDGET_THRESHOLD;
    }
}
