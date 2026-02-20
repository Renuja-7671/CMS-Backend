package com.epic.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for public key request.
 * Contains the public key and session ID for encrypted payload submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponse {
	
	/**
	 * Session ID to identify the key pair.
	 * Must be sent back when submitting encrypted payload.
	 */
	private String sessionId;
	
	/**
	 * Base64-encoded RSA public key.
	 * Client uses this to encrypt the AES key.
	 */
	private String publicKey;
	
	/**
	 * Timestamp when the key was generated.
	 */
	private String timestamp;
	
	/**
	 * TTL (time to live) in seconds.
	 * Key pair expires after this duration.
	 */
	private long ttlSeconds;
}
