package com.epic.cms.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.epic.cms.exception.EncryptionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for card number encryption.
 * 
 * This implementation provides:
 * 1. Full card number encryption for database storage (fixed key)
 * 2. Middle-digit encryption for API responses (unique key per response)
 */
@Slf4j
@Component
public class CardEncryptionUtil {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12; // 96 bits
	private static final int GCM_TAG_LENGTH = 128; // 128 bits

	@Value("${card.encryption.database-key}")
	private String databaseEncryptionKey;

	/**
	 * Encrypts the full card number for database storage using a fixed key.
	 * Uses a fixed IV derived from the key to ensure deterministic encryption
	 * (same card number always produces the same encrypted value for searching).
	 * 
	 * @param plainCardNumber Full plain card number (e.g., "4532015112830366")
	 * @return Base64-encoded encrypted card number
	 * @throws EncryptionException if encryption fails
	 */
	public String encryptCardNumberForDatabase(String plainCardNumber) {
		try {
			if (plainCardNumber == null || plainCardNumber.isEmpty()) {
				throw new EncryptionException("Card number encryption", "Card number cannot be null or empty");
			}

			// Decode the fixed database encryption key
			byte[] decodedKey = Base64.getDecoder().decode(databaseEncryptionKey);
			SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

			// Use a fixed IV derived from the key (first 12 bytes of the key for deterministic encryption)
			// This ensures same input always produces same output
			byte[] iv = new byte[GCM_IV_LENGTH];
			System.arraycopy(decodedKey, 0, iv, 0, GCM_IV_LENGTH);

			// Initialize cipher for encryption
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

			// Encrypt the full card number
			byte[] encryptedData = cipher.doFinal(plainCardNumber.getBytes(StandardCharsets.UTF_8));

			// Combine IV and encrypted data
			byte[] combined = new byte[iv.length + encryptedData.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

			// Return Base64-encoded result
			String result = Base64.getEncoder().encodeToString(combined);
			log.debug("Full card number encrypted for database storage");
			return result;

		} catch (EncryptionException e) {
			throw e; // Re-throw our custom exception
		} catch (Exception e) {
			log.error("Failed to encrypt card number for database", e);
			throw new EncryptionException("Database card number encryption", "Encryption operation failed", e);
		}
	}

	/**
	 * Decrypts the full card number from database storage using the fixed key.
	 * 
	 * @param encryptedCardNumber Base64-encoded encrypted card number
	 * @return Full plain card number
	 * @throws EncryptionException if decryption fails
	 */
	public String decryptCardNumberFromDatabase(String encryptedCardNumber) {
		try {
			if (encryptedCardNumber == null || encryptedCardNumber.isEmpty()) {
				throw new EncryptionException("Card number decryption", "Encrypted card number cannot be null or empty");
			}

			// Decode the fixed database encryption key
			byte[] decodedKey = Base64.getDecoder().decode(databaseEncryptionKey);
			SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

			// Decode the encrypted card number
			byte[] combined = Base64.getDecoder().decode(encryptedCardNumber);

			// Extract IV and encrypted data
			byte[] iv = new byte[GCM_IV_LENGTH];
			byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
			System.arraycopy(combined, 0, iv, 0, iv.length);
			System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

			// Initialize cipher for decryption
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

			// Decrypt the card number
			byte[] decryptedData = cipher.doFinal(encryptedData);
			String plainCardNumber = new String(decryptedData, StandardCharsets.UTF_8);
			log.debug("Full card number decrypted from database storage");
			return plainCardNumber;

		} catch (EncryptionException e) {
			throw e; // Re-throw our custom exception
		} catch (Exception e) {
			log.error("Failed to decrypt card number from database", e);
			throw new EncryptionException("Database card number decryption", "Decryption operation failed", e);
		}
	}

	/**
	 * Encrypts the middle digits of a card number and generates a unique encryption key.
	 * 
	 * Format: First 6 digits (plain) + Encrypted middle digits (Base64) + Last 4 digits (plain)
	 * 
	 * @param cardNumber Full plain card number (e.g., "4532015112830366")
	 * @return Map containing "displayCardNumber" and "encryptionKey"
	 * @throws EncryptionException if encryption fails
	 */
	public Map<String, String> encryptMiddleDigits(String cardNumber) {
		try {
			if (cardNumber == null || cardNumber.length() < 10) {
				throw new EncryptionException("Middle digits encryption", "Card number must be at least 10 digits");
			}

			// Extract parts: first 6, middle, last 4
			String first6 = cardNumber.substring(0, 6);
			String last4 = cardNumber.substring(cardNumber.length() - 4);
			String middleDigits = cardNumber.substring(6, cardNumber.length() - 4);

			// Generate a unique encryption key for this card
			SecretKey uniqueKey = generateKey();
			String encryptionKey = Base64.getEncoder().encodeToString(uniqueKey.getEncoded());

			// Generate random IV for encryption
			byte[] iv = new byte[GCM_IV_LENGTH];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);

			// Initialize cipher for encryption
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, uniqueKey, parameterSpec);

			// Encrypt the middle digits
			byte[] encryptedData = cipher.doFinal(middleDigits.getBytes(StandardCharsets.UTF_8));

			// Combine IV and encrypted data
			byte[] combined = new byte[iv.length + encryptedData.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

			// Create display card number: first6 + encrypted_middle + last4
			String encryptedMiddle = Base64.getEncoder().encodeToString(combined);
			String displayCardNumber = first6 + encryptedMiddle + last4;

			log.debug("Card number middle digits encrypted successfully");

			// Return both the display card number and the encryption key
			Map<String, String> result = new HashMap<>();
			result.put("displayCardNumber", displayCardNumber);
			result.put("encryptionKey", encryptionKey);
			return result;

		} catch (EncryptionException e) {
			throw e; // Re-throw our custom exception
		} catch (Exception e) {
			log.error("Failed to encrypt card number middle digits", e);
			throw new EncryptionException("Middle digits encryption", "Encryption operation failed", e);
		}
	}

