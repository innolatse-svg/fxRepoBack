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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "subscriptionPlan", user.getSubscriptionPlan(),
                "onboardingCompleted", user.getOnboardingCompleted()
        ));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<Void> updatePreferences(Authentication authentication, @RequestBody Map<String, Object> payload) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserPreference preference = userPreferenceRepository.findById(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        preference.setSettings(payload);
        userPreferenceRepository.save(preference);

        user.setOnboardingCompleted(true);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }
}
