package com.epic.cms.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.epic.cms.dto.SecureEncryptedPayloadRequest;
import com.epic.cms.service.KeyManagementService;
import com.epic.cms.util.EncryptionContext;
import com.epic.cms.util.PayloadEncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter to decrypt incoming encrypted requests.
 * Automatically decrypts requests with encrypted payloads and makes the decrypted data
 * available to controllers.
 * 
 * Flow:
 * 1. Client sends encrypted request with sessionId, encryptedData, and encryptedKey
 * 2. Filter intercepts the request
 * 3. Filter decrypts the payload using RSA + AES hybrid encryption
 * 4. Filter wraps the request with decrypted payload
 * 5. Controller receives decrypted data as normal JSON
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RequestDecryptionFilter extends OncePerRequestFilter {

	private final PayloadEncryptionUtil payloadEncryptionUtil;
	private final KeyManagementService keyManagementService;
	private final ObjectMapper objectMapper;

	/**
	 * Paths that should be excluded from encryption (public endpoints).
	 */
	private static final String[] EXCLUDED_PATHS = {
		"/api/encryption/public-key",  // Public key distribution endpoint
		"/api/cards/secure",            // Legacy endpoint with manual decryption
		"/actuator",                    // Spring Boot Actuator endpoints (if enabled)
		"/swagger",                     // Swagger UI (if enabled)
		"/v3/api-docs"                  // OpenAPI docs (if enabled)
	};

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Clear any previous encryption context
		EncryptionContext.clear();

		// Skip encryption for excluded paths
		String requestPath = request.getRequestURI();
		if (shouldSkipDecryption(requestPath)) {
			log.debug("Skipping decryption for path: {}", requestPath);
			filterChain.doFilter(request, response);
			EncryptionContext.clear(); // Clean up after request
			return;
		}

		// Only decrypt POST/PUT/PATCH requests with content
		String method = request.getMethod();
		if (!("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
			log.debug("Skipping decryption for {} request to {}", method, requestPath);
			filterChain.doFilter(request, response);
			EncryptionContext.clear(); // Clean up after request
			return;
		}

		try {
			// Read request body directly from input stream
			// NOTE: We must read the input stream BEFORE wrapping, as it can only be read once
			byte[] body = request.getInputStream().readAllBytes();
			
			if (body.length == 0) {
				// No body, proceed without decryption
				log.debug("Empty request body, skipping decryption");
				filterChain.doFilter(request, response);
				EncryptionContext.clear(); // Clean up after request
				return;
			}

			String bodyString = new String(body, StandardCharsets.UTF_8);
			log.debug("Request body length: {} bytes", body.length);

			// Check if request contains encrypted payload
			SecureEncryptedPayloadRequest encryptedRequest;
			try {
				encryptedRequest = objectMapper.readValue(bodyString, SecureEncryptedPayloadRequest.class);
			} catch (Exception e) {
				// Not an encrypted payload, proceed without decryption
				log.debug("Request is not encrypted, proceeding normally");
				// Wrap original request to replay the body we already read
				DecryptedRequestWrapper replayRequest = new DecryptedRequestWrapper(request, bodyString);
				filterChain.doFilter(replayRequest, response);
				EncryptionContext.clear(); // Clean up after request
				return;
			}

			// Validate encrypted request fields
			if (encryptedRequest.getSessionId() == null || 
				encryptedRequest.getEncryptedData() == null || 
				encryptedRequest.getEncryptedKey() == null) {
				// Not a valid encrypted payload, proceed without decryption
				log.debug("Request does not contain valid encrypted payload fields");
				// Wrap original request to replay the body we already read
				DecryptedRequestWrapper replayRequest = new DecryptedRequestWrapper(request, bodyString);
				filterChain.doFilter(replayRequest, response);
				EncryptionContext.clear(); // Clean up after request
				return;
			}

			log.info("Decrypting request for session: {}", encryptedRequest.getSessionId());

			// Retrieve private key for the session
			PrivateKey privateKey = keyManagementService.getPrivateKey(encryptedRequest.getSessionId());
			if (privateKey == null) {
				log.error("No private key found for session: {}", encryptedRequest.getSessionId());
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired session");
				EncryptionContext.clear(); // Clean up after request
				return;
			}

			// Decrypt the payload - decrypt as generic Object first
			String decryptedJson = decryptPayloadToJson(
				encryptedRequest.getEncryptedData(),
				encryptedRequest.getEncryptedKey(),
				privateKey
			);

			log.info("Successfully decrypted request payload for session: {}", encryptedRequest.getSessionId());

			// Store session context in ThreadLocal for response encryption
			EncryptionContext.setSessionContext(
				encryptedRequest.getSessionId(),
				encryptedRequest.getEncryptedKey()
			);

			// Create a new request wrapper with decrypted payload
			DecryptedRequestWrapper decryptedRequest = new DecryptedRequestWrapper(request, decryptedJson);

			// Continue with the decrypted request
			filterChain.doFilter(decryptedRequest, response);

		} catch (Exception e) {
			log.error("Failed to decrypt request", e);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to decrypt request: " + e.getMessage());
		} finally {
			// Always clear encryption context after request completes
			EncryptionContext.clear();
		}
	}

	/**
	 * Decrypt encrypted payload to JSON string.
	 * Also stores the decrypted AES key in request attribute for response encryption.
	 */
	private String decryptPayloadToJson(String encryptedData, String encryptedKey, PrivateKey privateKey) {
		// We need to decrypt to a raw JSON string, not a specific class
		// This is a workaround - decrypt to Object and convert back to JSON
		Object decryptedObject = payloadEncryptionUtil.decryptPayloadWithRSA(
			encryptedData,
			encryptedKey,
			privateKey,
			Object.class
		);

		try {
			return objectMapper.writeValueAsString(decryptedObject);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize decrypted payload", e);
		}
	}

	/**
	 * Check if decryption should be skipped for the given path.
	 */
	private boolean shouldSkipDecryption(String path) {
		for (String excludedPath : EXCLUDED_PATHS) {
			if (path.startsWith(excludedPath)) {
				return true;
			}
		}
		return false;
	}
}