	/**
	 * Decrypts the middle digits and reconstructs the full card number.
	 * 
	 * @param displayCardNumber Display card number with encrypted middle (first6 + encrypted + last4)
	 * @param encryptionKey Base64-encoded encryption key
	 * @return Full plain card number
	 * @throws EncryptionException if decryption fails
	 */
	public String decryptMiddleDigits(String displayCardNumber, String encryptionKey) {
		try {
			if (displayCardNumber == null || displayCardNumber.length() < 10) {
				throw new EncryptionException("Middle digits decryption", "Invalid display card number");
			}

			// Extract parts: first 6, encrypted middle, last 4
			String first6 = displayCardNumber.substring(0, 6);
			String last4 = displayCardNumber.substring(displayCardNumber.length() - 4);
			String encryptedMiddle = displayCardNumber.substring(6, displayCardNumber.length() - 4);

			// Decode the encryption key
			byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
			SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

			// Decode the encrypted middle digits
			byte[] combined = Base64.getDecoder().decode(encryptedMiddle);

			// Extract IV and encrypted data
			byte[] iv = new byte[GCM_IV_LENGTH];
			byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
			System.arraycopy(combined, 0, iv, 0, iv.length);
			System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

			// Initialize cipher for decryption
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

			// Decrypt the middle digits
			byte[] decryptedData = cipher.doFinal(encryptedData);
			String middleDigits = new String(decryptedData, StandardCharsets.UTF_8);

			// Reconstruct full card number
			String fullCardNumber = first6 + middleDigits + last4;
			log.debug("Card number middle digits decrypted successfully");
			return fullCardNumber;

		} catch (EncryptionException e) {
			throw e; // Re-throw our custom exception
		} catch (Exception e) {
			log.error("Failed to decrypt card number middle digits", e);
			throw new EncryptionException("Middle digits decryption", "Decryption operation failed", e);
		}
	}

	/**
	 * Generates a new AES-256 encryption key.
	 * 
	 * @return New SecretKey for AES-256 encryption
	 * @throws EncryptionException if key generation fails
	 */
	private SecretKey generateKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256, new SecureRandom());
			return keyGenerator.generateKey();
		} catch (Exception e) {
			throw new EncryptionException("Encryption key generation", "Failed to generate AES key", e);
		}
	}
}

