package com.nest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Generates due recurring transactions on a schedule (hourly by default; see {@code nest.recurring.cron}). */
@Component
@ConditionalOnProperty(name = "nest.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final RecurringTransactionProcessor processor;
    private final Clock clock;

    public RecurringTransactionScheduler(RecurringTransactionProcessor processor, Clock clock) {
        this.processor = processor;
        this.clock = clock;
    }

    @Scheduled(cron = "${nest.recurring.cron:0 15 * * * *}", zone = "UTC")
    public void run() {
        RecurringTransactionProcessor.Result result = processor.processDue(LocalDate.now(clock));
        if (result.generated() > 0 || result.paused() > 0) {
            log.info("Recurring run: {} generated, {} paused", result.generated(), result.paused());
        }
    }
}
