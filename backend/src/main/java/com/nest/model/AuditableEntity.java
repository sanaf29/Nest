package com.nest.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Audit timestamps shared by every persistent entity.
 *
 * <p>This is a genuine IS-A relationship: every entity is an auditable record. The superclass
 * holds no domain behaviour, so subclasses cannot violate it. Timestamps are set only by JPA
 * lifecycle callbacks and are never writable from outside.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Matches the DATETIMEOFFSET(6) column precision so in-memory and stored values agree.
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
