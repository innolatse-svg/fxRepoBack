package com.forexintel.backend.execution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du service de chiffrement AES-256-GCM (Credential Vault).
 *
 * @author Innocent
 */
class AesEncryptionServiceTest {

    private AesEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new AesEncryptionService("ForexIntelMasterKey2026Secure32B");
    }

    @Test
    @DisplayName("Devrait chiffrer et déchiffrer fidèlement un mot de passe broker")
    void encryptAndDecrypt_ShouldRestoreOriginalPassword() {
        String originalPassword = "SuperSecretBrokerPassword123!#";

        AesEncryptionService.EncryptedData encrypted = encryptionService.encrypt(originalPassword);

        assertNotNull(encrypted.cipherTextBase64());
        assertNotNull(encrypted.ivBase64());
        assertNotEquals(originalPassword, encrypted.cipherTextBase64());

        String decrypted = encryptionService.decrypt(encrypted.cipherTextBase64(), encrypted.ivBase64());
        assertEquals(originalPassword, decrypted);
    }

    @Test
    @DisplayName("Devrait générer un IV aléatoire et un texte chiffré différent pour deux chiffrements du même mot de passe")
    void encrypt_ShouldProduceDistinctCiphertextAndIVs() {
        String password = "SamePasswordTwice";

        AesEncryptionService.EncryptedData first = encryptionService.encrypt(password);
        AesEncryptionService.EncryptedData second = encryptionService.encrypt(password);

        assertNotEquals(first.ivBase64(), second.ivBase64());
        assertNotEquals(first.cipherTextBase64(), second.cipherTextBase64());
    }

    @Test
    @DisplayName("Devrait échouer lors du déchiffrement si le texte chiffré ou l'IV a été altéré")
    void decrypt_ShouldThrowSecurityException_WhenCiphertextTampered() {
        String password = "TestPassword";
        AesEncryptionService.EncryptedData encrypted = encryptionService.encrypt(password);

        String tamperedCipher = "A" + encrypted.cipherTextBase64().substring(1);

        assertThrows(SecurityException.class, () ->
                encryptionService.decrypt(tamperedCipher, encrypted.ivBase64())
        );
    }
}
