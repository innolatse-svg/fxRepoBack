package com.forexintel.backend.marketdata.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.execution.service.AesEncryptionService;
import com.forexintel.backend.marketdata.dto.QuoteTickDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du MarketDataService pour la validation du Data Gating.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private TradingAccountRepository tradingAccountRepository;

    @Mock
    private AesEncryptionService aesEncryptionService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MarketDataService marketDataService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(marketDataService, "bridgeUrl", "http://localhost:5001");
    }

    @Test
    @DisplayName("Devrait rejeter l'accès aux cotations avec HTTP 403 si aucun compte MT5 n'est raccordé")
    void getGatedQuote_NoAccount_ShouldThrowForbidden() {
        when(tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                marketDataService.getGatedQuote(userId, "EUR/USD")
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Connectez votre compte Deriv ou broker MT5"));
    }

    @Test
    @DisplayName("Devrait autoriser l'accès et retourner la cotation si un compte MT5 est raccordé")
    void getGatedQuote_WithAccount_ShouldReturnQuote() {
        TradingAccount account = TradingAccount.builder()
                .id(UUID.randomUUID())
                .accountLogin("12345678")
                .serverName("Deriv-Demo")
                .credential(AccountCredential.builder()
                        .encryptedPassword("EncryptedSecret")
                        .ivBase64("IV123")
                        .build())
                .build();

        when(tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(account));
        when(aesEncryptionService.decrypt("EncryptedSecret", "IV123")).thenReturn("PlainPass");

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("bid", 1.0845, "ask", 1.08465, "spread", 1.5)));

        QuoteTickDto quote = marketDataService.getGatedQuote(userId, "EUR/USD");

        assertNotNull(quote);
        assertEquals("EUR/USD", quote.getSymbol());
        assertEquals(1.0845, quote.getBid());
        assertEquals(1.08465, quote.getAsk());
    }
}
