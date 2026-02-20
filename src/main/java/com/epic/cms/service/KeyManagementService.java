package com.epic.cms.service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.epic.cms.util.RSAEncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing RSA key pairs for secure payload encryption.
 * Handles key generation, storage, retrieval, and expiration.
 * 
 * Security Features:
 * - Keys are stored in-memory only (never persisted to disk)
 * - Each key pair has a unique session ID
 * - Keys expire after 5 minutes (configurable)
 * - Expired keys are automatically cleaned up
 * - Private keys are never exposed to clients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyManagementService {

	private final RSAEncryptionUtil rsaEncryptionUtil;

	// TTL for key pairs (5 minutes)
	private static final long KEY_TTL_SECONDS = 300;

	// In-memory storage for key pairs
	// Key: sessionId, Value: KeyPairEntry
	private final Map<String, KeyPairEntry> keyStore = new ConcurrentHashMap<>();

	/**
	 * Generate a new RSA key pair and store it with a unique session ID.
	 * 
	 * @return Session ID and public key
	 */
	public KeyPairInfo generateKeyPair() {
		// Generate unique session ID
		String sessionId = UUID.randomUUID().toString();

		// Generate RSA key pair
		KeyPair keyPair = rsaEncryptionUtil.generateKeyPair();

		// Store key pair with expiration time
		LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(KEY_TTL_SECONDS);
		KeyPairEntry entry = new KeyPairEntry(keyPair, expiryTime);
		keyStore.put(sessionId, entry);

		// Encode public key for transmission
		String publicKey = rsaEncryptionUtil.encodePublicKey(keyPair.getPublic());

		log.info("Generated new key pair with session ID: {}, expires at: {}", sessionId, expiryTime);
		log.debug("Total active key pairs: {}", keyStore.size());

		return new KeyPairInfo(sessionId, publicKey, expiryTime, KEY_TTL_SECONDS);
	}

	/**
	 * Retrieve private key for a given session ID.
	 * 
	 * @param sessionId Session ID
	 * @return Private key if found and not expired, null otherwise
	 */
	public PrivateKey getPrivateKey(String sessionId) {
		KeyPairEntry entry = keyStore.get(sessionId);

		if (entry == null) {
			log.warn("No key pair found for session ID: {}", sessionId);
			return null;
		}

		// Check if key has expired
		if (LocalDateTime.now().isAfter(entry.getExpiryTime())) {
			log.warn("Key pair expired for session ID: {}", sessionId);
			keyStore.remove(sessionId);
			return null;
		}

		log.debug("Retrieved private key for session ID: {}", sessionId);
		return entry.getKeyPair().getPrivate();
	}

	/**
	 * Invalidate a key pair after use (optional security measure).
	 * 
	 * @param sessionId Session ID
	 */
	public void invalidateKeyPair(String sessionId) {
		KeyPairEntry removed = keyStore.remove(sessionId);
		if (removed != null) {
			log.info("Invalidated key pair for session ID: {}", sessionId);
		}
	}

	/**
	 * Scheduled cleanup task to remove expired key pairs.
	 * Runs every minute.
	 */
	@Scheduled(fixedRate = 60000) // Run every 60 seconds
	public void cleanupExpiredKeys() {
		LocalDateTime now = LocalDateTime.now();
		
		// Use array to make it effectively final for lambda
		final int[] removedCount = {0};

		// Remove expired entries
		keyStore.entrySet().removeIf(entry -> {
			if (now.isAfter(entry.getValue().getExpiryTime())) {
				log.debug("Removing expired key pair: {}", entry.getKey());
				removedCount[0]++;
				return true;
			}
			return false;
		});

		if (removedCount[0] > 0) {
			log.info("Cleaned up {} expired key pair(s). Active key pairs: {}", removedCount[0], keyStore.size());
		}
	}

	/**
	 * Get current number of active key pairs (for monitoring).
	 * 
	 * @return Number of active key pairs
	 */
	public int getActiveKeyPairCount() {
		return keyStore.size();
	}

	/**
	 * Inner class to store key pair with expiration time.
	 */
	private static class KeyPairEntry {
		private final KeyPair keyPair;
		private final LocalDateTime expiryTime;

		public KeyPairEntry(KeyPair keyPair, LocalDateTime expiryTime) {
			this.keyPair = keyPair;
			this.expiryTime = expiryTime;
		}

		public KeyPair getKeyPair() {
			return keyPair;
		}

		public LocalDateTime getExpiryTime() {
			return expiryTime;
		}
	}

	/**
	 * Data class for key pair information returned to client.
	 */
	public static class KeyPairInfo {
		private final String sessionId;
		private final String publicKey;
		private final LocalDateTime expiryTime;
		private final long ttlSeconds;

		public KeyPairInfo(String sessionId, String publicKey, LocalDateTime expiryTime, long ttlSeconds) {
			this.sessionId = sessionId;
			this.publicKey = publicKey;
			this.expiryTime = expiryTime;
			this.ttlSeconds = ttlSeconds;
		}

		public String getSessionId() {
			return sessionId;
		}

		public String getPublicKey() {
			return publicKey;
		}

		public LocalDateTime getExpiryTime() {
			return expiryTime;
		}

		public long getTtlSeconds() {
			return ttlSeconds;
		}
	}
}
