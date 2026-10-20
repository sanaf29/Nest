package com.nest.dto.response;

import com.nest.model.NotificationCategory;

import java.time.Instant;

public record NotificationResponse(Long id, NotificationCategory category, String title, String message, String link,
                                   boolean read, Instant createdAt) {
}
