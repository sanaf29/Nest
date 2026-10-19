package com.nest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Bound from {@code nest.cors.*}: origins (the SPA) allowed to call the API from a browser. */
@ConfigurationProperties(prefix = "nest.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
