package com.forexintel.backend.intelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de demande d'exécution d'un signal vers un compte de trading.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSignalRequestDto {
    private UUID accountId;
    private Double lotSize;
}
