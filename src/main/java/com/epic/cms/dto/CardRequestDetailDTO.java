package com.epic.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for card request details including full card information.
 * Used for request confirmation/review page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestDetailDTO {
	
	// Request Information
	private Long requestId;
	private String requestType;
	private String requestTypeDescription;
	private String requestStatus;
	private String requestStatusDescription;
	private String reason;
	private LocalDateTime requestedAt;
	private LocalDateTime processedAt;
	
	// Card Information (encrypted for API response)
	private String displayCardNumber;
	private String encryptionKey;
	private LocalDate expiryDate;
	private String cardStatus;
	private String cardStatusDescription;
	private BigDecimal creditLimit;
	private BigDecimal cashLimit;
	private BigDecimal availableCreditLimit;
	private BigDecimal availableCashLimit;
	private BigDecimal usedCreditLimit;
	private BigDecimal usedCashLimit;
	private LocalDateTime lastUpdateTime;
}
