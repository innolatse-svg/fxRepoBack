package com.forexintel.backend.execution.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entité du Credential Vault stockant le mot de passe broker chiffré en AES-256-GCM avec son IV.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Entity
@Table(name = "account_credentials", schema = "execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCredential {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "account_id")
    private TradingAccount tradingAccount;

    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;

    @Column(name = "iv_base64", nullable = false, length = 100)
    private String ivBase64;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
