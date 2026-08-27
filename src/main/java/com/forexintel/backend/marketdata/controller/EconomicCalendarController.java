package com.forexintel.backend.marketdata.controller;

import com.forexintel.backend.marketdata.dto.EconomicEventDto;
import com.forexintel.backend.marketdata.service.EconomicCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST pour la consultation du calendrier macroéconomique.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/market/calendar")
@RequiredArgsConstructor
public class EconomicCalendarController {

    private final EconomicCalendarService economicCalendarService;

    @GetMapping
    public ResponseEntity<List<EconomicEventDto>> getCalendar() {
        return ResponseEntity.ok(economicCalendarService.getWeeklyCalendar());
    }
}
