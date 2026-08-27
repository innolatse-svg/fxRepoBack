package com.forexintel.backend.execution.provider;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

/**
 * Implémentation simulée du fournisseur d'exécution de trades (Étape 6 Mock Bridge).
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Component
public class MockExecutionProvider implements TradingExecutionProvider {

    private final Random random = new Random();

    @Override
    public ExecutionResult executeTrade(TradeIntentDto intent, TradingAccount account, String plainPassword) {
        log.info("[MockExecutionProvider] Routage de l'ordre vers le serveur {} pour le compte {}",
                account.getServerName(), account.getAccountLogin());

        // Simulation de latence réseau MT5
        String ticket = "MT5-" + Math.abs(random.nextLong() % 100000000);
        double executedPrice = intent.getEntryPrice() != null ? intent.getEntryPrice() : 1.0845;
        double executedVolume = intent.getLotSize() != null ? intent.getLotSize() : 0.1;

        log.info("[MockExecutionProvider] Ordre exécuté avec succès : Ticket {} à {}", ticket, executedPrice);

        return new ExecutionResult(
                true,
                ticket,
                "Ordre exécuté avec succès sur " + account.getBrokerName(),
                executedPrice,
                executedVolume
        );
    }

    @Override
    public int pingServer(String serverName) {
        return Math.floorMod(random.nextInt(), 30) + 15; // 15ms - 45ms
    }
}
