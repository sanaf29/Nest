package com.nest.service.impl;

import com.nest.dto.request.ContributionRequest;
import com.nest.dto.request.CreateGoalRequest;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.SavingsGoalMapper;
import com.nest.model.SavingsGoal;
import com.nest.model.User;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.UserRepository;
import com.nest.service.SavingsGoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceImplTest {

    private static final long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SavingsGoalRepository goalRepository;
    @Mock
    private UserRepository userRepository;

    private SavingsGoalServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new SavingsGoalServiceImpl(goalRepository, userRepository, new SavingsGoalMapper(), CLOCK, event -> { });
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "EUR");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
    }

    private SavingsGoal goal() {
        return new SavingsGoal(owner, "Trip", "EUR", new BigDecimal("1000"), null, LocalDate.of(2026, 10, 19));
    }

    @Test
    void createsGoalInPreferredCurrency() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(goalRepository.saveAndFlush(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(USER_ID, new CreateGoalRequest("Trip", new BigDecimal("1000"), null, null));

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void duplicateNameIsAConflict() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(goalRepository.existsByUserIdAndNameIgnoreCase(USER_ID, "Trip")).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateGoalRequest(" Trip ", BigDecimal.TEN, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void contributionDefaultsToTodayAndIsCreated() {
        SavingsGoal goal = goal();
        when(goalRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(goal));
        when(goalRepository.saveAndFlush(goal)).thenReturn(goal);

        SavingsGoalService.ContributionResult result =
                service.contribute(USER_ID, 3L, new ContributionRequest(new BigDecimal("250"), null, null, "abcdefgh"));

        assertThat(result.created()).isTrue();
        assertThat(result.goal().savedAmount()).isEqualByComparingTo("250");
        assertThat(result.goal().contributions().getFirst().contributionDate()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void replayedIdempotencyKeyIsNotCountedTwice() {
        SavingsGoal goal = goal();
        goal.contribute(new BigDecimal("250"), LocalDate.of(2026, 10, 19), null, "abcdefgh", LocalDate.of(2026, 10, 19),
                CLOCK.instant());
        when(goalRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(goal));

        SavingsGoalService.ContributionResult replay =
                service.contribute(USER_ID, 3L, new ContributionRequest(new BigDecimal("250"), null, null, "abcdefgh"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.goal().savedAmount()).isEqualByComparingTo("250");
        verify(goalRepository, never()).saveAndFlush(any());
    }

    @Test
    void removingUnknownContributionIsNotFound() {
        when(goalRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(goal()));

        assertThatThrownBy(() -> service.removeContribution(USER_ID, 3L, 42L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anotherUsersGoalIsNotFound() {
        when(goalRepository.findByIdAndUserId(3L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
