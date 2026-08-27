package com.forexintel.backend.backtesting.controller;

import com.forexintel.backend.backtesting.dto.BacktestRequestDto;
import com.forexintel.backend.backtesting.dto.BacktestResultDto;
import com.forexintel.backend.backtesting.service.BacktestEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour le Laboratoire de Backtesting & simulations quantitatives.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/backtesting")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestEngineService backtestEngineService;

    /**
     * Lance une simulation de backtesting complète sur historique de marché.
     */
    @PostMapping("/run")
    public ResponseEntity<BacktestResultDto> runBacktest(@RequestBody BacktestRequestDto request) {
        BacktestResultDto result = backtestEngineService.runBacktest(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Liste les stratégies algorithmiques disponibles pour simulation.
     */
    @GetMapping("/strategies")
    public ResponseEntity<List<Map<String, String>>> getStrategies() {
        List<Map<String, String>> list = backtestEngineService.getAvailableStrategies();
        return ResponseEntity.ok(list);
    }
}
