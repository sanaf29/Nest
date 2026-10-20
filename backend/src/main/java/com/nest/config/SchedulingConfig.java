package com.nest.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Background jobs (recurring transactions, reminders). Turned off with
 * {@code nest.scheduling.enabled=false}; the test profile does this and calls the services
 * directly with a fixed date, so tests never depend on wall-clock timing.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "nest.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
