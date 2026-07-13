package com.tradeforge.auth.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Registered user (trader or administrator).
 *
 * <p>Rules enforced here and in the database:
 * <ul>
 *   <li>Email is normalised (lower-cased, trimmed) before persistence.</li>
 *   <li>Email is unique.</li>
 *   <li>Password hash is never serialised — the field has no getter returning the raw hash.</li>
 *   <li>Default role is {@link UserRole#TRADER}.</li>
 * </ul>
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email")
)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Normalised (lower-cased, trimmed) email — unique within the system.
     */
    @Column(nullable = false, length = 254)
    private String email;

    /**
     * BCrypt hash. Never exposed via public API.
     */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.TRADER;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected User() { /* JPA */ }

    /**
     * Create a new user. Email is normalised automatically.
     *
     * @param name         display name
     * @param email        raw email (will be trimmed and lower-cased)
     * @param passwordHash BCrypt hash from the auth service
     * @param role         assigned role
     */
    public static User create(String name, String email, String passwordHash, UserRole role) {
        User user = new User();
        user.name         = name.strip();
        user.email        = normaliseEmail(email);
        user.passwordHash = passwordHash;
        user.role         = role;
        return user;
    }

    // ── Domain methods ────────────────────────────────────────────────────────

    /**
     * Normalise an email: trim whitespace and lower-case.
     */
    public static String normaliseEmail(String raw) {
        if (raw == null) throw new IllegalArgumentException("Email must not be null");
        return raw.strip().toLowerCase();
    }

    public boolean isAdmin() {
        return UserRole.ADMIN == role;
    }

    // ── Getters (no setter for passwordHash) ──────────────────────────────────

    public UUID getId()           { return id; }
    public String getName()       { return name; }
    public String getEmail()      { return email; }
    public UserRole getRole()     { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Returns the stored password hash. This method must only be called by
     * authentication infrastructure — never serialised to API responses.
     */
    public String getPasswordHash() { return passwordHash; }
}
