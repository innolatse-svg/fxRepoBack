package com.forexintel.backend.marketdata.service;

import com.forexintel.backend.marketdata.dto.EconomicEventDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service fournissant le calendrier des annonces macroéconomiques à fort impact.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Service
public class EconomicCalendarService {

    /**
     * Renvoie les événements macroéconomiques programmés de la semaine avec annotations IA.
     */
    public List<EconomicEventDto> getWeeklyCalendar() {
        return List.of(
                EconomicEventDto.builder()
                        .id("ev-1")
                        .time("14:30 GMT+1")
                        .currency("USD")
                        .countryCode("US")
                        .title("Indice des Prix à la Consommation (Core CPI YoY)")
                        .impact("HIGH")
                        .forecast("2.9%")
                        .previous("3.1%")
                        .affectedPairs(List.of("EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD"))
                        .historicalPipMove(65)
                        .aiNote("Volatilité extrême attendue. Une publication sous le consensus (<2.8%) affaiblira fortement le dollar en augmentant les anticipations de baisse de taux Fed.")
                        .build(),
                EconomicEventDto.builder()
                        .id("ev-2")
                        .time("15:45 GMT+1")
                        .currency("USD")
                        .countryCode("US")
                        .title("PMI Manufacturier S&P Global")
                        .impact("MEDIUM")
                        .forecast("50.8")
                        .previous("50.5")
                        .affectedPairs(List.of("USD/CAD", "EUR/USD"))
                        .historicalPipMove(25)
                        .aiNote("Baromètre d'activité industrielle. Au-dessus de 50 = expansion économique.")
                        .build(),
                EconomicEventDto.builder()
                        .id("ev-3")
                        .time("10:00 GMT+1")
                        .currency("EUR")
                        .countryCode("EU")
                        .title("Indice ZEW du Sentiment Économique Allemand")
                        .impact("MEDIUM")
                        .actual("15.2")
                        .forecast("14.0")
                        .previous("12.8")
                        .affectedPairs(List.of("EUR/USD", "EUR/GBP", "EUR/JPY"))
                        .historicalPipMove(30)
                        .aiNote("Surprise positive (+15.2 vs 14.0) soutenant l'euro sur les paires croisées.")
                        .build(),
                EconomicEventDto.builder()
                        .id("ev-4")
                        .time("Demain 08:00")
                        .currency("GBP")
                        .countryCode("GB")
                        .title("PIB Mensuel (MoM)")
                        .impact("HIGH")
                        .forecast("0.2%")
                        .previous("0.1%")
                        .affectedPairs(List.of("GBP/USD", "EUR/GBP"))
                        .historicalPipMove(45)
                        .aiNote("Chiffre clé pour la trajectoire monétaire de la Bank of England (BoE).")
                        .build(),
                EconomicEventDto.builder()
                        .id("ev-5")
                        .time("Demain 14:30")
                        .currency("USD")
                        .countryCode("US")
                        .title("Inscriptions Hebdomadaires au Chômage (Jobless Claims)")
                        .impact("HIGH")
                        .forecast("215K")
                        .previous("220K")
                        .affectedPairs(List.of("EUR/USD", "USD/JPY"))
                        .historicalPipMove(35)
                        .aiNote("Mesure hebdomadaire de la tension sur le marché du travail US.")
                        .build()
        );
    }
}
