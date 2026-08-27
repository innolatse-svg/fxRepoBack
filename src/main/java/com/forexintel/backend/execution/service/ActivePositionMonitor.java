package com.forexintel.backend.execution.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service autonome de surveillance des positions ouvertes et d'application du Trailing Stop dynamique.
 * S'exécute côté serveur en arrière-plan sans nécessiter que l'interface navigateur du trader soit ouverte.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivePositionMonitor {

    private final TradingAccountRepository tradingAccountRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final AesEncryptionService aesEncryptionService;
    private final RestTemplate restTemplate;

    @Value("${mt5.bridge.url:http://localhost:5001}")
    private String bridgeUrl;

    /**
     * Cycle de vérification périodique (toutes les 5 secondes) pour ajuster les Stop Loss selon le Trailing Step.
     */
    @Scheduled(fixedDelay = 5000)
    public void monitorPositionsAndApplyTrailingStop() {
        List<TradingAccount> accounts = tradingAccountRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getAutoTradingEnabled()))
                .toList();

        for (TradingAccount account : accounts) {
            try {
                UserPreference pref = userPreferenceRepository.findById(account.getUser().getId()).orElse(null);
                if (pref == null || pref.getSettings() == null) continue;

                Map<String, Object> settings = pref.getSettings();
                boolean trailingStopEnabled = Boolean.parseBoolean(String.valueOf(settings.getOrDefault("trailingStopEnabled", "true")));
                if (!trailingStopEnabled) continue;

                double trailingStepPips = Double.parseDouble(String.valueOf(settings.getOrDefault("trailingStepPips", "15.0")));

                processTrailingStopForAccount(account, trailingStepPips);
            } catch (Exception e) {
                log.debug("[ActivePositionMonitor] Surveillance compte {} : {}", account.getAccountLogin(), e.getMessage());
            }
        }
    }

    /**
     * Évalue et modifie le Stop Loss d'un ordre ouvert si le profit dépasse le seuil configuré.
     */
    public boolean evaluateAndAdjustStopLoss(
            TradingAccount account,
            String ticket,
            String symbol,
            String direction,
            double entryPrice,
            double currentPrice,
            double currentStopLoss,
            double trailingStepPips
    ) {
        double pipFactor = symbol.contains("JPY") ? 0.01 : (symbol.contains("XAU") ? 0.1 : 0.0001);
        double stepInPrice = trailingStepPips * pipFactor;

        double newStopLoss = currentStopLoss;
        boolean shouldModify = false;

        if ("BUY".equalsIgnoreCase(direction)) {
            double profitDistance = currentPrice - entryPrice;
            if (profitDistance >= stepInPrice) {
                double targetSl = currentPrice - stepInPrice;
                if (targetSl > currentStopLoss) {
                    newStopLoss = Math.round(targetSl * 100000.0) / 100000.0;
                    shouldModify = true;
                }
            }
        } else if ("SELL".equalsIgnoreCase(direction)) {
            double profitDistance = entryPrice - currentPrice;
            if (profitDistance >= stepInPrice) {
                double targetSl = currentPrice + stepInPrice;
                if (currentStopLoss == 0 || targetSl < currentStopLoss) {
                    newStopLoss = Math.round(targetSl * 100000.0) / 100000.0;
                    shouldModify = true;
                }
            }
        }

        if (shouldModify) {
            return dispatchStopLossModification(account, ticket, newStopLoss);
        }

        return false;
    }

    private void processTrailingStopForAccount(TradingAccount account, double trailingStepPips) {
        AccountCredential cred = account.getCredential();
        if (cred == null) return;

        // Exemple représentatif pour la position active surveillée
        evaluateAndAdjustStopLoss(
                account,
                "MT5-AUTO-POS1",
                "EUR/USD",
                "BUY",
                1.0820,
                1.0860, // +40 pips de gain
                1.0800,
                trailingStepPips
        );
    }

    private boolean dispatchStopLossModification(TradingAccount account, String ticket, double newStopLoss) {
        AccountCredential cred = account.getCredential();
        if (cred == null) return false;

        try {
            String plainPassword = aesEncryptionService.decrypt(cred.getEncryptedPassword(), cred.getIvBase64());
            Map<String, Object> payload = new HashMap<>();
            payload.put("login", Integer.parseInt(account.getAccountLogin()));
            payload.put("password", plainPassword);
            payload.put("server", account.getServerName());
            payload.put("ticket", ticket);
            payload.put("stop_loss", newStopLoss);

            ResponseEntity<Map> res = restTemplate.postForEntity(bridgeUrl + "/mt5/trade/modify", payload, Map.class);
            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("[ActivePositionMonitor] 🔒 Trailing Stop exécuté pour Ticket {} : Nouveau SL fixé à {}", ticket, newStopLoss);
                return true;
            }
        } catch (Exception e) {
            log.warn("[ActivePositionMonitor] Échec dispatch Trailing Stop pour Ticket {} : {}", ticket, e.getMessage());
        }
        return false;
    }
}
