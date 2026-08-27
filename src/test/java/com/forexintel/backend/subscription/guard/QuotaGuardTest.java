package com.forexintel.backend.subscription.guard;

import com.forexintel.backend.iam.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du QuotaGuard pour la gestion des plafonds SaaS et du statut Super Admin.
 *
 * @author Innocent
 */
class QuotaGuardTest {

    private QuotaGuard quotaGuard;

    @BeforeEach
    void setUp() {
        quotaGuard = new QuotaGuard();
    }

    @Test
    @DisplayName("Devrait autoriser le Super Admin à outrepasser toutes les limites sans restriction")
    void checkCanAddTradingAccount_SuperAdmin_ShouldBypassAllLimits() {
        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@forexintel.com")
                .role("SUPER_ADMIN")
                .subscriptionPlan("LIFETIME_UNLIMITED")
                .subscriptionStatus("ACTIVE")
                .build();

        // Tente d'ajouter un 100ème compte MT5
        assertDoesNotThrow(() -> quotaGuard.checkCanAddTradingAccount(superAdmin, 99));
        assertDoesNotThrow(() -> quotaGuard.checkCanExecuteTrade(superAdmin));
    }

    @Test
    @DisplayName("Devrait bloquer un utilisateur en forfait FREE_TRIAL tentant d'ajouter un 2ème compte MT5")
    void checkCanAddTradingAccount_FreeTrial_ShouldBlockSecondAccount() {
        User trialUser = User.builder()
                .id(UUID.randomUUID())
                .email("trial@forexintel.com")
                .role("USER")
                .subscriptionPlan("FREE_TRIAL")
                .subscriptionStatus("ACTIVE")
                .trialEndsAt(OffsetDateTime.now().plusDays(10))
                .build();

        // 1er compte (0 existant) -> Autorisé
        assertDoesNotThrow(() -> quotaGuard.checkCanAddTradingAccount(trialUser, 0));

        // 2ème compte (1 existant) -> Rejeté (403 FORBIDDEN)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quotaGuard.checkCanAddTradingAccount(trialUser, 1)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Limite de comptes broker atteinte"));
    }

    @Test
    @DisplayName("Devrait rejeter l'accès si la période d'essai de 15 jours est expirée")
    void checkSubscriptionActive_ExpiredTrial_ShouldThrowForbidden() {
        User expiredUser = User.builder()
                .id(UUID.randomUUID())
                .email("expired@forexintel.com")
                .role("USER")
                .subscriptionPlan("FREE_TRIAL")
                .subscriptionStatus("ACTIVE")
                .trialEndsAt(OffsetDateTime.now().minusDays(1)) // Expiré hier
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quotaGuard.checkSubscriptionActive(expiredUser)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("période d'essai de 15 jours est terminée"));
    }

    @Test
    @DisplayName("Devrait rejeter l'accès si le statut de l'abonnement est explicitement EXPIRED")
    void checkSubscriptionActive_ExplicitlyExpired_ShouldThrowForbidden() {
        User expiredUser = User.builder()
                .id(UUID.randomUUID())
                .email("expired2@forexintel.com")
                .role("USER")
                .subscriptionPlan("PRO")
                .subscriptionStatus("EXPIRED")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quotaGuard.checkCanExecuteTrade(expiredUser)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("expiré"));
    }
}
