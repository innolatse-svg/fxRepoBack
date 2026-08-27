package com.forexintel.backend.subscription.service;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du SubscriptionService pour le passage automatique au statut EXPIRED.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User expiredUser;
    private User activeUser;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        expiredUser = User.builder()
                .id(UUID.randomUUID())
                .email("expired@forexintel.com")
                .role("USER")
                .subscriptionPlan("FREE_TRIAL")
                .subscriptionStatus("ACTIVE")
                .trialEndsAt(OffsetDateTime.now().minusDays(2))
                .build();

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .email("active@forexintel.com")
                .role("USER")
                .subscriptionPlan("FREE_TRIAL")
                .subscriptionStatus("ACTIVE")
                .trialEndsAt(OffsetDateTime.now().plusDays(10))
                .build();

        superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@forexintel.com")
                .role("SUPER_ADMIN")
                .subscriptionPlan("LIFETIME_UNLIMITED")
                .subscriptionStatus("ACTIVE")
                .trialEndsAt(OffsetDateTime.now().minusDays(5)) // Même avec date passée, le SuperAdmin ne doit jamais expirer
                .build();
    }

    @Test
    @DisplayName("Devrait passer au statut EXPIRED uniquement les utilisateurs dont l'essai est dépassé")
    void checkExpiredTrialsDaily_ShouldExpireOnlyOverdueTrials() {
        when(userRepository.findAll()).thenReturn(List.of(expiredUser, activeUser, superAdmin));

        subscriptionService.checkExpiredTrialsDaily();

        assertEquals("EXPIRED", expiredUser.getSubscriptionStatus());
        assertEquals("ACTIVE", activeUser.getSubscriptionStatus());
        assertEquals("ACTIVE", superAdmin.getSubscriptionStatus());

        verify(userRepository, times(1)).save(expiredUser);
        verify(userRepository, never()).save(activeUser);
        verify(userRepository, never()).save(superAdmin);
    }

    @Test
    @DisplayName("Devrait mettre à niveau le plan d'un utilisateur et réactiver son statut")
    void upgradeSubscription_ShouldUpdatePlanAndActivate() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(expiredUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User upgraded = subscriptionService.upgradeSubscription(userId, "PRO");

        assertEquals("PRO", upgraded.getSubscriptionPlan());
        assertEquals("ACTIVE", upgraded.getSubscriptionStatus());
    }
}
