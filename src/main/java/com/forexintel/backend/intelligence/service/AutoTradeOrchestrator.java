package com.forexintel.backend.intelligence.service;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.execution.service.TradingAccountService;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.intelligence.domain.TradeSignal;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Moteur d'exécution autonome Full-Auto des signaux d'intelligence artificielle.
 * Orchestration : Détection de signal -> Filtre de profil utilisateur -> Validation Risk Engine -> Routage MT5 Bridge.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTradeOrchestrator {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradingAccountService tradingAccountService;
    private final RiskEngineService riskEngineService;

    /**
     * Traite et exécute automatiquement un signal pour tous les traders configurés en FULL_AUTO.
     *
     * @param signal Signal d'opportunité généré
     */
    public void processAutoExecutionForSignal(TradeSignal signal) {
        log.info("[AutoTradeOrchestrator] Évaluation du signal {} ({}) pour exécution Full-Auto", signal.getId(), signal.getSymbol());

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> !"EXPIRED".equalsIgnoreCase(u.getSubscriptionStatus()))
                .toList();

        for (User user : activeUsers) {
            try {
                UserPreference pref = userPreferenceRepository.findById(user.getId()).orElse(null);
                if (pref == null || pref.getSettings() == null) continue;

                Map<String, Object> settings = pref.getSettings();
                String automationLevel = String.valueOf(settings.getOrDefault("automationLevel", "MANUAL"));

                if ("FULL_AUTO".equalsIgnoreCase(automationLevel)) {
                    executeAutoTradeForUser(user, signal, settings);
                }
            } catch (Exception e) {
                log.error("[AutoTradeOrchestrator] Erreur lors de l'exécution automatique pour {} : {}", user.getEmail(), e.getMessage());
            }
        }
    }

    private void executeAutoTradeForUser(User user, TradeSignal signal, Map<String, Object> settings) {
        List<TradingAccount> accounts = tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (accounts.isEmpty()) {
            log.debug("[AutoTradeOrchestrator] Utilisateur {} en FULL_AUTO mais aucun compte MT5 raccordé", user.getEmail());
            return;
        }

        TradingAccount targetAccount = accounts.get(0);
        double requestedRiskPct = 1.0;
        if (settings.containsKey("maxRiskPerTradePct")) {
            requestedRiskPct = Double.parseDouble(settings.get("maxRiskPerTradePct").toString());
        }

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol(signal.getSymbol())
                .direction(signal.getDirection())
                .lotSize(0.25)
                .entryPrice(signal.getEntryPrice())
                .stopLoss(signal.getStopLoss())
                .takeProfit(signal.getTakeProfit())
                .requestedRiskPct(requestedRiskPct)
                .build();

        // 1. Validation financière préalable auprès du Risk Engine
        RiskEvaluationResultDto riskCheck = riskEngineService.evaluateTradeIntent(user.getId(), intent);

        if (!riskCheck.isAllowed()) {
            log.warn("[AutoTradeOrchestrator] Ordre automatique rejeté par le Risk Engine pour {} : {}", user.getEmail(), riskCheck.getReason());
            return;
        }

        // 2. Dispatch vers le pipeline MT5
        TradingExecutionProvider.ExecutionResult result = tradingAccountService.executeTradePipeline(
                user.getId(),
                targetAccount.getId(),
                intent
        );

        if (result.success()) {
            log.info("[AutoTradeOrchestrator] 🚀 Ordre FULL_AUTO exécuté avec succès pour {} : Ticket={}, Prix={}",
                    user.getEmail(), result.orderTicket(), result.executedPrice());
        } else {
            log.warn("[AutoTradeOrchestrator] Échec de l'ordre FULL_AUTO pour {} : {}", user.getEmail(), result.message());
        }
    }
}
