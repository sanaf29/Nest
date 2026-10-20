package com.nest.service;

import com.nest.dto.response.NotificationResponse;
import com.nest.dto.response.PageResponse;

public interface NotificationService {

    PageResponse<NotificationResponse> list(long userId, boolean unreadOnly, int page, int size);

    long unreadCount(long userId);

    NotificationResponse markRead(long userId, long id);

    int markAllRead(long userId);
}
