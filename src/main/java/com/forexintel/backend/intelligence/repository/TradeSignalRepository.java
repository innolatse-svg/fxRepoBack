package com.forexintel.backend.intelligence.repository;

import com.forexintel.backend.intelligence.domain.TradeSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Répertoire JPA pour la persistance et la consultation des signaux de trading IA.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Repository
public interface TradeSignalRepository extends JpaRepository<TradeSignal, UUID> {
    List<TradeSignal> findAllByOrderByCreatedAtDesc();
    List<TradeSignal> findByStatusOrderByCreatedAtDesc(String status);
    List<TradeSignal> findByDirectionOrderByCreatedAtDesc(String direction);
    List<TradeSignal> findByStatusAndDirectionOrderByCreatedAtDesc(String status, String direction);
}
