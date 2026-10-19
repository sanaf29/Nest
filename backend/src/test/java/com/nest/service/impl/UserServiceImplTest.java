package com.nest.service.impl;

import com.nest.dto.request.UpdateProfileRequest;
import com.nest.dto.response.UserResponse;
import com.nest.exception.BusinessRuleException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.UserMapper;
import com.nest.model.User;
import com.nest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, new UserMapper());
        user = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
    }

    @Test
    void returnsProfileOfRequestedUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserResponse profile = userService.getProfile(7L);

        assertThat(profile.email()).isEqualTo("sanae@example.com");
        assertThat(profile.firstName()).isEqualTo("Sanae");
    }

    @Test
    void missingUserIsNotFound() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesProfileAndFlushesSoAuditTimestampIsCurrent() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse updated = userService.updateProfile(7L, new UpdateProfileRequest("Sana", "F", "EUR"));

        assertThat(updated.firstName()).isEqualTo("Sana");
        assertThat(updated.preferredCurrency()).isEqualTo("EUR");
        assertThat(updated.email()).isEqualTo("sanae@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void invalidUpdateIsRejectedByTheDomainAndNotSaved() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(7L, new UpdateProfileRequest("Sana", "F", "XAU")))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }
}
