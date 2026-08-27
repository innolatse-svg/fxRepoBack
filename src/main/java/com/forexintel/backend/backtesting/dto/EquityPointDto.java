package com.forexintel.backend.backtesting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Point temporel de la courbe d'équité pour le tracé graphique.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquityPointDto {
    private Instant timestamp;
    private Double equity;
    private Double drawdownPct;
}
