package com.forexintel.backend.intelligence.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entité représentant un signal de trading algorithmique avec score de confluence multi-facteurs.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Entity
@Table(name = "trade_signals", schema = "intelligence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String direction; // BUY / SELL

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String timeframe = "H1";

    @Column(name = "alignment_score", nullable = false)
    private Integer alignmentScore;

    @Column(name = "entry_price", nullable = false)
    private Double entryPrice;

    @Column(name = "stop_loss", nullable = false)
    private Double stopLoss;

    @Column(name = "take_profit", nullable = false)
    private Double takeProfit;

    @Column(name = "risk_reward_ratio", nullable = false, length = 20)
    private String riskRewardRatio;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_CONFIRMATION";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "confluence_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> confluenceData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
