package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving RSA-encrypted payload from client.
 * Uses hybrid encryption: RSA for key exchange, AES-GCM for data encryption.
 * 
 * Flow:
 * 1. Client requests public key from backend (GET /api/encryption/public-key)
 * 2. Client generates random AES-256 key
 * 3. Client encrypts payload with AES-GCM using the random key
 * 4. Client encrypts the AES key with RSA public key
 * 5. Client sends both encrypted payload and encrypted key to backend
 * 6. Backend decrypts AES key using RSA private key
 * 7. Backend decrypts payload using decrypted AES key
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureEncryptedPayloadRequest {

	/**
	 * Session ID received from public key request.
	 * Used to identify which private key to use for decryption.
	 */
	@NotBlank(message = "Session ID is required")
	private String sessionId;

	/**
	 * Base64-encoded AES-encrypted payload (includes IV prefix).
	 * Encrypted using AES-256-GCM with a random key.
	 */
	@NotBlank(message = "Encrypted data is required")
	private String encryptedData;

	/**
	 * Base64-encoded RSA-encrypted AES key.
	 * The random AES key is encrypted with the backend's RSA public key.
	 */
	@NotBlank(message = "Encrypted key is required")
	private String encryptedKey;

	/**
	 * Optional: Payload type identifier to help backend determine how to process.
	 * Examples: "CREATE_CARD", "UPDATE_CARD", "CREATE_REQUEST"
	 */
	private String payloadType;
}
