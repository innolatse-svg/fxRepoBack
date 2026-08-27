package com.forexintel.backend.backtesting.service;

import com.forexintel.backend.backtesting.dto.BacktestRequestDto;
import com.forexintel.backend.backtesting.dto.BacktestResultDto;
import com.forexintel.backend.backtesting.model.ExecutedBacktestTrade;
import com.forexintel.backend.backtesting.strategy.BacktestStrategy;
import com.forexintel.backend.backtesting.strategy.SmcOrderFlowStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires et de validité mathématique du moteur de Backtesting.
 *
 * @author Innocent
 */
class BacktestEngineServiceTest {

    private BacktestEngineService backtestEngineService;
    private SmcOrderFlowStrategy smcStrategy;

    @BeforeEach
    void setUp() {
        smcStrategy = new SmcOrderFlowStrategy();
        HistoricalDataSeeder seeder = new HistoricalDataSeeder();
        backtestEngineService = new BacktestEngineService(List.of(smcStrategy), seeder);
    }

    @Test
    @DisplayName("Devrait calculer avec une exactitude mathématique absolue le Net Profit et le Win Rate")
    void computePerformanceMetrics_ShouldBeMathematicallyExact() {
        // Given
        double initialCapital = 10000.0;
        List<ExecutedBacktestTrade> trades = List.of(
                ExecutedBacktestTrade.builder().id("1").entryTime(Instant.now()).exitTime(Instant.now()).pnlDollar(250.0).rMultiple(2.5).outcome("WIN").build(),
                ExecutedBacktestTrade.builder().id("2").entryTime(Instant.now()).exitTime(Instant.now()).pnlDollar(-100.0).rMultiple(-1.0).outcome("LOSS").build(),
                ExecutedBacktestTrade.builder().id("3").entryTime(Instant.now()).exitTime(Instant.now()).pnlDollar(300.0).rMultiple(3.0).outcome("WIN").build(),
                ExecutedBacktestTrade.builder().id("4").entryTime(Instant.now()).exitTime(Instant.now()).pnlDollar(-100.0).rMultiple(-1.0).outcome("LOSS").build()
        );

        // When
        BacktestResultDto result = backtestEngineService.computePerformanceMetrics(smcStrategy, initialCapital, trades);

        // Then
        // Net profit = 250 - 100 + 300 - 100 = 350
        assertEquals(10350.0, result.getFinalCapital());
        assertEquals(350.0, result.getNetProfitDollar());
        assertEquals(3.5, result.getNetProfitPct());

        // Total trades = 4, Wins = 2, Losses = 2 -> Win Rate = 50.0%
        assertEquals(4, result.getTotalTrades());
        assertEquals(2, result.getWinningTrades());
        assertEquals(2, result.getLosingTrades());
        assertEquals(50.0, result.getWinRate());

        // Profit Factor = Gross Profit (550) / Gross Loss (200) = 2.75
        assertEquals(2.75, result.getProfitFactor());
        assertNotNull(result.getEquityCurve());
        assertFalse(result.getEquityCurve().isEmpty());
    }

    @Test
    @DisplayName("Devrait exécuter une simulation complète avec 5000 bougies sans erreur de calcul")
    void runBacktest_ShouldExecuteSimulationEndToEnd() {
        BacktestRequestDto request = BacktestRequestDto.builder()
                .strategyId("smc-orderflow")
                .symbol("EUR/USD")
                .period("3Y")
                .initialCapital(10000.0)
                .riskPerTradePct(1.0)
                .build();

        BacktestResultDto result = backtestEngineService.runBacktest(request);

        assertNotNull(result);
        assertEquals("smc-orderflow", result.getStrategyId());
        assertTrue(result.getTotalTrades() > 0);
        assertTrue(result.getWinRate() > 0);
        assertTrue(result.getProfitFactor() > 0);
        assertTrue(result.getEquityCurve().size() <= 100, "La courbe doit être optimisée à 100 points maximum pour l'UI");
    }
}
