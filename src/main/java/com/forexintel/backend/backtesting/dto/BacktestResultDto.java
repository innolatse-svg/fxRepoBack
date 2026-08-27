package com.forexintel.backend.backtesting.dto;

import com.forexintel.backend.backtesting.model.ExecutedBacktestTrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO complet des résultats d'un backtest avec métriques institutionnelles et courbe d'équité.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResultDto {
    private String strategyId;
    private String strategyName;
    private Double initialCapital;
    private Double finalCapital;
    private Double netProfitDollar;
    private Double netProfitPct;
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private Double winRate;
    private Double profitFactor;
    private Double maxDrawdownPct;
    private Double sharpeRatio;
    private Double expectancyR;
    private List<EquityPointDto> equityCurve;
    private List<ExecutedBacktestTrade> trades;
}
