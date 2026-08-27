package com.forexintel.backend.execution.provider;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.risk.dto.TradeIntentDto;

/**
 * Interface d'abstraction pour l'exécution d'ordres vers les passerelles brokers (MT5, MetaApi, cTrader, ZMQ).
 *
 * @author Innocent
 * @version 1.0.0
 */
public interface TradingExecutionProvider {

    /**
     * Résultat d'une tentative d'exécution d'ordre sur le marché broker.
     */
    record ExecutionResult(
            boolean success,
            String orderTicket,
            String message,
            Double executedPrice,
            Double executedVolume
    ) {}

    /**
     * Exécute un ordre de trading sur le compte cible spécifié.
     *
     * @param intent Paramètres validés du trade
     * @param account Compte broker cible
     * @param plainPassword Mot de passe déchiffré à la volée depuis le Vault
     * @return Résultat de la transaction
     */
    ExecutionResult executeTrade(TradeIntentDto intent, TradingAccount account, String plainPassword);

    /**
     * Teste la connectivité et le ping avec le serveur broker.
     */
    int pingServer(String serverName);
}
