package com.forexintel.backend.execution.controller;

import com.forexintel.backend.execution.dto.CreateTradingAccountRequestDto;
import com.forexintel.backend.execution.dto.TradingAccountResponseDto;
import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.service.TradingAccountService;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion sécurisée des comptes MT5 et le routage des ordres.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class TradingAccountController {

    private final TradingAccountService tradingAccountService;
    private final com.forexintel.backend.execution.service.AccountSyncService accountSyncService;
    private final UserRepository userRepository;

    /**
     * Connecte et chiffre un nouveau compte MT5 dans le Credential Vault.
     */
    @PostMapping
    public ResponseEntity<TradingAccountResponseDto> createAccount(
            Authentication authentication,
            @RequestBody CreateTradingAccountRequestDto request) {
        User user = getUser(authentication);
        TradingAccountResponseDto response = tradingAccountService.createAccount(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère la liste des comptes connectés pour l'utilisateur authentifié (Zero-Leak).
     */
    @GetMapping
    public ResponseEntity<List<TradingAccountResponseDto>> getAccounts(Authentication authentication) {
        User user = getUser(authentication);
        List<TradingAccountResponseDto> accounts = tradingAccountService.getUserAccounts(user.getId());
        return ResponseEntity.ok(accounts);
    }

    /**
     * Synchronise immédiatement les soldes et métriques d'un compte broker spécifique.
     */
    @GetMapping("/{id}/sync")
    public ResponseEntity<TradingAccountResponseDto> syncAccount(
            Authentication authentication,
            @PathVariable UUID id) {
        User user = getUser(authentication);
        TradingAccountResponseDto synced = accountSyncService.syncSingleAccount(user.getId(), id);
        return ResponseEntity.ok(synced);
    }

    /**
     * Supprime un compte de trading et ses identifiants du Vault.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            @PathVariable UUID id) {
        User user = getUser(authentication);
        tradingAccountService.deleteAccount(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bascule l'état d'activation du trading automatique.
     */
    @PatchMapping("/{id}/toggle-trading")
    public ResponseEntity<TradingAccountResponseDto> toggleAutoTrading(
            Authentication authentication,
            @PathVariable UUID id) {
        User user = getUser(authentication);
        TradingAccountResponseDto updated = tradingAccountService.toggleAutoTrading(user.getId(), id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Exécute un ordre de trading via le pipeline complet (Risk Engine -> Vault -> Provider).
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<TradingExecutionProvider.ExecutionResult> executeTrade(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody TradeIntentDto intent) {
        User user = getUser(authentication);
        TradingExecutionProvider.ExecutionResult result = tradingAccountService.executeTradePipeline(user.getId(), id, intent);
        return ResponseEntity.ok(result);
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
