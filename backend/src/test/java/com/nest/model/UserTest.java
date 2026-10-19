package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTest {

    private static User validUser() {
        return new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
    }

    @Nested
    class Creation {

        @Test
        void normalisesEmailToLowerCaseWithoutSurroundingWhitespace() {
            User user = new User("Sanae", "Faghfouri", "  Sanae@Example.COM ", "$2a$hash", "CAD");

            assertThat(user.getEmail()).isEqualTo("sanae@example.com");
        }

        @Test
        void stripsWhitespaceFromNames() {
            User user = new User("  Sanae ", " Faghfouri  ", "s@example.com", "$2a$hash", "CAD");

            assertThat(user.getFirstName()).isEqualTo("Sanae");
            assertThat(user.getLastName()).isEqualTo("Faghfouri");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlankFirstName(String firstName) {
            assertThatThrownBy(() -> new User(firstName, "Faghfouri", "s@example.com", "$2a$hash", "CAD"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("First name");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void rejectsBlankPasswordHash(String hash) {
            assertThatThrownBy(() -> new User("Sanae", "Faghfouri", "s@example.com", hash, "CAD"))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"cad", "ZZZ", "XAU", "CA"})
        void rejectsUnsupportedCurrency(String currency) {
            assertThatThrownBy(() -> new User("Sanae", "Faghfouri", "s@example.com", "$2a$hash", currency))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("currency");
        }
    }

    @Nested
    class ProfileUpdates {

        @Test
        void updatesNameAndCurrency() {
            User user = validUser();

            user.updateProfile("Sana", "F", "EUR");

            assertThat(user.getFirstName()).isEqualTo("Sana");
            assertThat(user.getLastName()).isEqualTo("F");
            assertThat(user.getPreferredCurrency()).isEqualTo("EUR");
        }

        @Test
        void rejectsInvalidUpdateWithoutPartiallyApplyingIt() {
            User user = validUser();

            assertThatThrownBy(() -> user.updateProfile("Sana", "  ", "EUR"))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(user.getLastName()).isEqualTo("Faghfouri");
            assertThat(user.getPreferredCurrency()).isEqualTo("CAD");
        }
    }

    @Nested
    class PasswordVerification {

        @Test
        void delegatesComparisonToTheEncoderWithTheStoredHash() {
            PasswordEncoder encoder = mock(PasswordEncoder.class);
            when(encoder.matches("secret123", "$2a$hash")).thenReturn(true);

            assertThat(validUser().passwordMatches("secret123", encoder)).isTrue();
            verify(encoder).matches("secret123", "$2a$hash");
        }

        @Test
        void nullPasswordNeverMatches() {
            assertThat(validUser().passwordMatches(null, mock(PasswordEncoder.class))).isFalse();
        }
    }

    @Test
    void toStringNeverContainsThePasswordHash() {
        assertThat(validUser().toString()).doesNotContain("$2a$hash");
    }
}
