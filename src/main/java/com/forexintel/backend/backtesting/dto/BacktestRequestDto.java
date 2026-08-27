package com.forexintel.backend.backtesting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO portant les paramètres d'une simulation de backtest.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestRequestDto {
    private String strategyId;
    private String symbol;
    private String timeframe;
    private String period; // 1Y, 3Y, 5Y
    private Double initialCapital;
    private Double riskPerTradePct;
}
