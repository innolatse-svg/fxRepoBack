package com.forexintel.backend.backtesting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Modèle de données d'une bougie OHLCV pour les simulations de backtest.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private Instant timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
}
