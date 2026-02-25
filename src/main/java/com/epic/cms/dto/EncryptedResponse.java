package com.epic.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for encrypted response sent to client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedResponse {

	/**
	 * Session ID used for this encryption.
	 */
	private String sessionId;

	/**
	 * Base64-encoded AES-encrypted response data (includes IV prefix).
	 */
	private String encryptedData;

	/**
	 * Base64-encoded RSA-encrypted AES key.
	 */
	private String encryptedKey;
}
