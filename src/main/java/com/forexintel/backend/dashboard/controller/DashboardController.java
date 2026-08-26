package com.forexintel.backend.dashboard.controller;

import com.forexintel.backend.dashboard.dto.DashboardMetricsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST exposant les indicateurs clés et métriques du Dashboard.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /**
     * Récupère les métriques globales du compte de trading simulé.
     *
     * @return DTO contenant le solde, équité, PnL et limites de risque
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDto> getMetrics() {
        DashboardMetricsDto metrics = DashboardMetricsDto.builder()
                .accountBalance(10000.00)
                .accountEquity(10080.35)
                .dailyProfitDollar(80.35)
                .dailyProfitPct(0.80)
                .currentExposurePct(1.85)
                .maxExposureLimitPct(4.00)
                .consumedDailyLossPct(0.00)
                .maxDailyLossLimitPct(3.00)
                .openPositionsCount(2)
                .maxPositionsLimit(3)
                .circuitBreakerStatus("NORMAL")
                .build();

        return ResponseEntity.ok(metrics);
    }
}
