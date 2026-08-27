package com.forexintel.backend.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO d'un événement macroéconomique du calendrier avec note contextuelle IA.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EconomicEventDto {
    private String id;
    private String time;
    private String currency;
    private String countryCode;
    private String title;
    private String impact; // HIGH / MEDIUM / LOW
    private String actual;
    private String forecast;
    private String previous;
    private List<String> affectedPairs;
    private Integer historicalPipMove;
    private String aiNote;
}
