package com.epic.cms.config;

import java.security.PrivateKey;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.epic.cms.dto.EncryptedResponse;
import com.epic.cms.service.KeyManagementService;
import com.epic.cms.util.EncryptionContext;
import com.epic.cms.util.PayloadEncryptionUtil;
import com.epic.cms.util.RSAEncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Advice to automatically encrypt response bodies for encrypted requests.
 * 
 * Flow:
 * 1. Controller returns response
 * 2. Advice intercepts the response
 * 3. If request was encrypted (has sessionId attribute), encrypt the response
 * 4. Use the SAME AES key that was used for the request (stored in request attribute)
 * 5. Encrypt response data with the same AES key
 * 6. Return encrypted response (client already has the AES key, so they can decrypt)
 * 
 * Note: This approach reuses the same AES key for request and response within a single transaction.
 * This is secure because:
 * - The AES key is never transmitted in plaintext (always encrypted with RSA)
 * - The key is random and unique per request
 * - The key is only used once (for both request and response)
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseEncryptionAdvice implements ResponseBodyAdvice<Object> {

	private final PayloadEncryptionUtil payloadEncryptionUtil;
	private final KeyManagementService keyManagementService;
	private final RSAEncryptionUtil rsaEncryptionUtil;
	private final ObjectMapper objectMapper;

	/**
	 * Paths that should be excluded from response encryption.
	 */
	private static final String[] EXCLUDED_PATHS = {
		"/api/encryption/public-key",
		"/actuator",
		"/swagger",
		"/v3/api-docs"
	};

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		// Support all return types
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {

		// Skip encryption for excluded paths
		String path = request.getURI().getPath();
		if (shouldSkipEncryption(path)) {
			log.debug("Skipping encryption for path: {}", path);
			return body;
		}

		// Skip encryption if response is already encrypted
		if (body instanceof EncryptedResponse) {
			log.debug("Response is already encrypted, skipping");
			return body;
		}

		// Skip encryption for file downloads (octet-stream)
		if (selectedContentType != null && selectedContentType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
			log.debug("Skipping encryption for binary content");
			return body;
		}

		// Skip encryption for PDF and CSV files
		if (selectedContentType != null && 
			(selectedContentType.toString().contains("pdf") || selectedContentType.toString().contains("csv"))) {
			log.debug("Skipping encryption for file download");
			return body;
		}

		// Check if we have encryption context from the request filter
		if (!EncryptionContext.hasSessionContext()) {
			log.debug("No encryption context found, returning unencrypted response");
			return body;
		}

		try {
			// Get session context from ThreadLocal
			EncryptionContext.SessionContext sessionContext = EncryptionContext.getSessionContext();
			String sessionId = sessionContext.getSessionId();
			String encryptedAesKey = sessionContext.getEncryptedAesKey();

			log.info("Encrypting response for session: {}", sessionId);

			// Retrieve private key for the session
			PrivateKey privateKey = keyManagementService.getPrivateKey(sessionId);
			if (privateKey == null) {
				log.warn("No private key found for session: {}, returning unencrypted response", sessionId);
				return body;
			}

			// Decrypt the AES key (which was used for the request)
			byte[] aesKey = rsaEncryptionUtil.decryptWithPrivateKey(encryptedAesKey, privateKey);
			String aesKeyBase64 = java.util.Base64.getEncoder().encodeToString(aesKey);

			// Encrypt the response body with the SAME AES key
			String encryptedData = payloadEncryptionUtil.encryptPayload(body, aesKeyBase64);

			// Create encrypted response DTO
			// We return the same encrypted AES key - client already has it decrypted
			EncryptedResponse encryptedResponse = new EncryptedResponse(
				sessionId,
				encryptedData,
				encryptedAesKey  // Same encrypted key as the request
			);

			log.info("Successfully encrypted response for session: {}", sessionId);
			return encryptedResponse;

		} catch (Exception e) {
			log.error("Failed to encrypt response, returning unencrypted", e);
			// On error, return unencrypted response
			return body;
		}
	}

	/**
	 * Check if encryption should be skipped for the given path.
	 */
	private boolean shouldSkipEncryption(String path) {
		for (String excludedPath : EXCLUDED_PATHS) {
			if (path.startsWith(excludedPath)) {
				return true;
			}
		}
		return false;
	}
}
