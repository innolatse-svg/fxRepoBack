package com.forexintel.backend.marketdata.service;

import com.forexintel.backend.marketdata.dto.QuoteTickDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moteur de simulation de marché temps réel (Étape 4 - Mock Market Engine).
 * Diffuse des cotations réalistes sur le topic WebSocket STOMP /topic/quotes.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockMarketEngine {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, QuoteTickDto> marketState = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @PostConstruct
    public void initMarket() {
        registerPair("EUR/USD", "Euro / US Dollar", "FOREX", 1.08450, 0.8, 5, 0.0001, "BULLISH", "STRONG_BUY", 88);
        registerPair("GBP/USD", "British Pound / US Dollar", "FOREX", 1.29120, 1.1, 5, 0.0001, "NEUTRAL", "NEUTRAL", 64);
        registerPair("USD/JPY", "US Dollar / Japanese Yen", "FOREX", 154.680, 0.9, 3, 0.01, "BEARISH", "SELL", 82);
        registerPair("USD/CAD", "US Dollar / Canadian Dollar", "FOREX", 1.35400, 0.9, 5, 0.0001, "NEUTRAL", "NEUTRAL", 70);
        registerPair("AUD/USD", "Australian Dollar / US Dollar", "FOREX", 0.65800, 1.0, 5, 0.0001, "BULLISH", "BUY", 76);
        registerPair("USD/CHF", "US Dollar / Swiss Franc", "FOREX", 0.88450, 0.8, 5, 0.0001, "NEUTRAL", "NEUTRAL", 68);
        registerPair("EUR/GBP", "Euro / British Pound", "FOREX", 0.85600, 0.8, 5, 0.0001, "NEUTRAL", "NEUTRAL", 65);
        registerPair("EUR/JPY", "Euro / Japanese Yen", "FOREX", 167.750, 1.1, 3, 0.01, "BEARISH", "SELL", 78);
        registerPair("GBP/JPY", "British Pound / Japanese Yen", "FOREX", 199.650, 1.4, 3, 0.01, "BEARISH", "SELL", 80);
        registerPair("XAU/USD", "Gold Spot / US Dollar", "COMMODITY", 2845.50, 3.5, 2, 0.1, "BULLISH", "STRONG_BUY", 91);
        registerPair("BTC/USD", "Bitcoin / US Dollar", "CRYPTO", 77950.00, 5.0, 2, 1.0, "BULLISH", "STRONG_BUY", 89);
        log.info("[MarketEngine] Initialisation de {} instruments de trading terminée", marketState.size());
    }

    private void registerPair(String symbol, String name, String category, double initialBid,
                              double spreadPips, int digits, double pipSize,
                              String bias, String trend, int aiConfidence) {
        double ask = round(initialBid + (spreadPips * pipSize), digits);
        List<Double> sparkline = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            sparkline.add(round(initialBid - (i * 0.0002), digits));
        }

        QuoteTickDto quote = QuoteTickDto.builder()
                .symbol(symbol)
                .name(name)
                .category(category)
                .bid(initialBid)
                .ask(ask)
                .spread(spreadPips)
                .change24h(0.35)
                .high24h(round(initialBid * 1.004, digits))
                .low24h(round(initialBid * 0.996, digits))
                .digits(digits)
                .pipSize(pipSize)
                .bias(bias)
                .trend(trend)
                .aiConfidence(aiConfidence)
                .lastTickDirection("UP")
                .sparkline(sparkline)
                .timestamp(Instant.now())
                .build();

        marketState.put(symbol, quote);
    }

    /**
     * Diffusion périodique des cotations de marché toutes les secondes.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastMarketTicks() {
        List<QuoteTickDto> updatedQuotes = new ArrayList<>();

        for (Map.Entry<String, QuoteTickDto> entry : marketState.entrySet()) {
            QuoteTickDto quote = entry.getValue();

            // Micro fluctuation aléatoire
            int directionSign = random.nextBoolean() ? 1 : -1;
            double deltaPips = (random.nextDouble() * 0.4 + 0.1) * directionSign;
            double newBid = round(quote.getBid() + (deltaPips * quote.getPipSize()), quote.getDigits());
            double newAsk = round(newBid + (quote.getSpread() * quote.getPipSize()), quote.getDigits());

            String tickDirection = newBid >= quote.getBid() ? "UP" : "DOWN";

            List<Double> spark = new ArrayList<>(quote.getSparkline());
            if (spark.size() >= 10) {
                spark.remove(0);
            }
            spark.add(newBid);

            quote.setBid(newBid);
            quote.setAsk(newAsk);
            quote.setLastTickDirection(tickDirection);
            quote.setSparkline(spark);
            quote.setTimestamp(Instant.now());

            updatedQuotes.add(quote);
        }

        // Publication sur le topic WebSocket STOMP
        messagingTemplate.convertAndSend("/topic/quotes", updatedQuotes);
    }

    /**
     * Récupère la liste courante de toutes les cotations.
     */
    public List<QuoteTickDto> getAllQuotes() {
        return new ArrayList<>(marketState.values());
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
