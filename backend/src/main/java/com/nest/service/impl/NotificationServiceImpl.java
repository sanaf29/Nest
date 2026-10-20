package com.nest.service.impl;

import com.nest.dto.response.NotificationResponse;
import com.nest.dto.response.PageResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.Notification;
import com.nest.repository.NotificationRepository;
import com.nest.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationServiceImpl(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(long userId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        var result = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponse.from(result, NotificationServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(long userId, long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.markRead(clock.instant());
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(long userId) {
        return notificationRepository.markAllRead(userId, clock.instant());
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getCategory(), n.getTitle(), n.getMessage(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }
}
