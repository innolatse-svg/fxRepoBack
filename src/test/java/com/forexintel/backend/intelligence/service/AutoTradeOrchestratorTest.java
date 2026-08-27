package com.forexintel.backend.intelligence.service;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.execution.service.TradingAccountService;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.intelligence.domain.TradeSignal;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.service.RiskEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'AutoTradeOrchestrator pour l'exécution Full-Auto des signaux IA.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class AutoTradeOrchestratorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private TradingAccountRepository tradingAccountRepository;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private RiskEngineService riskEngineService;

    @InjectMocks
    private AutoTradeOrchestrator autoTradeOrchestrator;

    private User user;
    private TradeSignal signal;
    private TradingAccount account;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("autotrader@forexintel.com")
                .subscriptionStatus("ACTIVE")
                .build();

        signal = TradeSignal.builder()
                .id(UUID.randomUUID())
                .symbol("EUR/USD")
                .direction("BUY")
                .entryPrice(1.0845)
                .stopLoss(1.0815)
                .takeProfit(1.0915)
                .build();

        account = TradingAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountLogin("889900")
                .build();
    }

    @Test
    @DisplayName("Devrait exécuter l'ordre automatiquement si l'utilisateur est en FULL_AUTO et le Risk Engine autorise")
    void processAutoExecutionForSignal_FullAutoUser_ShouldExecute() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userPreferenceRepository.findById(user.getId())).thenReturn(Optional.of(
                UserPreference.builder()
                        .user(user)
                        .settings(Map.of("automationLevel", "FULL_AUTO", "maxRiskPerTradePct", 1.5))
                        .build()
        ));
        when(tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(account));
        when(riskEngineService.evaluateTradeIntent(eq(user.getId()), any(TradeIntentDto.class)))
                .thenReturn(RiskEvaluationResultDto.builder().allowed(true).decision("ALLOWED").build());

        when(tradingAccountService.executeTradePipeline(eq(user.getId()), eq(account.getId()), any(TradeIntentDto.class)))
                .thenReturn(new TradingExecutionProvider.ExecutionResult(true, "MT5-AUTO-999", "Exécuté", 1.0845, 0.25));

        autoTradeOrchestrator.processAutoExecutionForSignal(signal);

        verify(riskEngineService).evaluateTradeIntent(eq(user.getId()), any(TradeIntentDto.class));
        verify(tradingAccountService).executeTradePipeline(eq(user.getId()), eq(account.getId()), any(TradeIntentDto.class));
    }

    @Test
    @DisplayName("Ne devrait PAS exécuter d'ordre si le mode utilisateur est MANUAL")
    void processAutoExecutionForSignal_ManualUser_ShouldNotExecute() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userPreferenceRepository.findById(user.getId())).thenReturn(Optional.of(
                UserPreference.builder()
                        .user(user)
                        .settings(Map.of("automationLevel", "MANUAL"))
                        .build()
        ));

        autoTradeOrchestrator.processAutoExecutionForSignal(signal);

        verifyNoInteractions(tradingAccountService);
    }
}
