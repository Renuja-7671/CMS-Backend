package com.epic.cms.util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.epic.cms.exception.EncryptionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for decrypting client-encrypted payloads.
 * Supports two encryption modes:
 * 1. Direct AES-GCM encryption (legacy, less secure - key exposed)
 * 2. Hybrid RSA + AES-GCM encryption (secure - key encrypted with RSA)
 */
@Slf4j
@Component
public class PayloadEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final RSAEncryptionUtil rsaEncryptionUtil;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     * Uses Spring-managed ObjectMapper bean which is already configured with JavaTimeModule
     * in JacksonConfig.java
     * 
     * @param rsaEncryptionUtil RSA encryption utility
     * @param objectMapper Spring-managed ObjectMapper (configured in JacksonConfig)
     */
    public PayloadEncryptionUtil(RSAEncryptionUtil rsaEncryptionUtil, ObjectMapper objectMapper) {
        this.rsaEncryptionUtil = rsaEncryptionUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Decrypt an encrypted payload from the client.
     * Legacy method: Uses direct AES key (less secure - key exposed in transit).
     * 
     * @param encryptedData Base64-encoded encrypted data (includes IV prefix)
     * @param encryptionKey Base64-encoded encryption key
     * @param targetClass Class type to deserialize the decrypted JSON into
     * @return Decrypted and deserialized object
     * @throws RuntimeException if decryption or deserialization fails
     * @deprecated Use {@link #decryptPayloadWithRSA} for better security
     */
    @Deprecated
    public <T> T decryptPayload(String encryptedData, String encryptionKey, Class<T> targetClass) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                throw new EncryptionException("Payload decryption", "Encrypted data cannot be null or empty");
            }
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                throw new EncryptionException("Payload decryption", "Encryption key cannot be null or empty");
            }

            // Decode the encryption key
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            
            // Decrypt using AES key
            T result = decryptWithAESKey(encryptedData, decodedKey, targetClass);
            
            log.info("Successfully decrypted payload (legacy mode) for class: {}", targetClass.getSimpleName());
            return result;

        } catch (Exception e) {
            log.error("Failed to decrypt payload", e);
            throw new EncryptionException("Payload decryption", "Failed to decrypt payload: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt an encrypted payload using hybrid RSA + AES encryption.
     * Secure method: AES key is encrypted with RSA public key.
     * 
     * Flow:
     * 1. Client encrypts payload with random AES key
     * 2. Client encrypts AES key with backend's RSA public key
     * 3. Backend decrypts AES key using RSA private key
     * 4. Backend decrypts payload using decrypted AES key
     * 
     * @param encryptedData Base64-encoded AES-encrypted data (includes IV prefix)
     * @param encryptedKey Base64-encoded RSA-encrypted AES key
     * @param privateKey RSA private key for decrypting the AES key
     * @param targetClass Class type to deserialize the decrypted JSON into
     * @return Decrypted and deserialized object
     * @throws RuntimeException if decryption or deserialization fails
     */
    public <T> T decryptPayloadWithRSA(String encryptedData, String encryptedKey, 
                                        PrivateKey privateKey, Class<T> targetClass) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                throw new EncryptionException("Payload decryption", "Encrypted data cannot be null or empty");
            }
            if (encryptedKey == null || encryptedKey.isEmpty()) {
                throw new EncryptionException("Payload decryption", "Encrypted key cannot be null or empty");
            }
            if (privateKey == null) {
                throw new EncryptionException("Payload decryption", "Private key cannot be null");
            }

            // Step 1: Decrypt the AES key using RSA private key
            byte[] aesKey = rsaEncryptionUtil.decryptWithPrivateKey(encryptedKey, privateKey);
            log.debug("Successfully decrypted AES key using RSA");

            // Step 2: Decrypt the payload using the AES key
            T result = decryptWithAESKey(encryptedData, aesKey, targetClass);
            
            log.info("Successfully decrypted payload (RSA mode) for class: {}", targetClass.getSimpleName());
            return result;

        } catch (Exception e) {
            log.error("Failed to decrypt payload with RSA", e);
            throw new EncryptionException("Payload decryption with RSA", "Failed to decrypt payload: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to decrypt data using AES key.
     * 
     * @param encryptedData Base64-encoded encrypted data (includes IV prefix)
     * @param aesKey AES key as byte array
     * @param targetClass Class type to deserialize the decrypted JSON into
     * @return Decrypted and deserialized object
     * @throws Exception if decryption or deserialization fails
     */
    private <T> T decryptWithAESKey(String encryptedData, byte[] aesKey, Class<T> targetClass) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(aesKey, "AES");

        // Decode the encrypted data
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // Extract IV (first 12 bytes) and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // Decrypt the data
        byte[] decryptedData = cipher.doFinal(encryptedBytes);

        // Convert decrypted bytes to string
        String jsonString = new String(decryptedData, StandardCharsets.UTF_8);

        // Deserialize JSON to target class
        return objectMapper.readValue(jsonString, targetClass);
    }

    /**
     * Encrypt a payload for sending to client (if needed for responses).
     * 
     * @param payload Object to encrypt
     * @param encryptionKey Base64-encoded encryption key
     * @return Base64-encoded encrypted data (includes IV prefix)
     * @throws RuntimeException if encryption fails
     */
    public String encryptPayload(Object payload, String encryptionKey) {
        try {
            if (payload == null) {
                throw new EncryptionException("Payload encryption", "Payload cannot be null");
            }
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                throw new EncryptionException("Payload decryption", "Encryption key cannot be null or empty");
            }

            // Serialize object to JSON
            String jsonString = objectMapper.writeValueAsString(payload);
            byte[] data = jsonString.getBytes(StandardCharsets.UTF_8);

            // Decode the encryption key
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the data
            byte[] encryptedData = cipher.doFinal(data);

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            // Return Base64-encoded result
            String result = Base64.getEncoder().encodeToString(combined);
            log.info("Successfully encrypted payload");
            return result;

        } catch (Exception e) {
            log.error("Failed to encrypt payload", e);
            throw new EncryptionException("Payload encryption", "Failed to encrypt payload: " + e.getMessage(), e);
        }
    }
}
