package com.forexintel.backend.risk.controller;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.dto.RiskAuditLogDto;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Contrôleur REST pour l'évaluation du risque d'un ordre et la consultation des journaux d'audit.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskEngineService riskEngineService;
    private final UserRepository userRepository;

    /**
     * Évalue la conformité d'une intention de trade par rapport aux règles de risque.
     *
     * @param authentication Session de l'utilisateur connecté
     * @param intent Intention de trade à analyser
     * @return Résultat d'évaluation et décision du moteur
     */
    @PostMapping("/evaluate")
    public ResponseEntity<RiskEvaluationResultDto> evaluateTrade(
            Authentication authentication,
            @RequestBody TradeIntentDto intent) {
        User user = getUser(authentication);
        RiskEvaluationResultDto result = riskEngineService.evaluateTradeIntent(user.getId(), intent);
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère l'historique complet des décisions du journal d'audit du Risk Engine.
     *
     * @param authentication Session de l'utilisateur connecté
     * @return Liste ordonnée des audits de risque
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<RiskAuditLogDto>> getAuditLogs(Authentication authentication) {
        User user = getUser(authentication);
        List<RiskAuditLogDto> logs = riskEngineService.getUserAuditLogs(user.getId());
        return ResponseEntity.ok(logs);
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
