package com.epic.cms.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for card request responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestDTO {
	
	private Long requestId;
	private String displayCardNumber;  // First6 + Encrypted middle + Last4
	private String encryptionKey;      // Key to decrypt the card number
	private String requestType;
	private String requestTypeDescription;
	private String requestStatus;
	private String requestStatusDescription;
	private String reason;
	private LocalDateTime requestedAt;
	private LocalDateTime processedAt;
	private String requestedUser;
	private String approvedUser;
}
