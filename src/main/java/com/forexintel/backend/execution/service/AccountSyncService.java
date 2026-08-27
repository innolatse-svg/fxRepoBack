package com.forexintel.backend.execution.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.dto.TradingAccountResponseDto;
import com.forexintel.backend.execution.provider.PythonBridgeExecutionProvider;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service de synchronisation périodique et à la demande des soldes et de la santé des comptes MT5.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSyncService {

    private final TradingAccountRepository tradingAccountRepository;
    private final AesEncryptionService aesEncryptionService;
    private final PythonBridgeExecutionProvider pythonBridgeProvider;

    /**
     * Synchronise immédiatement un compte de trading spécifique pour un utilisateur.
     */
    @Transactional
    public TradingAccountResponseDto syncSingleAccount(UUID userId, UUID accountId) {
        TradingAccount account = tradingAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Compte de trading introuvable"));

        AccountCredential cred = account.getCredential();
        if (cred == null) {
            throw new IllegalStateException("Credentials introuvables pour ce compte");
        }

        String plainPassword = aesEncryptionService.decrypt(cred.getEncryptedPassword(), cred.getIvBase64());
        PythonBridgeExecutionProvider.AccountSyncResult syncResult =
                pythonBridgeProvider.syncAccountMetrics(account, plainPassword);

        account.setBalance(syncResult.balance());
        account.setEquity(syncResult.equity());
        account.setIsConnected(syncResult.connected());
        if (syncResult.currency() != null) account.setCurrency(syncResult.currency());
        if (syncResult.leverage() != null) account.setLeverage(syncResult.leverage());

        TradingAccount updated = tradingAccountRepository.save(account);
        log.info("[AccountSync] Compte {} synchronisé avec succès. Nouveau solde: {} {}",
                account.getAccountLogin(), updated.getBalance(), updated.getCurrency());

        return TradingAccountResponseDto.builder()
                .id(updated.getId())
                .broker(updated.getBrokerName())
                .server(updated.getServerName())
                .login(updated.getAccountLogin())
                .accountType(updated.getEnvironment())
                .balance(updated.getBalance())
                .equity(updated.getEquity())
                .currency(updated.getCurrency())
                .leverage(updated.getLeverage())
                .connected(updated.getIsConnected())
                .autoTradingEnabled(updated.getAutoTradingEnabled())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    /**
     * Tâche de fond automatique toutes les 60 secondes pour rafraîchir l'équité des comptes connectés.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    @Transactional
    public void backgroundSyncAllAccounts() {
        List<TradingAccount> accounts = tradingAccountRepository.findAll();
        for (TradingAccount acc : accounts) {
            try {
                if (acc.getCredential() != null) {
                    String plainPassword = aesEncryptionService.decrypt(
                            acc.getCredential().getEncryptedPassword(),
                            acc.getCredential().getIvBase64()
                    );
                    PythonBridgeExecutionProvider.AccountSyncResult res =
                            pythonBridgeProvider.syncAccountMetrics(acc, plainPassword);

                    acc.setBalance(res.balance());
                    acc.setEquity(res.equity());
                    acc.setIsConnected(res.connected());
                    tradingAccountRepository.save(acc);
                }
            } catch (Exception e) {
                log.debug("[AccountSync] Erreur sync arrière-plan pour compte {} : {}", acc.getAccountLogin(), e.getMessage());
            }
        }
    }
}
