package com.forexintel.backend.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO décrivant une intention de passage d'ordre à soumettre au Risk Engine.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeIntentDto {
    private String symbol;
    private String direction;
    private Double lotSize;
    private Double entryPrice;
    private Double stopLoss;
    private Double takeProfit;
    private Double requestedRiskPct;
    private Double accountBalance;
    private Integer currentOpenPositions;
    private Double currentExposurePct;
    private Double currentDailyLossPct;
}
