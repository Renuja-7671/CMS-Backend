package com.epic.cms.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for RSA encryption/decryption operations.
 * Used for secure key exchange in hybrid encryption system.
 * 
 * Hybrid Encryption Flow:
 * 1. Backend generates RSA key pair (2048-bit)
 * 2. Backend sends public key to client
 * 3. Client generates random AES-256 key
 * 4. Client encrypts payload with AES-GCM
 * 5. Client encrypts AES key with RSA public key
 * 6. Backend decrypts AES key with RSA private key
 * 7. Backend decrypts payload with AES key
 */
@Slf4j
@Component
public class RSAEncryptionUtil {

	private static final String RSA_ALGORITHM = "RSA";
	private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
	private static final int KEY_SIZE = 2048;

	/**
	 * Generate a new RSA key pair.
	 * 
	 * @return KeyPair containing public and private keys
	 * @throws RuntimeException if key generation fails
	 */
	public KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
			keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			
			log.debug("RSA key pair generated successfully");
			return keyPair;
			
		} catch (Exception e) {
			log.error("Failed to generate RSA key pair", e);
			throw new RuntimeException("Failed to generate RSA key pair", e);
		}
	}

	/**
	 * Convert public key to Base64-encoded string for transmission.
	 * 
	 * @param publicKey Public key to encode
	 * @return Base64-encoded public key
	 */
	public String encodePublicKey(PublicKey publicKey) {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	/**
	 * Decrypt data encrypted with RSA public key using the private key.
	 * Used to decrypt the AES key sent by client.
	 * 
	 * @param encryptedData Base64-encoded encrypted data
	 * @param privateKey Private key for decryption
	 * @return Decrypted data as byte array
	 * @throws RuntimeException if decryption fails
	 */
	public byte[] decryptWithPrivateKey(String encryptedData, PrivateKey privateKey) {
		try {
			// Create OAEP parameter spec matching Python's cryptography library
			// Uses SHA-256 for both the hash function and MGF1
			OAEPParameterSpec oaepParams = new OAEPParameterSpec(
				"SHA-256",
				"MGF1",
				MGF1ParameterSpec.SHA256,
				PSource.PSpecified.DEFAULT
			);
			
			Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
			
			byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
			
			log.debug("RSA decryption successful");
			return decryptedBytes;
			
		} catch (Exception e) {
			log.error("RSA decryption failed", e);
			throw new RuntimeException("Failed to decrypt data with RSA private key", e);
		}
	}

	/**
	 * Encrypt data with RSA public key.
	 * NOTE: This is typically done on the client side.
	 * Included here for testing purposes.
	 * 
	 * @param data Data to encrypt
	 * @param publicKey Public key for encryption
	 * @return Base64-encoded encrypted data
	 * @throws RuntimeException if encryption fails
	 */
	public String encryptWithPublicKey(byte[] data, PublicKey publicKey) {
		try {
			// Create OAEP parameter spec matching decryption
			OAEPParameterSpec oaepParams = new OAEPParameterSpec(
				"SHA-256",
				"MGF1",
				MGF1ParameterSpec.SHA256,
				PSource.PSpecified.DEFAULT
			);
			
			Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
			
			byte[] encryptedBytes = cipher.doFinal(data);
			String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
			
			log.debug("RSA encryption successful");
			return encryptedData;
			
		} catch (Exception e) {
			log.error("RSA encryption failed", e);
			throw new RuntimeException("Failed to encrypt data with RSA public key", e);
		}
	}
}
