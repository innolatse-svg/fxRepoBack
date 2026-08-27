package com.forexintel.backend.intelligence.service;

import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.service.TradingAccountService;
import com.forexintel.backend.intelligence.domain.TradeSignal;
import com.forexintel.backend.intelligence.dto.ExecuteSignalRequestDto;
import com.forexintel.backend.intelligence.dto.TradeSignalDto;
import com.forexintel.backend.intelligence.repository.TradeSignalRepository;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du moteur de signaux et d'intelligence de confluence.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class SignalsEngineServiceTest {

    @Mock
    private TradeSignalRepository tradeSignalRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private AutoTradeOrchestrator autoTradeOrchestrator;

    @InjectMocks
    private SignalsEngineService signalsEngineService;

    private UUID signalId;
    private UUID userId;
    private UUID accountId;
    private TradeSignal signal;

    @BeforeEach
    void setUp() {
        signalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        signal = TradeSignal.builder()
                .id(signalId)
                .symbol("EUR/USD")
                .direction("BUY")
                .timeframe("H1")
                .alignmentScore(88)
                .entryPrice(1.0845)
                .stopLoss(1.0815)
                .takeProfit(1.0915)
                .riskRewardRatio("1:2.33")
                .status("PENDING_CONFIRMATION")
                .confluenceData(Map.of("technical", Map.of("title", "Bullish Order Block")))
                .build();
    }

    @Test
    @DisplayName("Devrait exécuter un signal via le pipeline de compte et marquer le statut EXECUTED_DEMO")
    void executeSignal_ShouldRouteThroughAccountPipelineAndMarkExecuted() {
        when(tradeSignalRepository.findById(signalId)).thenReturn(Optional.of(signal));

        TradingExecutionProvider.ExecutionResult execResult =
                new TradingExecutionProvider.ExecutionResult(true, "MT5-SIG-1234", "Exécuté avec succès", 1.0845, 0.20);

        when(tradingAccountService.executeTradePipeline(eq(userId), eq(accountId), any(TradeIntentDto.class)))
                .thenReturn(execResult);

        ExecuteSignalRequestDto req = ExecuteSignalRequestDto.builder()
                .accountId(accountId)
                .lotSize(0.20)
                .build();

        TradingExecutionProvider.ExecutionResult result = signalsEngineService.executeSignal(userId, signalId, req);

        assertTrue(result.success());
        assertEquals("MT5-SIG-1234", result.orderTicket());
        assertEquals("EXECUTED_DEMO", signal.getStatus());
        verify(tradeSignalRepository).save(signal);
        verify(tradingAccountService).executeTradePipeline(eq(userId), eq(accountId), any(TradeIntentDto.class));
    }

    @Test
    @DisplayName("Devrait annuler un signal lors du rejet utilisateur")
    void dismissSignal_ShouldUpdateStatusToCancelled() {
        when(tradeSignalRepository.findById(signalId)).thenReturn(Optional.of(signal));

        signalsEngineService.dismissSignal(signalId);

        assertEquals("CANCELLED", signal.getStatus());
        verify(tradeSignalRepository).save(signal);
    }

    @Test
    @DisplayName("Devrait générer et diffuser un signal sur les canaux WebSockets STOMP")
    void generateAndBroadcastSignal_ShouldPersistAndPublishToWebSockets() {
        when(tradeSignalRepository.save(any(TradeSignal.class))).thenAnswer(i -> {
            TradeSignal s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        signalsEngineService.generateAndBroadcastSignal();

        verify(tradeSignalRepository).save(any(TradeSignal.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/signals"), any(TradeSignalDto.class));
        verify(messagingTemplate).convertAndSend(eq("/user/queue/signals"), any(TradeSignalDto.class));
    }
}
