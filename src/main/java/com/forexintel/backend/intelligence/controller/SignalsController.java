package com.forexintel.backend.intelligence.controller;

import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.intelligence.dto.ExecuteSignalRequestDto;
import com.forexintel.backend.intelligence.dto.TradeSignalDto;
import com.forexintel.backend.intelligence.service.SignalsEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la consultation et l'exécution directe des signaux de trading IA.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalsController {

    private final SignalsEngineService signalsEngineService;
    private final UserRepository userRepository;

    /**
     * Récupère la liste filtrée des signaux d'opportunité IA.
     */
    @GetMapping
    public ResponseEntity<List<TradeSignalDto>> getSignals(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String direction) {
        List<TradeSignalDto> list = signalsEngineService.getSignals(status, direction);
        return ResponseEntity.ok(list);
    }

    /**
     * Exécute un signal de trading vers un compte MT5 via le pipeline de risque et le Vault.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<TradingExecutionProvider.ExecutionResult> executeSignal(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody ExecuteSignalRequestDto request) {
        User user = getUser(authentication);
        TradingExecutionProvider.ExecutionResult result = signalsEngineService.executeSignal(user.getId(), id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Ignore ou rejette un signal.
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissSignal(@PathVariable UUID id) {
        signalsEngineService.dismissSignal(id);
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
