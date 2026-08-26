package com.forexintel.backend.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO représentant un tick de cotation de marché temps réel.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteTickDto {
    private String symbol;
    private String name;
    private String category;
    private Double bid;
    private Double ask;
    private Double spread;
    private Double change24h;
    private Double high24h;
    private Double low24h;
    private Integer digits;
    private Double pipSize;
    private String bias;
    private String trend;
    private Integer aiConfidence;
    private String lastTickDirection;
    private List<Double> sparkline;
    private Instant timestamp;
}
