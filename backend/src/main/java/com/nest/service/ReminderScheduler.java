package com.nest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Dispatches reminders and alerts every minute by default ({@code nest.reminders.cron}). */
@Component
@ConditionalOnProperty(name = "nest.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderDispatcher dispatcher;
    private final Clock clock;

    public ReminderScheduler(ReminderDispatcher dispatcher, Clock clock) {
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "${nest.reminders.cron:0 * * * * *}", zone = "UTC")
    public void run() {
        ReminderDispatcher.Result result = dispatcher.runAll(clock.instant());
        if (result.total() > 0) {
            log.info("Reminder run: {} notifications", result.total());
        }
    }
}
