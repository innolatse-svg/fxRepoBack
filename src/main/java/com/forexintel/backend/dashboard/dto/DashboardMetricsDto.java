package com.forexintel.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO des métriques agrégées du tableau de bord.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private Double accountBalance;
    private Double accountEquity;
    private Double dailyProfitDollar;
    private Double dailyProfitPct;
    private Double currentExposurePct;
    private Double maxExposureLimitPct;
    private Double consumedDailyLossPct;
    private Double maxDailyLossLimitPct;
    private Integer openPositionsCount;
    private Integer maxPositionsLimit;
    private String circuitBreakerStatus;
}
