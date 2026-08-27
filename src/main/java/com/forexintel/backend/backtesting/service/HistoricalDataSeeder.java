package com.forexintel.backend.backtesting.service;

import com.forexintel.backend.backtesting.model.Candle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Générateur de séries temporelles de marché haute fidélité pour les simulations quantitatives.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Service
public class HistoricalDataSeeder {

    private final Random random = new Random(42); // Graine fixe pour reproductibilité mathématique

    /**
     * Génère une série de bougies OHLC historique en mémoire.
     *
     * @param symbol Paire de devises (ex: EUR/USD)
     * @param candleCount Nombre de bougies à générer (ex: 5 000)
     * @param basePrice Prix initial de référence
     * @return Liste ordonnée chronologiquement de bougies
     */
    public List<Candle> generateHistoricalCandles(String symbol, int candleCount, double basePrice) {
        List<Candle> series = new ArrayList<>(candleCount);
        Instant startTime = Instant.now().minus(candleCount * 4L, ChronoUnit.HOURS);

        double currentPrice = basePrice;
        double pipSize = symbol.contains("JPY") ? 0.01 : 0.0001;
        double baseAtr = pipSize * 25;

        for (int i = 0; i < candleCount; i++) {
            Instant candleTime = startTime.plus(i * 4L, ChronoUnit.HOURS);

            // Modélisation cyclique sinusoïdale + micro-bruit gaussien
            double wave = Math.sin(i / 35.0) * 0.15;
            double drift = (random.nextGaussian() * 0.5 + wave) * baseAtr;

            double open = currentPrice;
            double close = open + drift;
            double high = Math.max(open, close) + Math.abs(random.nextGaussian() * 0.4 * baseAtr);
            double low = Math.min(open, close) - Math.abs(random.nextGaussian() * 0.4 * baseAtr);
            double volume = Math.floor(1000 + random.nextDouble() * 5000);

            series.add(Candle.builder()
                    .timestamp(candleTime)
                    .open(round(open, 5))
                    .high(round(high, 5))
                    .low(round(low, 5))
                    .close(round(close, 5))
                    .volume(volume)
                    .build());

            currentPrice = close;
        }

        return series;
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
