package com.nest.model;

import com.nest.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Objects;

/**
 * A registered Nest user and the owner of all of their financial data.
 *
 * <p>Encapsulation: there are no public setters. The profile changes only through
 * {@link #updateProfile}, which re-validates every field. The password hash is never exposed;
 * callers ask {@link #passwordMatches} instead, so the hash cannot leak through a getter.
 */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "preferred_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String preferredCurrency;

    /** Required by JPA only. */
    protected User() {
    }

    public User(String firstName, String lastName, String email, String passwordHash, String preferredCurrency) {
        this.email = normalizeEmail(requireText(email, "Email"));
        changePasswordHash(passwordHash);
        updateProfile(firstName, lastName, preferredCurrency);
    }

    /**
     * Canonical form used for storage and lookups, which makes email uniqueness case-insensitive.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public void updateProfile(String firstName, String lastName, String preferredCurrency) {
        this.firstName = requireText(firstName, "First name").strip();
        this.lastName = requireText(lastName, "Last name").strip();
        this.preferredCurrency = Currencies.requireSupported(preferredCurrency);
    }

    /** Replaces the stored hash. The caller is responsible for hashing; plaintext never reaches the entity. */
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = requireText(newPasswordHash, "Password hash");
    }

    /**
     * Verifies a raw password against the stored hash using the supplied encoder.
     * The entity keeps its hash private; the encoder (BCrypt) is passed in by the service.
     */
    public boolean passwordMatches(String rawPassword, PasswordEncoder encoder) {
        return rawPassword != null && encoder.matches(rawPassword, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPreferredCurrency() {
        return preferredCurrency;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(field + " must not be blank.");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(User.class);
    }

    @Override
    public String toString() {
        return "User{id=%d, email='%s'}".formatted(id, email);
    }
}
