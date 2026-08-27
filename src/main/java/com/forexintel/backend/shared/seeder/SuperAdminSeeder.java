package com.forexintel.backend.shared.seeder;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Initialiseur automatique du compte Super Administrateur de la plateforme (Role SUPER_ADMIN, LIFETIME_UNLIMITED).
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String SUPER_ADMIN_EMAIL = "admin@forexintel.com";
    public static final String SUPER_ADMIN_DEFAULT_PASSWORD = "SuperAdmin2026!ForexIntelSecure";

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(SUPER_ADMIN_EMAIL).isEmpty()) {
            log.info("[SuperAdminSeeder] Création du compte Super Administrateur maître ({})", SUPER_ADMIN_EMAIL);

            User superAdmin = User.builder()
                    .email(SUPER_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(SUPER_ADMIN_DEFAULT_PASSWORD))
                    .firstName("Super")
                    .lastName("Admin")
                    .role("SUPER_ADMIN")
                    .subscriptionPlan("LIFETIME_UNLIMITED")
                    .subscriptionStatus("ACTIVE")
                    .onboardingCompleted(true)
                    .trialEndsAt(OffsetDateTime.now().plusYears(100))
                    .build();

            userRepository.save(superAdmin);
            log.info("[SuperAdminSeeder] Compte Super Administrateur initialisé avec succès avec statut LIFETIME_UNLIMITED");
        }
    }
}
