package com.nest.controller;

import com.nest.dto.request.ContributionRequest;
import com.nest.dto.request.CreateGoalRequest;
import com.nest.dto.request.UpdateGoalRequest;
import com.nest.dto.response.SavingsGoalResponse;
import com.nest.model.GoalStatus;
import com.nest.security.CurrentUserProvider;
import com.nest.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/savings-goals")
@Tag(name = "Savings Goals")
public class SavingsGoalController {

    private final SavingsGoalService goalService;
    private final CurrentUserProvider currentUser;

    public SavingsGoalController(SavingsGoalService goalService, CurrentUserProvider currentUser) {
        this.goalService = goalService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Create a savings goal", description = "201; 409 duplicate name; 422 past target date or invalid amount.")
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        SavingsGoalResponse created = goalService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/savings-goals/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List my savings goals", description = "Optional status filter: ACTIVE or ACHIEVED.")
    public List<SavingsGoalResponse> list(@RequestParam(required = false) GoalStatus status) {
        return goalService.list(currentUser.currentUserId(), status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a goal with its contributions")
    public SavingsGoalResponse get(@PathVariable long id) {
        return goalService.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a goal's name, target, or deadline", description = "The status is re-evaluated.")
    public SavingsGoalResponse update(@PathVariable long id, @Valid @RequestBody UpdateGoalRequest request) {
        return goalService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a goal and its contributions")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        goalService.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contributions")
    @Operation(summary = "Record a contribution",
            description = "Tracks money set aside for the goal (no money moves between accounts). "
                    + "201 when recorded; 200 when the idempotencyKey was already used (nothing changes).")
    public ResponseEntity<SavingsGoalResponse> contribute(@PathVariable long id, @Valid @RequestBody ContributionRequest request) {
        SavingsGoalService.ContributionResult result = goalService.contribute(currentUser.currentUserId(), id, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.goal());
    }

    @DeleteMapping("/{id}/contributions/{contributionId}")
    @Operation(summary = "Remove a contribution", description = "Returns the updated goal; the status is re-evaluated.")
    public SavingsGoalResponse removeContribution(@PathVariable long id, @PathVariable long contributionId) {
        return goalService.removeContribution(currentUser.currentUserId(), id, contributionId);
    }
}
