package com.nest.service;

import com.nest.dto.request.ReminderRequest;
import com.nest.dto.response.ReminderResponse;
import com.nest.model.ReminderStatus;

import java.util.List;

public interface ReminderService {

    ReminderResponse create(long userId, ReminderRequest request);

    /** @param status {@code null} for all */
    List<ReminderResponse> list(long userId, ReminderStatus status);

    ReminderResponse get(long userId, long id);

    /** Replaces the reminder's details and schedules it again. */
    ReminderResponse update(long userId, long id, ReminderRequest request);

    ReminderResponse dismiss(long userId, long id);

    void delete(long userId, long id);
}
