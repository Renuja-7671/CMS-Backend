package com.epic.cms.controller;

import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epic.cms.dto.ApiResponse;
import com.epic.cms.dto.PublicKeyResponse;
import com.epic.cms.service.KeyManagementService;
import com.epic.cms.service.KeyManagementService.KeyPairInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.CrossOrigin;



/**
 * Controller for encryption-related operations.
 * Handles public key distribution for secure payload encryption.
 */
@Slf4j
@RestController
@RequestMapping("/api/encryption")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EncryptionController {

	private final KeyManagementService keyManagementService;

	/**
	 * Generate and return a new RSA public key for payload encryption.
	 * Client should:
	 * 1. Call this endpoint to get sessionId and publicKey
	 * 2. Generate random AES-256 key
	 * 3. Encrypt payload with AES key
	 * 4. Encrypt AES key with RSA public key
	 * 5. Send sessionId, encryptedData, and encryptedKey to card creation endpoint
	 * 
	 * @return Public key and session ID
	 */
	@GetMapping("/public-key")
	public ResponseEntity<ApiResponse<PublicKeyResponse>> getPublicKey() {
		log.info("GET /api/encryption/public-key - Generate RSA public key");

		// Generate new key pair
		KeyPairInfo keyPairInfo = keyManagementService.generateKeyPair();

		// Create response
		PublicKeyResponse response = new PublicKeyResponse(
				keyPairInfo.getSessionId(),
				keyPairInfo.getPublicKey(),
				keyPairInfo.getExpiryTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
				keyPairInfo.getTtlSeconds()
		);

		log.info("Generated public key with session ID: {}, TTL: {} seconds", 
				response.getSessionId(), response.getTtlSeconds());

		return ResponseEntity.ok(
				ApiResponse.success("Public key generated successfully", response));
	}

	/**
	 * Get the count of active key pairs (for monitoring/debugging).
	 * 
	 * @return Count of active key pairs
	 */
	@GetMapping("/key-count")
	public ResponseEntity<ApiResponse<Integer>> getActiveKeyCount() {
		log.info("GET /api/encryption/key-count - Get active key pair count");
		
		int count = keyManagementService.getActiveKeyPairCount();
		
		return ResponseEntity.ok(
				ApiResponse.success("Active key pair count retrieved", count));
	}
}
