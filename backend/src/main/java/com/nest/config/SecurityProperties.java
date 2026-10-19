package com.nest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code nest.security.*}. */
@ConfigurationProperties(prefix = "nest.security")
public record SecurityProperties(int bcryptStrength) {
}
