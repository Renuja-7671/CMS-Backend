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

import com.epic.cms.exception.EncryptionException;

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
			throw new EncryptionException("RSA key generation", "Failed to generate RSA key pair", e);
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
			
			log.debug("RSA decryption successful, decrypted {} bytes", decryptedBytes.length);
			
			// Check if the decrypted data is a base64-encoded string (from frontend)
			// Frontend base64-encodes the AES key before encrypting with RSA
			// If the decrypted bytes form a valid base64 string, decode it
			try {
				String decryptedString = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
				// Check if it looks like base64 (only contains base64 characters)
				if (decryptedString.matches("^[A-Za-z0-9+/=]+$")) {
					byte[] decodedKey = Base64.getDecoder().decode(decryptedString);
					log.debug("Decoded base64-encoded AES key, final key length: {} bytes", decodedKey.length);
					return decodedKey;
				}
			} catch (Exception e) {
				// Not base64, return raw bytes
				log.debug("Decrypted data is not base64-encoded, using raw bytes");
			}
			
			return decryptedBytes;
			
		} catch (Exception e) {
			log.error("RSA decryption failed", e);
			throw new EncryptionException("RSA decryption", "Failed to decrypt data with RSA private key", e);
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
			throw new EncryptionException("RSA encryption", "Failed to encrypt data with RSA public key", e);
		}
	}

	/**
	 * Encode public key to Base64 string and then decode it back.
	 * Used for testing purposes.
	 * 
	 * @param encodedPublicKey Base64-encoded public key string
	 * @return PublicKey object
	 * @throws RuntimeException if decoding fails
	 */
	public PublicKey decodePublicKey(String encodedPublicKey) {
		try {
			byte[] keyBytes = Base64.getDecoder().decode(encodedPublicKey);
			java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
			java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(RSA_ALGORITHM);
			return keyFactory.generatePublic(spec);
		} catch (Exception e) {
			log.error("Failed to decode public key", e);
			throw new EncryptionException("Public key decoding", "Failed to decode public key", e);
		}
	}

	/**
	 * Generate a random AES-256 key.
	 * Used for encrypting response data.
	 * 
	 * @return Random AES-256 key as byte array
	 */
	public byte[] generateAESKey() {
		try {
			javax.crypto.KeyGenerator keyGen = javax.crypto.KeyGenerator.getInstance("AES");
			keyGen.init(256, new SecureRandom());
			javax.crypto.SecretKey secretKey = keyGen.generateKey();
			return secretKey.getEncoded();
		} catch (Exception e) {
			log.error("Failed to generate AES key", e);
			throw new EncryptionException("AES key generation", "Failed to generate AES key", e);
		}
	}
}
