package com.forexintel.backend.marketdata.controller;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.marketdata.dto.QuoteTickDto;
import com.forexintel.backend.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contrôleur REST pour l'accès aux données de marché en direct soumises au Data Gating.
 *
 * @author Innocent
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final UserRepository userRepository;

    /**
     * Récupère la cotation en temps réel d'un instrument via le compte MT5 du trader.
     */
    @GetMapping("/rates")
    public ResponseEntity<QuoteTickDto> getRates(
            Authentication authentication,
            @RequestParam(defaultValue = "EUR/USD") String symbol
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        QuoteTickDto quote = marketDataService.getGatedQuote(user.getId(), symbol);
        return ResponseEntity.ok(quote);
    }
}
