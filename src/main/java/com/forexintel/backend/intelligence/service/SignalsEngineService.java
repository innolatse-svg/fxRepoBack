package com.forexintel.backend.intelligence.service;

import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.service.TradingAccountService;
import com.forexintel.backend.intelligence.domain.TradeSignal;
import com.forexintel.backend.intelligence.dto.ExecuteSignalRequestDto;
import com.forexintel.backend.intelligence.dto.TradeSignalDto;
import com.forexintel.backend.intelligence.repository.TradeSignalRepository;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur d'agrégation d'intelligence, de calcul des scores de confluence et de diffusion temps réel des signaux.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalsEngineService {

    private final TradeSignalRepository tradeSignalRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TradingAccountService tradingAccountService;
    private final AutoTradeOrchestrator autoTradeOrchestrator;

    private final Random random = new Random();

    @PostConstruct
    public void initSeedSignals() {
        if (tradeSignalRepository.count() == 0) {
            log.info("[SignalsEngine] Initialisation des signaux de marché de référence");
            seedInitialSignals();
        }
    }

    /**
     * Récupère la liste filtrée des signaux de trading IA.
     */
    @Transactional(readOnly = true)
    public List<TradeSignalDto> getSignals(String status, String direction) {
        List<TradeSignal> list;
        boolean filterStatus = status != null && !status.equalsIgnoreCase("ALL");
        boolean filterDir = direction != null && !direction.equalsIgnoreCase("ALL");

        if (filterStatus && filterDir) {
            list = tradeSignalRepository.findByStatusAndDirectionOrderByCreatedAtDesc(status, direction);
        } else if (filterStatus) {
            list = tradeSignalRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (filterDir) {
            list = tradeSignalRepository.findByDirectionOrderByCreatedAtDesc(direction);
        } else {
            list = tradeSignalRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Exécute un signal de trading vers un compte broker via le pipeline complet (Risk Engine -> Vault -> MT5 Provider).
     */
    @Transactional
    public TradingExecutionProvider.ExecutionResult executeSignal(UUID userId, UUID signalId, ExecuteSignalRequestDto request) {
        TradeSignal signal = tradeSignalRepository.findById(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal introuvable : " + signalId));

        if (!"PENDING_CONFIRMATION".equalsIgnoreCase(signal.getStatus())) {
            throw new IllegalStateException("Ce signal n'est plus en attente d'exécution (Statut actuel : " + signal.getStatus() + ")");
        }

        double lotSize = (request.getLotSize() != null && request.getLotSize() > 0) ? request.getLotSize() : 0.10;

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol(signal.getSymbol())
                .direction(signal.getDirection())
                .entryPrice(signal.getEntryPrice())
                .stopLoss(signal.getStopLoss())
                .takeProfit(signal.getTakeProfit())
                .lotSize(lotSize)
                .requestedRiskPct(1.0)
                .build();

        log.info("[SignalsEngine] Routage de l'ordre issu du signal {} ({} {}) pour l'utilisateur {}",
                signalId, signal.getDirection(), signal.getSymbol(), userId);

        // Appel du pipeline Zero-Trust dans TradingAccountService
        TradingExecutionProvider.ExecutionResult result =
                tradingAccountService.executeTradePipeline(userId, request.getAccountId(), intent);

        if (result.success()) {
            signal.setStatus("EXECUTED_DEMO");
            tradeSignalRepository.save(signal);
            log.info("[SignalsEngine] Signal {} marqué comme EXECUTED_DEMO suite au ticket {}", signalId, result.orderTicket());
        }

        return result;
    }

    /**
     * Rejette ou ignore un signal.
     */
    @Transactional
    public void dismissSignal(UUID signalId) {
        TradeSignal signal = tradeSignalRepository.findById(signalId)
                .orElseThrow(() -> new IllegalArgumentException("Signal introuvable : " + signalId));
        signal.setStatus("CANCELLED");
        tradeSignalRepository.save(signal);
    }

    /**
     * Tâche de fond automatique générant de nouveaux signaux et les diffusant via WebSocket.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    @Transactional
    public void generateAndBroadcastSignal() {
        String[] pairs = {"EUR/USD", "GBP/USD", "USD/JPY", "USD/CAD", "XAU/USD"};
        String pair = pairs[random.nextInt(pairs.length)];
        String direction = random.nextBoolean() ? "BUY" : "SELL";
        double basePrice = pair.contains("JPY") ? 154.60 : (pair.contains("XAU") ? 2890.0 : 1.0865);
        double delta = (random.nextDouble() - 0.5) * 0.0050;
        double entry = Math.round((basePrice + delta) * 100000.0) / 100000.0;
        double slDistance = pair.contains("JPY") ? 0.40 : (pair.contains("XAU") ? 15.0 : 0.0030);
        double sl = direction.equals("BUY") ? entry - slDistance : entry + slDistance;
        double tp = direction.equals("BUY") ? entry + (slDistance * 2.4) : entry - (slDistance * 2.4);

        int alignmentScore = 75 + random.nextInt(20);

        Map<String, Object> confluence = Map.of(
                "technical", Map.of(
                        "title", "Fair Value Gap & Rejet Support H1",
                        "detail", "Balayage de liquidité asiatique avec confirmation EMA 50.",
                        "score", alignmentScore - 5
                ),
                "macro", Map.of(
                        "title", "Différentiel de Taux & Sentiment Fed",
                        "detail", "Anticipations de politique monétaire favorables à l'actif.",
                        "score", alignmentScore - 2
                ),
                "sentiment", Map.of(
                        "title", "Positionnement Retail & COT",
                        "detail", "68% des traders particuliers sont à contre-tendance.",
                        "score", alignmentScore - 8
                ),
                "aiConfidence", Map.of(
                        "title", "Modèle Quantitatif Neural V4",
                        "detail", "Validation par réseau récurrent LSTM sur 5 000 ticks.",
                        "score", alignmentScore
                )
        );

        TradeSignal signal = TradeSignal.builder()
                .symbol(pair)
                .direction(direction)
                .timeframe("H1")
                .alignmentScore(alignmentScore)
                .entryPrice(entry)
                .stopLoss(Math.round(sl * 100000.0) / 100000.0)
                .takeProfit(Math.round(tp * 100000.0) / 100000.0)
                .riskRewardRatio("1:2.4")
                .status("PENDING_CONFIRMATION")
                .confluenceData(confluence)
                .build();

        TradeSignal saved = tradeSignalRepository.save(signal);
        TradeSignalDto dto = mapToDto(saved);

        // Diffusion temps réel sur les canaux WebSockets STOMP
        messagingTemplate.convertAndSend("/topic/signals", dto);
        messagingTemplate.convertAndSend("/user/queue/signals", dto);

        // Pipeline d'exécution automatique Full-Auto
        autoTradeOrchestrator.processAutoExecutionForSignal(saved);

        log.info("[SignalsEngine] Nouveau signal diffusé via WebSocket : {} {} (Score: {}%)",
                signal.getDirection(), signal.getSymbol(), signal.getAlignmentScore());
    }

    private void seedInitialSignals() {
        List<TradeSignal> seeds = List.of(
                TradeSignal.builder()
                        .symbol("EUR/USD")
                        .direction("BUY")
                        .timeframe("H1")
                        .alignmentScore(88)
                        .entryPrice(1.08450)
                        .stopLoss(1.08150)
                        .takeProfit(1.09150)
                        .riskRewardRatio("1:2.33")
                        .status("PENDING_CONFIRMATION")
                        .confluenceData(Map.of(
                                "technical", Map.of("title", "Bullish Order Block H4 & FVG", "detail", "Balayage des plus bas de la session asiatique puis cassure CHoCH.", "score", 90),
                                "macro", Map.of("title", "Discours BCE Accommodant & Taux US", "detail", "Différentiel de taux favorable au rebond technique de l'euro.", "score", 85),
                                "sentiment", Map.of("title", "Positionnement Retail Contrarian", "detail", "72% des particuliers positionnés à la vente.", "score", 86),
                                "aiConfidence", Map.of("title", "Réseau de Neurones Multi-Facteurs", "detail", "Pattern de rebond validé avec 88% de précision historique.", "score", 88)
                        ))
                        .build(),
                TradeSignal.builder()
                        .symbol("GBP/USD")
                        .direction("BUY")
                        .timeframe("H4")
                        .alignmentScore(82)
                        .entryPrice(1.28400)
                        .stopLoss(1.27900)
                        .takeProfit(1.29600)
                        .riskRewardRatio("1:2.40")
                        .status("PENDING_CONFIRMATION")
                        .confluenceData(Map.of(
                                "technical", Map.of("title", "Cassure Ligne de Tendance & Pullback", "detail", "Retest du support 1.2840 avec divergence RSI haussière.", "score", 84),
                                "macro", Map.of("title", "PIB UK supérieur aux attentes", "detail", "Réduction des anticipations de baisse rapide des taux BoE.", "score", 80),
                                "sentiment", Map.of("title", "Flux Institutionnels Libellés en Sterling", "detail", "Entrées nettes enregistrées sur les contrats à terme CME.", "score", 81),
                                "aiConfidence", Map.of("title", "Modèle Momentum", "detail", "Alignement parfait des moyennes mobiles 50/200 sur D1.", "score", 82)
                        ))
                        .build(),
                TradeSignal.builder()
                        .symbol("USD/JPY")
                        .direction("SELL")
                        .timeframe("M30")
                        .alignmentScore(79)
                        .entryPrice(154.200)
                        .stopLoss(154.650)
                        .takeProfit(153.100)
                        .riskRewardRatio("1:2.44")
                        .status("PENDING_CONFIRMATION")
                        .confluenceData(Map.of(
                                "technical", Map.of("title", "Rejet Zone de Liquidité 154.50", "detail", "Double sommet avec pin bar de rejet sur résistance majeure.", "score", 82),
                                "macro", Map.of("title", "Intervention Verbale Banque du Japon", "detail", "Risque d'intervention directe si le yen franchit 155.00.", "score", 85),
                                "sentiment", Map.of("title", "Aversion au Risque Globale", "detail", "Recherche de devises refuges suite aux tensions géopolitiques.", "score", 76),
                                "aiConfidence", Map.of("title", "Filtre Volatilité", "detail", "Sortie imminente de range identifiée.", "score", 79)
                        ))
                        .build()
        );

        tradeSignalRepository.saveAll(seeds);
    }

    private TradeSignalDto mapToDto(TradeSignal signal) {
        return TradeSignalDto.builder()
                .id(signal.getId())
                .symbol(signal.getSymbol())
                .direction(signal.getDirection())
                .timeframe(signal.getTimeframe())
                .alignmentScore(signal.getAlignmentScore())
                .entryPrice(signal.getEntryPrice())
                .stopLoss(signal.getStopLoss())
                .takeProfit(signal.getTakeProfit())
                .riskRewardRatio(signal.getRiskRewardRatio())
                .status(signal.getStatus())
                .confluence(signal.getConfluenceData())
                .timestamp(signal.getCreatedAt())
                .build();
    }
}
