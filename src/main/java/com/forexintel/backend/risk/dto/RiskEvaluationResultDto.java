package com.forexintel.backend.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO portant le verdict d'évaluation du Risk Engine pour une intention de trade.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResultDto {
    private String decision;
    private boolean allowed;
    private String reason;
    private Double effectiveRiskPct;
    private String appliedCeiling;
    private Double maxAllowedLotSize;
}
