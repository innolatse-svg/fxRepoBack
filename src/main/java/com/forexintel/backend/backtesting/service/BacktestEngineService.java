package com.forexintel.backend.backtesting.service;

import com.forexintel.backend.backtesting.dto.BacktestRequestDto;
import com.forexintel.backend.backtesting.dto.BacktestResultDto;
import com.forexintel.backend.backtesting.dto.EquityPointDto;
import com.forexintel.backend.backtesting.model.Candle;
import com.forexintel.backend.backtesting.model.ExecutedBacktestTrade;
import com.forexintel.backend.backtesting.strategy.BacktestStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Moteur de calcul quantitatif et d'orchestration des backtests.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngineService {

    private final List<BacktestStrategy> strategies;
    private final HistoricalDataSeeder historicalDataSeeder;

    /**
     * Exécute une simulation complète et calcule les métriques institutionnelles.
     */
    public BacktestResultDto runBacktest(BacktestRequestDto request) {
        log.info("[BacktestEngine] Lancement simulation: Stratégie={}, Instrument={}, Capital={}, Risque={}%",
                request.getStrategyId(), request.getSymbol(), request.getInitialCapital(), request.getRiskPerTradePct());

        // 1. Validation de la stratégie
        BacktestStrategy strategy = strategies.stream()
                .filter(s -> s.getId().equalsIgnoreCase(request.getStrategyId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stratégie inconnue : " + request.getStrategyId()));

        double capital = (request.getInitialCapital() != null && request.getInitialCapital() > 0)
                ? request.getInitialCapital() : 10000.0;
        double riskPct = (request.getRiskPerTradePct() != null && request.getRiskPerTradePct() > 0)
                ? request.getRiskPerTradePct() : 1.0;
        String symbol = (request.getSymbol() != null && !request.getSymbol().isBlank())
                ? request.getSymbol() : "EUR/USD";

        // 2. Génération de l'historique de marché
        int candleCount = mapPeriodToCandleCount(request.getPeriod());
        double basePrice = symbol.contains("JPY") ? 154.50 : (symbol.contains("XAU") ? 2850.0 : 1.0850);
        List<Candle> candles = historicalDataSeeder.generateHistoricalCandles(symbol, candleCount, basePrice);

        // 3. Exécution de la stratégie
        List<ExecutedBacktestTrade> trades = strategy.backtest(candles, capital, riskPct);

        // 4. Calculs mathématiques & KPIs
        return computePerformanceMetrics(strategy, capital, trades);
    }

    /**
     * Calcule avec rigueur mathématique tous les indicateurs clés et la courbe d'équité.
     */
    public BacktestResultDto computePerformanceMetrics(
            BacktestStrategy strategy,
            double initialCapital,
            List<ExecutedBacktestTrade> trades) {

        if (trades == null || trades.isEmpty()) {
            return BacktestResultDto.builder()
                    .strategyId(strategy.getId())
                    .strategyName(strategy.getName())
                    .initialCapital(initialCapital)
                    .finalCapital(initialCapital)
                    .netProfitDollar(0.0)
                    .netProfitPct(0.0)
                    .totalTrades(0)
                    .winningTrades(0)
                    .losingTrades(0)
                    .winRate(0.0)
                    .profitFactor(0.0)
                    .maxDrawdownPct(0.0)
                    .sharpeRatio(0.0)
                    .expectancyR(0.0)
                    .equityCurve(List.of(new EquityPointDto(Instant.now(), initialCapital, 0.0)))
                    .trades(List.of())
                    .build();
        }

        double currentEquity = initialCapital;
        double peakEquity = initialCapital;
        double maxDrawdownPct = 0.0;

        double grossProfit = 0.0;
        double grossLoss = 0.0;
        int winCount = 0;
        int lossCount = 0;

        List<EquityPointDto> rawEquityCurve = new ArrayList<>();
        rawEquityCurve.add(new EquityPointDto(trades.get(0).getEntryTime(), initialCapital, 0.0));

        List<Double> returnsList = new ArrayList<>();

        for (ExecutedBacktestTrade trade : trades) {
            double pnl = trade.getPnlDollar();
            currentEquity += pnl;

            if (pnl > 0) {
                grossProfit += pnl;
                winCount++;
            } else if (pnl < 0) {
                grossLoss += Math.abs(pnl);
                lossCount++;
            }

            if (currentEquity > peakEquity) {
                peakEquity = currentEquity;
            }

            double currentDrawdown = ((peakEquity - currentEquity) / peakEquity) * 100.0;
            if (currentDrawdown > maxDrawdownPct) {
                maxDrawdownPct = currentDrawdown;
            }

            returnsList.add(pnl / (currentEquity - pnl));
            rawEquityCurve.add(new EquityPointDto(trade.getExitTime(), round(currentEquity, 2), round(currentDrawdown, 2)));
        }

        int totalTrades = trades.size();
        double winRate = (double) winCount / totalTrades * 100.0;
        double profitFactor = grossLoss > 0 ? (grossProfit / grossLoss) : grossProfit;
        double netProfitDollar = currentEquity - initialCapital;
        double netProfitPct = (netProfitDollar / initialCapital) * 100.0;

        // Ratio de Sharpe annualisé simplifié
        double sharpeRatio = calculateSharpeRatio(returnsList);

        // Espérance mathématique en R
        double avgWinR = winCount > 0 ? trades.stream().filter(t -> t.getPnlDollar() > 0).mapToDouble(ExecutedBacktestTrade::getRMultiple).average().orElse(2.0) : 0.0;
        double avgLossR = lossCount > 0 ? Math.abs(trades.stream().filter(t -> t.getPnlDollar() < 0).mapToDouble(ExecutedBacktestTrade::getRMultiple).average().orElse(1.0)) : 1.0;
        double winRateDecimal = winRate / 100.0;
        double expectancyR = (winRateDecimal * avgWinR) - ((1.0 - winRateDecimal) * avgLossR);

        // Échantillonnage de la courbe d'équité pour optimiser le rendu UI Canvas / SVG
        List<EquityPointDto> downsampledCurve = downsampleEquityCurve(rawEquityCurve, 100);

        return BacktestResultDto.builder()
                .strategyId(strategy.getId())
                .strategyName(strategy.getName())
                .initialCapital(round(initialCapital, 2))
                .finalCapital(round(currentEquity, 2))
                .netProfitDollar(round(netProfitDollar, 2))
                .netProfitPct(round(netProfitPct, 2))
                .totalTrades(totalTrades)
                .winningTrades(winCount)
                .losingTrades(lossCount)
                .winRate(round(winRate, 2))
                .profitFactor(round(profitFactor, 2))
                .maxDrawdownPct(round(maxDrawdownPct, 2))
                .sharpeRatio(round(sharpeRatio, 2))
                .expectancyR(round(expectancyR, 2))
                .equityCurve(downsampledCurve)
                .trades(trades)
                .build();
    }

    private double calculateSharpeRatio(List<Double> returns) {
        if (returns.size() < 2) return 1.5;
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return 0.0;
        // Annualisation approximative (racine de 252 jours)
        return (mean / stdDev) * Math.sqrt(252);
    }

    private List<EquityPointDto> downsampleEquityCurve(List<EquityPointDto> rawList, int maxPoints) {
        if (rawList.size() <= maxPoints) {
            return rawList;
        }
        List<EquityPointDto> downsampled = new ArrayList<>();
        downsampled.add(rawList.get(0));

        double step = (double) (rawList.size() - 2) / (maxPoints - 2);
        for (int i = 1; i < maxPoints - 1; i++) {
            int index = (int) Math.round(i * step);
            downsampled.add(rawList.get(Math.min(index, rawList.size() - 2)));
        }

        downsampled.add(rawList.get(rawList.size() - 1));
        return downsampled;
    }

    private int mapPeriodToCandleCount(String period) {
        if (period == null) return 2000;
        return switch (period.toUpperCase()) {
            case "1Y" -> 2000;
            case "3Y" -> 4500;
            case "5Y" -> 7000;
            default -> 2500;
        };
    }

    private double round(double val, int places) {
        double factor = Math.pow(10, places);
        return Math.round(val * factor) / factor;
    }

    public List<Map<String, String>> getAvailableStrategies() {
        return strategies.stream()
                .map(s -> Map.of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "description", s.getDescription()
                ))
                .toList();
    }
}
