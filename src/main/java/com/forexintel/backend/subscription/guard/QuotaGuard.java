package com.forexintel.backend.subscription.guard;

import com.forexintel.backend.iam.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

/**
 * Gardien des quotas SaaS et des droits d'accès par niveau d'abonnement.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Component
public class QuotaGuard {

    /**
     * Vérifie si un utilisateur est autorisé à connecter un compte de trading supplémentaire.
     *
     * @param user Utilisateur demandeur
     * @param currentAccountCount Nombre actuel de comptes déjà enregistrés
     */
    public void checkCanAddTradingAccount(User user, long currentAccountCount) {
        if (isSuperAdminOrUnlimited(user)) {
            log.info("[QuotaGuard] Super Admin / Unlimited bypass pour {}", user.getEmail());
            return;
        }

        checkSubscriptionActive(user);

        int maxAccounts = switch (user.getSubscriptionPlan() != null ? user.getSubscriptionPlan().toUpperCase() : "FREE_TRIAL") {
            case "STARTER" -> 2;
            case "PRO" -> 5;
            case "LIFETIME_UNLIMITED" -> Integer.MAX_VALUE;
            default -> 1; // FREE_TRIAL
        };

        if (currentAccountCount >= maxAccounts) {
            log.warn("[QuotaGuard] Limite de comptes atteinte pour {} : {} / {} autorisés", user.getEmail(), currentAccountCount, maxAccounts);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Limite de comptes broker atteinte pour le forfait %s (%d max). Veuillez passer à un forfait supérieur.",
                            user.getSubscriptionPlan(), maxAccounts)
            );
        }
    }

    /**
     * Vérifie si l'utilisateur est autorisé à exécuter un signal ou un ordre de marché.
     */
    public void checkCanExecuteTrade(User user) {
        if (isSuperAdminOrUnlimited(user)) {
            return;
        }

        checkSubscriptionActive(user);
    }

    /**
     * Valide que l'abonnement ou la période d'essai de l'utilisateur n'est pas expiré.
     */
    public void checkSubscriptionActive(User user) {
        if (isSuperAdminOrUnlimited(user)) {
            return;
        }

        if ("EXPIRED".equalsIgnoreCase(user.getSubscriptionStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Votre période d'essai ou votre abonnement a expiré. Veuillez choisir un forfait actif pour continuer."
            );
        }

        if (user.getTrialEndsAt() != null && user.getTrialEndsAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Votre période d'essai de 15 jours est terminée. Veuillez renouveler votre accès."
            );
        }
    }

    public boolean isSuperAdminOrUnlimited(User user) {
        if (user == null) return false;
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isUnlimited = "LIFETIME_UNLIMITED".equalsIgnoreCase(user.getSubscriptionPlan());
        return isSuperAdmin || isUnlimited;
    }
}
