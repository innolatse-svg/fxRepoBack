package com.forexintel.backend.iam.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entité utilisateur du domaine IAM représentant un compte trader ou administrateur.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Entity
@Table(name = "users", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "USER"; // USER, ADMIN, SUPER_ADMIN

    @Column(name = "subscription_plan", length = 50)
    @Builder.Default
    private String subscriptionPlan = "FREE_TRIAL"; // FREE_TRIAL, STARTER, PRO, LIFETIME_UNLIMITED

    @Column(name = "subscription_status", length = 50)
    @Builder.Default
    private String subscriptionStatus = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "onboarding_completed")
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
