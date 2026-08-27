package com.forexintel.backend.execution.service;

import com.forexintel.backend.execution.domain.AccountCredential;
import com.forexintel.backend.execution.domain.TradingAccount;
import com.forexintel.backend.execution.dto.CreateTradingAccountRequestDto;
import com.forexintel.backend.execution.dto.TradingAccountResponseDto;
import com.forexintel.backend.execution.provider.TradingExecutionProvider;
import com.forexintel.backend.execution.repository.TradingAccountRepository;
import com.forexintel.backend.iam.domain.User;
import com.forexintel.backend.iam.repository.UserRepository;
import com.forexintel.backend.risk.dto.RiskEvaluationResultDto;
import com.forexintel.backend.risk.dto.TradeIntentDto;
import com.forexintel.backend.risk.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des comptes de trading et d'orchestration avec le Credential Vault et le Risk Engine.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingAccountService {

    private final TradingAccountRepository tradingAccountRepository;
    private final UserRepository userRepository;
    private final AesEncryptionService aesEncryptionService;
    private final RiskEngineService riskEngineService;
    private final TradingExecutionProvider tradingExecutionProvider;
    private final com.forexintel.backend.subscription.guard.QuotaGuard quotaGuard;

    /**
     * Enregistre un nouveau compte broker en chiffrant immédiatement son mot de passe en AES-256-GCM.
     */
    @Transactional
    public TradingAccountResponseDto createAccount(UUID userId, CreateTradingAccountRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé : " + userId));

        // Vérification des quotas SaaS et statut d'abonnement
        List<TradingAccount> existingAccounts = tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(userId);
        quotaGuard.checkCanAddTradingAccount(user, existingAccounts.size());

        // 1. Chiffrement cryptographique Zero-Knowledge
        AesEncryptionService.EncryptedData encrypted = aesEncryptionService.encrypt(request.getPassword());

        // 2. Création de l'entité compte
        TradingAccount account = TradingAccount.builder()
                .user(user)
                .brokerName(request.getBroker())
                .serverName(request.getServer())
                .accountLogin(request.getLogin())
                .environment(request.getAccountType() != null ? request.getAccountType().toUpperCase() : "DEMO")
                .balance(request.getAccountType() != null && request.getAccountType().equalsIgnoreCase("LIVE") ? 25000.0 : 10000.0)
                .equity(request.getAccountType() != null && request.getAccountType().equalsIgnoreCase("LIVE") ? 25000.0 : 10000.0)
                .currency("USD")
                .leverage("1:100")
                .isConnected(true)
                .autoTradingEnabled(false)
                .build();

        // 3. Liaison avec les credentials chiffrés
        AccountCredential credential = AccountCredential.builder()
                .tradingAccount(account)
                .encryptedPassword(encrypted.cipherTextBase64())
                .ivBase64(encrypted.ivBase64())
                .build();

        account.setCredential(credential);
        TradingAccount saved = tradingAccountRepository.save(account);

        log.info("[TradingAccount] Nouveau compte MT5 lié pour {} : Login {} sur {}",
                user.getEmail(), saved.getAccountLogin(), saved.getServerName());

        return mapToResponse(saved);
    }

    /**
     * Récupère la liste des comptes de l'utilisateur sans aucune information sensible.
     */
    @Transactional(readOnly = true)
    public List<TradingAccountResponseDto> getUserAccounts(UUID userId) {
        return tradingAccountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Supprime un compte et ses identifiants du Credential Vault.
     */
    @Transactional
    public void deleteAccount(UUID userId, UUID accountId) {
        TradingAccount account = tradingAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable ou accès non autorisé"));

        tradingAccountRepository.delete(account);
        log.info("[TradingAccount] Compte {} supprimé pour l'utilisateur {}", accountId, userId);
    }

    /**
     * Bascule l'activation de l'auto-trading sur un compte.
     */
    @Transactional
    public TradingAccountResponseDto toggleAutoTrading(UUID userId, UUID accountId) {
        TradingAccount account = tradingAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

        account.setAutoTradingEnabled(!account.getAutoTradingEnabled());
        TradingAccount updated = tradingAccountRepository.save(account);
        return mapToResponse(updated);
    }

    /**
     * Pipeline complet de routage d'ordre : Validation Risk Engine -> Déchiffrement Vault -> Exécution Provider.
     */
    @Transactional
    public TradingExecutionProvider.ExecutionResult executeTradePipeline(UUID userId, UUID accountId, TradeIntentDto intent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé : " + userId));
        quotaGuard.checkCanExecuteTrade(user);

        // A. Validation financière préalable par le Risk Engine (Zero-Trust)
        RiskEvaluationResultDto riskResult = riskEngineService.evaluateTradeIntent(userId, intent);
        if (!riskResult.isAllowed()) {
            throw new IllegalStateException("Trade bloqué par le Risk Engine : " + riskResult.getReason());
        }

        // B. Récupération et déchiffrement à la volée des credentials du compte cible
        TradingAccount account = tradingAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable pour l'exécution"));

        AccountCredential cred = account.getCredential();
        if (cred == null) {
            throw new IllegalStateException("Identifiants broker manquants dans le Credential Vault");
        }

        String plainPassword = aesEncryptionService.decrypt(cred.getEncryptedPassword(), cred.getIvBase64());

        // C. Transmission au fournisseur d'exécution
        return tradingExecutionProvider.executeTrade(intent, account, plainPassword);
    }

    private TradingAccountResponseDto mapToResponse(TradingAccount account) {
        return TradingAccountResponseDto.builder()
                .id(account.getId())
                .broker(account.getBrokerName())
                .server(account.getServerName())
                .login(account.getAccountLogin())
                .accountType(account.getEnvironment())
                .balance(account.getBalance())
                .equity(account.getEquity())
                .currency(account.getCurrency())
                .leverage(account.getLeverage())
                .connected(account.getIsConnected())
                .autoTradingEnabled(account.getAutoTradingEnabled())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
