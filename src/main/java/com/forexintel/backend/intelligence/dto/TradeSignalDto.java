package com.forexintel.backend.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO d'exposition des signaux de trading IA avec données d'explicabilité (4 piliers de confluence).
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSignalDto {
    private UUID id;
    private String symbol;
    private String direction;
    private String timeframe;
    private Integer alignmentScore;
    private Double entryPrice;
    private Double stopLoss;
    private Double takeProfit;
    private String riskRewardRatio;
    private String status;
    private Map<String, Object> confluence;
    private OffsetDateTime timestamp;
}
