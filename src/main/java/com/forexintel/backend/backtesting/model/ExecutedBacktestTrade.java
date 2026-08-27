package com.forexintel.backend.backtesting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modèle représentant un trade simulé lors d'un backtest.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutedBacktestTrade {
    private String id;
    private Instant entryTime;
    private Instant exitTime;
    private String symbol;
    private String direction; // BUY / SELL
    private double entryPrice;
    private double exitPrice;
    private double stopLoss;
    private double takeProfit;
    private double lotSize;
    private double pnlDollar;
    private double rMultiple;
    private String outcome; // WIN / LOSS / BREAKEVEN
    private String duration;
}
