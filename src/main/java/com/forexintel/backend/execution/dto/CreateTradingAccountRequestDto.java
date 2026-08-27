package com.forexintel.backend.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la connexion d'un compte broker MT5.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTradingAccountRequestDto {
    private String broker;
    private String server;
    private String login;
    private String password;
    private String accountType; // DEMO ou LIVE
}
