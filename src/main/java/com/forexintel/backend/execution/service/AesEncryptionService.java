package com.forexintel.backend.execution.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement cryptographique AES-256-GCM pour le Credential Vault.
 * Garantit la confidentialité Zero-Knowledge des identifiants et mots de passe des comptes brokers.
 *
 * @author Innocent
 * @version 1.0.0
 */
@Slf4j
@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesEncryptionService(@Value("${vault.encryption-key:ForexIntelMasterKey2026Secure32B}") String masterKey) {
        this.secretKey = deriveSecretKey(masterKey);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Résultat d'un chiffrement contenant la charge chiffrée et son vecteur d'initialisation unique.
     */
    public record EncryptedData(String cipherTextBase64, String ivBase64) {}

    /**
     * Chiffre une chaîne de caractères en clair avec AES-256-GCM.
     *
     * @param plainText Texte en clair à chiffrer
     * @return Objet contenant le texte chiffré et l'IV encodés en Base64
     */
    public EncryptedData encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("Le texte à chiffrer ne peut pas être vide");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String cipherTextBase64 = Base64.getEncoder().encodeToString(cipherText);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);

            return new EncryptedData(cipherTextBase64, ivBase64);
        } catch (Exception e) {
            log.error("[CredentialVault] Erreur lors du chiffrement AES-256-GCM", e);
            throw new SecurityException("Échec du chiffrement des identifiants", e);
        }
    }

    /**
     * Déchiffre une charge utile chiffrée en utilisant l'IV associé.
     *
     * @param cipherTextBase64 Données chiffrées en Base64
     * @param ivBase64 Vecteur d'initialisation en Base64
     * @return Mot de passe / texte en clair restauré
     */
    public String decrypt(String cipherTextBase64, String ivBase64) {
        if (cipherTextBase64 == null || ivBase64 == null) {
            throw new IllegalArgumentException("Les données chiffrées et l'IV sont requis");
        }

        try {
            byte[] cipherText = Base64.getDecoder().decode(cipherTextBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CredentialVault] Erreur lors du déchiffrement AES-256-GCM", e);
            throw new SecurityException("Échec du déchiffrement des identifiants", e);
        }
    }

    private SecretKey deriveSecretKey(String key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-256 non disponible", e);
        }
    }
}
