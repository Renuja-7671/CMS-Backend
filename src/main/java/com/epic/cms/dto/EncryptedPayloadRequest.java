package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving encrypted payload from client.
 * The client encrypts the entire request payload using AES-GCM encryption
 * for enhanced security during transmission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedPayloadRequest {

    /**
     * Base64-encoded encrypted payload (includes IV prefix)
     */
    @NotBlank(message = "Encrypted data is required")
    private String encryptedData;

    /**
     * Base64-encoded encryption key used to encrypt the payload
     * This key is generated client-side and sent with the request
     */
    @NotBlank(message = "Encryption key is required")
    private String encryptionKey;

    /**
     * Optional: Payload type identifier to help backend determine how to process
     * Examples: "CREATE_CARD", "UPDATE_CARD", "CREATE_REQUEST"
     */
    private String payloadType;
}
