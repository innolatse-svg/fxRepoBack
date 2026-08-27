package com.forexintel.backend.subscription.service;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion du cycle de vie des abonnements SaaS et de l'expiration des périodes d'essai.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;

    /**
     * Tâche quotidienne automatique (minuit) vérifiant l'échéance des périodes d'essai (15 jours).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkExpiredTrialsDaily() {
        log.info("[SubscriptionService] Vérification automatique des périodes d'essai expirées");
        List<User> users = userRepository.findAll();
        OffsetDateTime now = OffsetDateTime.now();

        int expiredCount = 0;
        for (User user : users) {
            if ("FREE_TRIAL".equalsIgnoreCase(user.getSubscriptionPlan())
                    && "ACTIVE".equalsIgnoreCase(user.getSubscriptionStatus())
                    && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {

                if (user.getTrialEndsAt() != null && user.getTrialEndsAt().isBefore(now)) {
                    user.setSubscriptionStatus("EXPIRED");
                    userRepository.save(user);
                    expiredCount++;
                    log.info("[SubscriptionService] Période d'essai expirée pour l'utilisateur {}", user.getEmail());
                }
            }
        }
        log.info("[SubscriptionService] {} utilisateur(s) passé(s) au statut EXPIRED", expiredCount);
    }

    /**
     * Met à jour le forfait d'un utilisateur après souscription.
     */
    @Transactional
    public User upgradeSubscription(UUID userId, String newPlan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));

        user.setSubscriptionPlan(newPlan.toUpperCase());
        user.setSubscriptionStatus("ACTIVE");
        return userRepository.save(user);
    }
}
