package com.forexintel.backend.risk.service;

import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.domain.UserPreference;
import com.forexintel.backend.iam.repository.UserPreferenceRepository;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.domain.RiskAuditLog;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.repository.RiskAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du RiskEngineService.
 * Valide les contrôles Zero-Trust et la suprématie Plateforme > Utilisateur.
 *
 * @author Innocent
 */
@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private RiskAuditLogRepository riskAuditLogRepository;

    @InjectMocks
    private RiskEngineService riskEngineService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("trader@forexintel.com")
                .firstName("Test")
                .lastName("Trader")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Devrait accepter un trade conforme aux règles de risque de l'utilisateur")
    void evaluateTradeIntent_ShouldAllowCompliantTrade() {
        // Given: L'utilisateur a fixé une limite de 1.5% par trade
        Map<String, Object> settings = Map.of(
                "riskRules", Map.of(
                        "riskPerTradePercent", 1.5,
                        "maxDailyLossPercent", 3.0,
                        "maxExposurePercent", 6.0,
                        "maxOpenPositions", 3
                )
        );
        UserPreference preference = UserPreference.builder().userId(userId).user(testUser).settings(settings).build();
        when(userPreferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("EUR/USD")
                .direction("BUY")
                .lotSize(0.25)
                .entryPrice(1.0845)
                .stopLoss(1.0820)
                .takeProfit(1.0895)
                .requestedRiskPct(1.0)
                .currentOpenPositions(1)
                .currentExposurePct(1.0)
                .currentDailyLossPct(0.0)
                .build();

        // When
        RiskEvaluationResultDto result = riskEngineService.evaluateTradeIntent(userId, intent);

        // Then
        assertTrue(result.isAllowed());
        assertEquals("ALLOWED", result.getDecision());
        verify(riskAuditLogRepository, times(1)).save(any(RiskAuditLog.class));
    }

    @Test
    @DisplayName("Devrait rejeter un trade si le risque demandé excède la limite définie par l'utilisateur")
    void evaluateTradeIntent_ShouldReject_WhenExceedsUserRiskLimit() {
        // Given: L'utilisateur a fixé une limite de 1.0%
        Map<String, Object> settings = Map.of(
                "riskRules", Map.of("riskPerTradePercent", 1.0)
        );
        UserPreference preference = UserPreference.builder().userId(userId).user(testUser).settings(settings).build();
        when(userPreferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        // Intention demandant 2.0% de risque
        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("GBP/USD")
                .stopLoss(1.2850)
                .requestedRiskPct(2.0)
                .build();

        // When
        RiskEvaluationResultDto result = riskEngineService.evaluateTradeIntent(userId, intent);

        // Then
        assertFalse(result.isAllowed());
        assertEquals("REJECTED", result.getDecision());
        assertTrue(result.getReason().contains("excède le plafond autorisé"));
        verify(riskAuditLogRepository, times(1)).save(any(RiskAuditLog.class));
    }

    @Test
    @DisplayName("Devrait rejeter un trade si la règle utilisateur en BDD tente de dépasser le plafond inviolable plateforme (3%)")
    void evaluateTradeIntent_ShouldReject_WhenUserSettingExceedsPlatformHardCeiling() {
        // Given: L'utilisateur a configuré 10% en BDD (tentative de contournement)
        Map<String, Object> settings = Map.of(
                "riskRules", Map.of("riskPerTradePercent", 10.0)
        );
        UserPreference preference = UserPreference.builder().userId(userId).user(testUser).settings(settings).build();
        when(userPreferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        // Intention demandant 4.0% (inférieur à 10% mais supérieur au plafond plateforme de 3.0%)
        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("USD/JPY")
                .stopLoss(153.50)
                .requestedRiskPct(4.0)
                .build();

        // When
        RiskEvaluationResultDto result = riskEngineService.evaluateTradeIntent(userId, intent);

        // Then
        assertFalse(result.isAllowed());
        assertEquals("REJECTED", result.getDecision());
        assertEquals(3.0, result.getEffectiveRiskPct()); // Le plafond plateforme 3% a été imposé
        verify(riskAuditLogRepository, times(1)).save(any(RiskAuditLog.class));
    }

    @Test
    @DisplayName("Devrait rejeter un trade sans Stop Loss obligatoire")
    void evaluateTradeIntent_ShouldReject_WhenStopLossMissing() {
        when(userPreferenceRepository.findById(userId)).thenReturn(Optional.empty());

        TradeIntentDto intent = TradeIntentDto.builder()
                .symbol("XAU/USD")
                .stopLoss(null) // Pas de stop loss
                .requestedRiskPct(0.5)
                .build();

        // When
        RiskEvaluationResultDto result = riskEngineService.evaluateTradeIntent(userId, intent);

        // Then
        assertFalse(result.isAllowed());
        assertEquals("REJECTED", result.getDecision());
        assertTrue(result.getReason().contains("Stop Loss obligatoire manquant"));
    }
}
