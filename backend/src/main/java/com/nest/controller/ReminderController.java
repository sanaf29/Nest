package com.nest.controller;

import com.nest.dto.request.ReminderRequest;
import com.nest.dto.response.ReminderResponse;
import com.nest.model.ReminderStatus;
import com.nest.security.CurrentUserProvider;
import com.nest.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUserProvider currentUser;

    public ReminderController(ReminderService reminderService, CurrentUserProvider currentUser) {
        this.reminderService = reminderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Create a reminder",
            description = "Bill, subscription, goal deadline, parking, or custom reminders fire once (in-app) at dueAt minus offset. "
                    + "Budget alerts notify once a month when spending reaches the threshold.")
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody ReminderRequest request) {
        ReminderResponse created = reminderService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/reminders/" + created.id())).body(created);
    }

    @GetMapping
    public List<ReminderResponse> list(@RequestParam(required = false) ReminderStatus status) {
        return reminderService.list(currentUser.currentUserId(), status);
    }

    @GetMapping("/{id}")
    public ReminderResponse get(@PathVariable long id) {
        return reminderService.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit and reschedule a reminder")
    public ReminderResponse update(@PathVariable long id, @Valid @RequestBody ReminderRequest request) {
        return reminderService.update(currentUser.currentUserId(), id, request);
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Turn a reminder off without deleting it")
    public ReminderResponse dismiss(@PathVariable long id) {
        return reminderService.dismiss(currentUser.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        reminderService.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
