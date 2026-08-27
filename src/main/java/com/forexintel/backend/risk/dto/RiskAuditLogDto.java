package com.forexintel.backend.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO pour l'exposition des enregistrements du journal d'audit du Risk Engine.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAuditLogDto {
    private UUID id;
    private String symbol;
    private String actionType;
    private Double requestedRiskPct;
    private Double lotSize;
    private String decision;
    private String reason;
    private OffsetDateTime createdAt;
}
