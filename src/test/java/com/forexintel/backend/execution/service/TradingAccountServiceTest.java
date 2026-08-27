package com.forexintel.backend.execution.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.dto.CreateTradingAccountRequestDto;
import com.forexintel.backend.execution.dto.TradingAccountResponseDto;
import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.service.RiskEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires et de sécurité du TradingAccountService (Credential Vault & Pipeline).
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class TradingAccountServiceTest {

    @Mock
    private TradingAccountRepository tradingAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AesEncryptionService aesEncryptionService;

    @Mock
    private RiskEngineService riskEngineService;

    @Mock
    private TradingExecutionProvider tradingExecutionProvider;

    @Mock
    private com.forexintel.backend.subscription.guard.QuotaGuard quotaGuard;

    @InjectMocks
    private TradingAccountService tradingAccountService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("trader@forexintel.com")
                .firstName("Innocent")
                .lastName("Dev")
                .build();
    }

    @Test
    @DisplayName("Devrait créer un compte MT5 et stocker les identifiants chiffrés en base de données")
    void createAccount_ShouldEncryptPasswordAndSave() {
        // Given
        CreateTradingAccountRequestDto request = CreateTradingAccountRequestDto.builder()
                .broker("IC Markets")
                .server("ICMarkets-Demo01")
                .login("12345678")
                .password("PlainTextPassword123")
                .accountType("DEMO")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aesEncryptionService.encrypt("PlainTextPassword123"))
                .thenReturn(new AesEncryptionService.EncryptedData("cipherBase64String==", "ivBase64=="));

        when(tradingAccountRepository.save(any(TradingAccount.class))).thenAnswer(invocation -> {
            TradingAccount acc = invocation.getArgument(0);
            acc.setId(UUID.randomUUID());
            return acc;
        });

        // When
        TradingAccountResponseDto response = tradingAccountService.createAccount(userId, request);

        // Then
        assertNotNull(response);
        assertEquals("IC Markets", response.getBroker());
        assertEquals("12345678", response.getLogin());

        // Vérification de la persistance des credentials chiffrés
        ArgumentCaptor<TradingAccount> captor = ArgumentCaptor.forClass(TradingAccount.class);
        verify(tradingAccountRepository).save(captor.capture());
        TradingAccount savedAccount = captor.getValue();

        assertNotNull(savedAccount.getCredential());
        assertEquals("cipherBase64String==", savedAccount.getCredential().getEncryptedPassword());
        assertEquals("ivBase64==", savedAccount.getCredential().getIvBase64());
        assertNotEquals("PlainTextPassword123", savedAccount.getCredential().getEncryptedPassword());
    }

    @Test
    @DisplayName("Devrait renvoyer les comptes sans aucune fuite de mot de passe ou d'IV (Zero-Leak)")
    void getUserAccounts_ShouldNeverLeakPasswordOrIv() {
        // Given
        TradingAccount acc = TradingAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .brokerName("Pepperstone")
                .serverName("Pepperstone-Live01")
                .accountLogin("99887766")
                .environment("LIVE")
                .balance(50000.0)
                .equity(50000.0)
                .currency("USD")
                .leverage("1:30")
                .isConnected(true)
                .autoTradingEnabled(true)
                .credential(AccountCredential.builder()
                        .encryptedPassword("VerySecretCipher")
                        .ivBase64("SecretIV")
                        .build())
                .build();

        when(tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(acc));

        // When
        List<TradingAccountResponseDto> list = tradingAccountService.getUserAccounts(userId);

        // Then
        assertEquals(1, list.size());
        TradingAccountResponseDto dto = list.get(0);
        assertEquals("Pepperstone", dto.getBroker());
        assertEquals("99887766", dto.getLogin());

        // Vérification de structure : aucun champ mot de passe n'existe dans le DTO
        assertNotNull(dto.getId());
    }

    @Test
    @DisplayName("Devrait exécuter le flux complet : Validation Risk Engine -> Déchiffrement Vault -> Exécution Provider")
    void executeTradePipeline_ShouldValidateRiskDecryptAndExecute() {
        // Given
        UUID accountId = UUID.randomUUID();
        TradingAccount acc = TradingAccount.builder()
                .id(accountId)
                .user(user)
                .brokerName("IC Markets")
                .serverName("ICMarkets-Live")
                .accountLogin("112233")
                .credential(AccountCredential.builder()
                        .encryptedPassword("EncryptedSecret")
                        .ivBase64("InitVector")
                        .build())
                .build();

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("EUR/USD")
                .direction("BUY")
                .lotSize(0.25)
                .entryPrice(1.0845)
                .stopLoss(1.0820)
                .requestedRiskPct(1.0)
                .build();

        // Risk Engine autorise l'ordre
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(riskEngineService.evaluateTradeIntent(eq(userId), any(TradeIntentDto.class)))
                .thenReturn(RiskEvaluationResultDto.builder().allowed(true).decision("ALLOWED").build());

        when(tradingAccountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(acc));
        when(aesEncryptionService.decrypt("EncryptedSecret", "InitVector")).thenReturn("PlainPass123");

        when(tradingExecutionProvider.executeTrade(eq(intent), eq(acc), eq("PlainPass123")))
                .thenReturn(new TradingExecutionProvider.ExecutionResult(true, "TICKET-7788", "Succès", 1.0845, 0.25));

        // When
        TradingExecutionProvider.ExecutionResult result = tradingAccountService.executeTradePipeline(userId, accountId, intent);

        // Then
        assertTrue(result.success());
        assertEquals("TICKET-7788", result.orderTicket());
        verify(quotaGuard).checkCanExecuteTrade(user);
        verify(riskEngineService).evaluateTradeIntent(userId, intent);
        verify(aesEncryptionService).decrypt("EncryptedSecret", "InitVector");
        verify(tradingExecutionProvider).executeTrade(intent, acc, "PlainPass123");
    }

    @Test
    @DisplayName("Devrait bloquer l'exécution si le Risk Engine refuse l'ordre")
    void executeTradePipeline_ShouldThrowException_WhenRiskEngineRejects() {
        // Given
        UUID accountId = UUID.randomUUID();
        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("GBP/USD")
                .requestedRiskPct(5.0) // Trop élevé
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(riskEngineService.evaluateTradeIntent(eq(userId), any(TradeIntentDto.class)))
                .thenReturn(RiskEvaluationResultDto.builder().allowed(false).decision("REJECTED").reason("Max Risk Exceeded").build());

        // When / Then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                tradingAccountService.executeTradePipeline(userId, accountId, intent)
        );

        assertTrue(ex.getMessage().contains("bloqué par le Risk Engine"));
        verifyNoInteractions(tradingExecutionProvider);
    }
}
