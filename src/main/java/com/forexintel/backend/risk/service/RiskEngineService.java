package com.forexintel.backend.risk.service;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.domain.RiskAuditLog;
import com.forexintel.backend.risk.dto.RiskAuditLogDto;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.repository.RiskAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cœur d'évaluation financière et d'autorisation d'ordre (Risk Engine).
 * Applique strictement le principe de Zero-Trust et la hiérarchie : Règle Utilisateur <= Limite Plateforme.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngineService {

    // Plafonds inviolables de la plateforme (Hard Guardrails)
    public static final double PLATFORM_MAX_RISK_PER_TRADE_PCT = 3.0;
    public static final double PLATFORM_MAX_DAILY_LOSS_PCT = 5.0;
    public static final double PLATFORM_MAX_EXPOSURE_PCT = 10.0;
    public static final int PLATFORM_MAX_OPEN_POSITIONS = 5;

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final RiskAuditLogRepository riskAuditLogRepository;

    /**
     * Évalue une intention de trading pour un utilisateur donné et enregistre l'audit immuable.
     *
     * @param userId Identifiant unique de l'utilisateur
     * @param intent Données de l'ordre à analyser
     * @return Résultat de l'évaluation (ALLOWED / REJECTED avec motif)
     */
    @Transactional
    public RiskEvaluationResultDto evaluateTradeIntent(UUID userId, TradeIntentDto intent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));

        // 1. Récupération des préférences utilisateur
        UserPreference preference = userPreferenceRepository.findById(userId).orElse(null);
        Map<String, Object> settings = preference != null ? preference.getSettings() : null;

        double userRiskPerTrade = extractDouble(settings, "riskRules", "riskPerTradePercent", 1.0);
        double userMaxDailyLoss = extractDouble(settings, "riskRules", "maxDailyLossPercent", 3.0);
        double userMaxExposure = extractDouble(settings, "riskRules", "maxExposurePercent", 6.0);
        int userMaxPositions = extractInt(settings, "riskRules", "maxOpenPositions", 3);

        // 2. Application de la suprématie Plateforme (Règle Utilisateur <= Limite Plateforme)
        double effectiveMaxRisk = Math.min(userRiskPerTrade, PLATFORM_MAX_RISK_PER_TRADE_PCT);
        double effectiveMaxDailyLoss = Math.min(userMaxDailyLoss, PLATFORM_MAX_DAILY_LOSS_PCT);
        double effectiveMaxExposure = Math.min(userMaxExposure, PLATFORM_MAX_EXPOSURE_PCT);
        int effectiveMaxPositions = Math.min(userMaxPositions, PLATFORM_MAX_OPEN_POSITIONS);

        double requestedRisk = intent.getRequestedRiskPct() != null ? intent.getRequestedRiskPct() : 1.0;
        double currentExposure = intent.getCurrentExposurePct() != null ? intent.getCurrentExposurePct() : 0.0;
        double currentDailyLoss = intent.getCurrentDailyLossPct() != null ? intent.getCurrentDailyLossPct() : 0.0;
        int currentPositions = intent.getCurrentOpenPositions() != null ? intent.getCurrentOpenPositions() : 0;
        double lotSize = intent.getLotSize() != null ? intent.getLotSize() : 0.1;

        // 3. Algorithmes de contrôle de risque
        boolean isAllowed = true;
        String reason = "Ordre conforme aux règles de risque. Risque : " + requestedRisk + "%, Exposition totale autorisée.";
        String ceilingApplied = userRiskPerTrade <= PLATFORM_MAX_RISK_PER_TRADE_PCT ? "USER_LIMIT" : "PLATFORM_HARD_LIMIT";

        // A. Vérification de la présence d'un Stop Loss
        if (intent.getStopLoss() == null || intent.getStopLoss() <= 0) {
            isAllowed = false;
            reason = "Rejeté : Stop Loss obligatoire manquant ou invalide.";
        }
        // B. Contrôle du risque par trade
        else if (requestedRisk > effectiveMaxRisk) {
            isAllowed = false;
            reason = String.format("Rejeté : Risque demandé (%.2f%%) excède le plafond autorisé (%.2f%%).",
                    requestedRisk, effectiveMaxRisk);
        }
        // C. Contrôle du nombre maximum de positions ouvertes
        else if (currentPositions >= effectiveMaxPositions) {
            isAllowed = false;
            reason = String.format("Rejeté : Nombre maximum de positions ouvertes atteint (%d/%d).",
                    currentPositions, effectiveMaxPositions);
        }
        // D. Contrôle du disjoncteur journalier (Daily Drawdown)
        else if (currentDailyLoss >= effectiveMaxDailyLoss) {
            isAllowed = false;
            reason = String.format("Rejeté : Disjoncteur journalier enclenché (Perte actuelle : %.2f%% / Max : %.2f%%).",
                    currentDailyLoss, effectiveMaxDailyLoss);
        }
        // E. Contrôle de l'exposition globale cumulée
        else if ((currentExposure + requestedRisk) > effectiveMaxExposure) {
            isAllowed = false;
            reason = String.format("Rejeté : Exposition globale cumulée (%.2f%%) excède le plafond autorisé (%.2f%%).",
                    (currentExposure + requestedRisk), effectiveMaxExposure);
        }

        String decision = isAllowed ? "ALLOWED" : "REJECTED";

        // 4. Traçabilité immuable dans l'Audit Log
        RiskAuditLog auditLog = RiskAuditLog.builder()
                .user(user)
                .symbol(intent.getSymbol() != null ? intent.getSymbol() : "UNKNOWN")
                .actionType("EVALUATION")
                .requestedRiskPct(requestedRisk)
                .lotSize(lotSize)
                .decision(decision)
                .reason(reason)
                .build();

        riskAuditLogRepository.save(auditLog);
        log.info("[RiskEngine] Décision pour {} sur {} : {} - Motif : {}",
                user.getEmail(), intent.getSymbol(), decision, reason);

        return RiskEvaluationResultDto.builder()
                .decision(decision)
                .allowed(isAllowed)
                .reason(reason)
                .effectiveRiskPct(effectiveMaxRisk)
                .appliedCeiling(ceilingApplied)
                .maxAllowedLotSize(lotSize)
                .build();
    }

    /**
     * Récupère l'historique immuable des décisions du Risk Engine pour un utilisateur.
     */
    @Transactional(readOnly = true)
    public List<RiskAuditLogDto> getUserAuditLogs(UUID userId) {
        return riskAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(log -> RiskAuditLogDto.builder()
                        .id(log.getId())
                        .symbol(log.getSymbol())
                        .actionType(log.getActionType())
                        .requestedRiskPct(log.getRequestedRiskPct())
                        .lotSize(log.getLotSize())
                        .decision(log.getDecision())
                        .reason(log.getReason())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private double extractDouble(Map<String, Object> settings, String parentKey, String fieldKey, double defaultValue) {
        if (settings == null || !settings.containsKey(parentKey)) return defaultValue;
        Object parent = settings.get(parentKey);
        if (parent instanceof Map<?, ?> map) {
            Object val = map.get(fieldKey);
            if (val instanceof Number num) {
                return num.doubleValue();
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private int extractInt(Map<String, Object> settings, String parentKey, String fieldKey, int defaultValue) {
        if (settings == null || !settings.containsKey(parentKey)) return defaultValue;
        Object parent = settings.get(parentKey);
        if (parent instanceof Map<?, ?> map) {
            Object val = map.get(fieldKey);
            if (val instanceof Number num) {
                return num.intValue();
            }
        }
        return defaultValue;
    }
}
