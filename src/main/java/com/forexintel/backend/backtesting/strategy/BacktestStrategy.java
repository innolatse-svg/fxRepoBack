package com.forexintel.backend.backtesting.strategy;

import com.forexintel.backend.backtesting.model.Candle;
import com.forexintel.backend.backtesting.model.ExecutedBacktestTrade;

import java.util.List;

/**
 * Interface générique pour l'implémentation de stratégies algorithmiques de backtesting.
 *
 * @author Innocent
 * @version 1.0.0
 */
public interface BacktestStrategy {

    String getId();
    String getName();
    String getDescription();

    /**
     * Exécute la logique de trading sur la série temporelle de bougies.
     *
     * @param historicalData Série de bougies OHLC chronologiques
     * @param initialCapital Capital de départ en USD
     * @param riskPerTradePct Pourcentage de risque par trade
     * @return Liste des trades exécutés
     */
    List<ExecutedBacktestTrade> backtest(List<Candle> historicalData, double initialCapital, double riskPerTradePct);
}
