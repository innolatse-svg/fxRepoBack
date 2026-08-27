package com.forexintel.backend.execution.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'ActivePositionMonitor pour le déclenchement du Trailing Stop dynamique.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class ActivePositionMonitorTest {

    @Mock
    private AesEncryptionService aesEncryptionService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ActivePositionMonitor activePositionMonitor;

    private TradingAccount account;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activePositionMonitor, "bridgeUrl", "http://localhost:5001");

        account = TradingAccount.builder()
                .id(UUID.randomUUID())
                .accountLogin("12345678")
                .serverName("ICMarkets-Demo")
                .credential(AccountCredential.builder()
                        .encryptedPassword("EncryptedSecret")
                        .ivBase64("IV123")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Devrait avancer le Stop Loss quand le prix d'un ordre BUY gagne +40 pips avec un step de 15 pips")
    void evaluateAndAdjustStopLoss_BuyOrderInProfit_ShouldAdvanceStopLoss() {
        when(aesEncryptionService.decrypt("EncryptedSecret", "IV123")).thenReturn("PlainPass");
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", true)));

        // Entry: 1.0820, Current: 1.0860 (+40 pips), Current SL: 1.0800, Step: 15 pips -> Target SL = 1.0845
        boolean modified = activePositionMonitor.evaluateAndAdjustStopLoss(
                account,
                "TICKET-101",
                "EUR/USD",
                "BUY",
                1.0820,
                1.0860,
                1.0800,
                15.0
        );

        assertTrue(modified);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }
}
