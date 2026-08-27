package com.forexintel.backend.execution.repository;

import com.forexintel.backend.execution.domain.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Répertoire JPA pour la gestion des comptes de trading connectés.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, UUID> {
    List<TradingAccount> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<TradingAccount> findByIdAndUserId(UUID id, UUID userId);
}
