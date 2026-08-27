package com.forexintel.backend.execution.provider;

import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du fournisseur d'exécution Python Bridge MT5.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class PythonBridgeExecutionProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private PythonBridgeExecutionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PythonBridgeExecutionProvider(restTemplate, "http://localhost:5001");
    }

    @Test
    @DisplayName("Devrait formater la requête et relayer l'ordre au microservice Python MT5")
    void executeTrade_ShouldPostToPythonBridgeAndReturnResult() {
        // Given
        TradingAccount account = TradingAccount.builder()
                .id(UUID.randomUUID())
                .accountLogin("8849102")
                .serverName("ICMarkets-Live01")
                .build();

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("EUR/USD")
                .direction("BUY")
                .lotSize(0.50)
                .entryPrice(1.0845)
                .stopLoss(1.0815)
                .takeProfit(1.0895)
                .build();

        Map<String, Object> mockBridgeResponse = Map.of(
                "success", true,
                "order_ticket", "MT5-TICKET-9999",
                "message", "Ordre execute avec succes",
                "executed_price", 1.0845,
                "executed_volume", 0.50
        );

        when(restTemplate.postForEntity(
                eq("http://localhost:5001/mt5/trade/execute"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockBridgeResponse, HttpStatus.OK));

        // When
        TradingExecutionProvider.ExecutionResult result =
                provider.executeTrade(intent, account, "PlainSecretPass");

        // Then
        assertTrue(result.success());
        assertEquals("MT5-TICKET-9999", result.orderTicket());
        assertEquals(1.0845, result.executedPrice());
        assertEquals(0.50, result.executedVolume());

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("http://localhost:5001/mt5/trade/execute"), captor.capture(), eq(Map.class));

        Map body = (Map) captor.getValue().getBody();
        assertNotNull(body);
        assertEquals(8849102L, body.get("login"));
        assertEquals("ICMarkets-Live01", body.get("server"));
        assertEquals("EUR/USD", body.get("symbol"));
        assertEquals(0.50, body.get("volume"));
    }

    @Test
    @DisplayName("Devrait synchroniser le compte via /mt5/account/sync")
    void syncAccountMetrics_ShouldReturnUpdatedBalances() {
        // Given
        TradingAccount account = TradingAccount.builder()
                .id(UUID.randomUUID())
                .accountLogin("12345678")
                .serverName("DemoServer")
                .balance(10000.0)
                .equity(10000.0)
                .currency("USD")
                .leverage("1:100")
                .build();

        Map<String, Object> mockSyncResp = Map.of(
                "connected", true,
                "balance", 10250.50,
                "equity", 10380.00,
                "margin", 320.0,
                "free_margin", 10060.0,
                "currency", "USD",
                "leverage", "1:100",
                "message", "Sync OK"
        );

        when(restTemplate.postForEntity(
                eq("http://localhost:5001/mt5/account/sync"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockSyncResp, HttpStatus.OK));

        // When
        PythonBridgeExecutionProvider.AccountSyncResult result =
                provider.syncAccountMetrics(account, "Password123");

        // Then
        assertTrue(result.connected());
        assertEquals(10250.50, result.balance());
        assertEquals(10380.00, result.equity());
    }
}
