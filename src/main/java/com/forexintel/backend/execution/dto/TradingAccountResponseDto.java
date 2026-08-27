package com.forexintel.backend.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour l'exposition sécurisée d'un compte de trading.
 * Ne contient JAMAIS d'informations d'authentification (mot de passe, IV, hash).
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingAccountResponseDto {
    private UUID id;
    private String broker;
    private String server;
    private String login;
    private String accountType;
    private Double balance;
    private Double equity;
    private String currency;
    private String leverage;
    private Boolean connected;
    private Boolean autoTradingEnabled;
    private OffsetDateTime createdAt;
}
