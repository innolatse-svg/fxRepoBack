package com.forexintel.backend.risk.domain;

import com.forexintel.backend.iam.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entité représentant un enregistrement immuable du journal d'audit du Risk Engine.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Entity
@Table(name = "risk_audit_logs", schema = "risk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "requested_risk_pct", nullable = false)
    private Double requestedRiskPct;

    @Column(name = "lot_size", nullable = false)
    private Double lotSize;

    @Column(nullable = false, length = 50)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
