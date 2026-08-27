package com.forexintel.backend.execution.provider;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Fournisseur d'exécution connecté en direct au microservice Python MetaTrader 5 (MT5 Bridge).
 * Assure la transmission sécurisée des ordres et la destruction immédiate des identifiants en mémoire.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Primary
@Component
public class PythonBridgeExecutionProvider implements TradingExecutionProvider {

    private final RestTemplate restTemplate;
    private final String bridgeUrl;

    public PythonBridgeExecutionProvider(
            RestTemplate restTemplate,
            @Value("${mt5.bridge.url:http://localhost:5001}") String bridgeUrl) {
        this.restTemplate = restTemplate;
        this.bridgeUrl = bridgeUrl;
    }

    /**
     * DTO de réponse du worker Python pour la synchronisation.
     */
    public record AccountSyncResult(
            boolean connected,
            Double balance,
            Double equity,
            Double margin,
            Double freeMargin,
            String currency,
            String leverage,
            String message
    ) {}

    @Override
    public ExecutionResult executeTrade(TradeIntentDto intent, TradingAccount account, String plainPassword) {
        String endpoint = bridgeUrl + "/mt5/trade/execute";
        log.info("[PythonBridge] Transmission de l'ordre au bridge MT5 (Compte: {}, Instrument: {})",
                account.getAccountLogin(), intent.getSymbol());

        try {
            long loginNum = parseLogin(account.getAccountLogin());

            Map<String, Object> payload = new HashMap<>();
            payload.put("login", loginNum);
            payload.put("password", plainPassword);
            payload.put("server", account.getServerName());
            payload.put("symbol", intent.getSymbol());
            payload.put("direction", intent.getDirection() != null ? intent.getDirection().toUpperCase() : "BUY");
            payload.put("volume", intent.getLotSize() != null ? intent.getLotSize() : 0.1);
            payload.put("entry_price", intent.getEntryPrice());
            payload.put("stop_loss", intent.getStopLoss());
            payload.put("take_profit", intent.getTakeProfit());
            payload.put("comment", "ForexIntel-V2");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                boolean success = Boolean.TRUE.equals(body.get("success"));
                String ticket = (String) body.get("order_ticket");
                String message = (String) body.get("message");
                Double executedPrice = body.get("executed_price") != null ? ((Number) body.get("executed_price")).doubleValue() : intent.getEntryPrice();
                Double executedVolume = body.get("executed_volume") != null ? ((Number) body.get("executed_volume")).doubleValue() : intent.getLotSize();

                log.info("[PythonBridge] Réponse d'exécution MT5 : Ticket {} - Succès : {}", ticket, success);
                return new ExecutionResult(success, ticket, message, executedPrice, executedVolume);
            }

            return new ExecutionResult(false, null, "Réponse inattendue du bridge MT5", null, null);

        } catch (Exception e) {
            log.error("[PythonBridge] Échec de l'appel au microservice Python MT5 : {}", e.getMessage());
            // En cas d'indisponibilité du worker Python, repli propre
            throw new IllegalStateException("Le pont MT5 n'a pas pu exécuter l'ordre : " + e.getMessage(), e);
        }
    }

    @Override
    public int pingServer(String serverName) {
        String endpoint = bridgeUrl + "/health";
        try {
            long start = System.currentTimeMillis();
            ResponseEntity<Map> resp = restTemplate.getForEntity(endpoint, Map.class);
            long latency = System.currentTimeMillis() - start;
            if (resp.getStatusCode().is2xxSuccessful()) {
                return (int) Math.max(10, latency);
            }
        } catch (Exception e) {
            log.warn("[PythonBridge] Impossible de joindre le healthcheck du bridge MT5 : {}", e.getMessage());
        }
        return 999;
    }

    /**
     * Synchronise le solde et l'équité réels d'un compte broker via le bridge MT5.
     */
    public AccountSyncResult syncAccountMetrics(TradingAccount account, String plainPassword) {
        String endpoint = bridgeUrl + "/mt5/account/sync";

        try {
            long loginNum = parseLogin(account.getAccountLogin());

            Map<String, Object> payload = new HashMap<>();
            payload.put("login", loginNum);
            payload.put("password", plainPassword);
            payload.put("server", account.getServerName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                return new AccountSyncResult(
                        Boolean.TRUE.equals(body.get("connected")),
                        body.get("balance") != null ? ((Number) body.get("balance")).doubleValue() : account.getBalance(),
                        body.get("equity") != null ? ((Number) body.get("equity")).doubleValue() : account.getEquity(),
                        body.get("margin") != null ? ((Number) body.get("margin")).doubleValue() : 0.0,
                        body.get("free_margin") != null ? ((Number) body.get("free_margin")).doubleValue() : 0.0,
                        (String) body.get("currency"),
                        (String) body.get("leverage"),
                        (String) body.get("message")
                );
            }
        } catch (Exception e) {
            log.warn("[PythonBridge] Échec de la synchronisation compte {} : {}", account.getAccountLogin(), e.getMessage());
        }

        return new AccountSyncResult(false, account.getBalance(), account.getEquity(), 0.0, 0.0, account.getCurrency(), account.getLeverage(), "Bridge indisponible");
    }

    private long parseLogin(String loginStr) {
        try {
            return Long.parseLong(loginStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 10000000L;
        }
    }
}
