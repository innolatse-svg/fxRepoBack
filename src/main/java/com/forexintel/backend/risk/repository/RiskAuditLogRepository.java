package com.forexintel.backend.risk.repository;

import com.forexintel.backend.risk.domain.RiskAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Répertoire JPA pour la consultation et la persistance des logs d'audit de risque.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Repository
public interface RiskAuditLogRepository extends JpaRepository<RiskAuditLog, UUID> {
    List<RiskAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
