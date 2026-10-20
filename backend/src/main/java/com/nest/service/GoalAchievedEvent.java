package com.nest.service;

/** Published when a contribution takes a savings goal to its target. */
public record GoalAchievedEvent(long userId, long goalId, String goalName, String target) {
}
