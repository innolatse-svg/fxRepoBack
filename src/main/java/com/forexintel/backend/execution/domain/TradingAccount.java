package com.forexintel.backend.execution.domain;

import com.forexintel.backend.iam.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entité représentant un compte de trading connecté (MetaTrader 5, cTrader, etc.).
 *
 * @author Innocent
 * @version 1.0.0
 */
@Entity
@Table(name = "trading_accounts", schema = "execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "broker_name", nullable = false, length = 100)
    private String brokerName;

    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName;

    @Column(name = "account_login", nullable = false, length = 100)
    private String accountLogin;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String environment = "DEMO";

    @Column(nullable = false)
    @Builder.Default
    private Double balance = 10000.0;

    @Column(nullable = false)
    @Builder.Default
    private Double equity = 10000.0;

    @Column(length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 20)
    @Builder.Default
    private String leverage = "1:100";

    @Column(name = "is_connected", nullable = false)
    @Builder.Default
    private Boolean isConnected = true;

    @Column(name = "auto_trading_enabled", nullable = false)
    @Builder.Default
    private Boolean autoTradingEnabled = false;

    @OneToOne(mappedBy = "tradingAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AccountCredential credential;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
