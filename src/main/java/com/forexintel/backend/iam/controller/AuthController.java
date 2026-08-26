package com.forexintel.backend.iam.controller;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.dto.AuthResponse;
import com.forexintel.backend.iam.dto.LoginRequest;
import com.forexintel.backend.iam.dto.RegisterRequest;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.shared.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .subscriptionPlan("FREE_TRIAL")
                .onboardingCompleted(false)
                .build();
        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        return ResponseEntity.ok(AuthResponse.builder().token(token).refreshToken(refreshToken).build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtProvider.generateToken(user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        return ResponseEntity.ok(AuthResponse.builder().token(token).refreshToken(refreshToken).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtProvider.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid refresh token");
        }
        
        String email = jwtProvider.extractEmail(refreshToken);
        String newToken = jwtProvider.generateToken(email);
        
        return ResponseEntity.ok(AuthResponse.builder().token(newToken).refreshToken(refreshToken).build());
    }
}
