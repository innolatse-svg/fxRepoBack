package com.forexintel.backend.iam.controller;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import com.forexintel.backend.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour la consultation du profil utilisateur et la mise à jour des préférences.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * Récupère les informations du profil utilisateur connecté avec statut SaaS et rôle.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole());
        response.put("subscriptionPlan", user.getSubscriptionPlan());
        response.put("subscriptionStatus", user.getSubscriptionStatus());
        response.put("trialEndsAt", user.getTrialEndsAt());
        response.put("onboardingCompleted", user.getOnboardingCompleted());

        return ResponseEntity.ok(response);
    }

    /**
     * Enregistre les préférences de trading et valide l'onboarding.
     */
    @PutMapping("/me/preferences")
    public ResponseEntity<Void> updatePreferences(Authentication authentication, @RequestBody Map<String, Object> payload) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        UserPreference preference = userPreferenceRepository.findById(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        preference.setSettings(payload);
        userPreferenceRepository.save(preference);

        user.setOnboardingCompleted(true);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }
}
