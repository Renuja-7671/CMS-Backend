package com.epic.cms.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Card Request Report generation.
 * Contains all card request details needed for PDF and CSV export.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestReportDTO {
	
	/**
	 * Request ID
	 */
	private Long requestId;
	
	/**
	 * Encrypted card number (full encrypted value)
	 */
	private String encryptedCardNumber;
	
	/**
	 * Masked card number for display (e.g., 123456****1234)
	 */
	private String maskedCardNumber;
	
	/**
	 * Request type code (e.g., ACTI, CDCL)
	 */
	private String requestTypeCode;
	
	/**
	 * Request type description from CardRequestType table
	 */
	private String requestTypeDescription;
	
	/**
	 * Request status code (e.g., PEND, APPR, RJCT)
	 */
	private String requestStatusCode;
	
	/**
	 * Request status description from RequestStatus table
	 */
	private String requestStatusDescription;
	
	/**
	 * Reason for the request
	 */
	private String reason;
	
	/**
	 * Timestamp when request was created
	 */
	private LocalDateTime requestedAt;
	
	/**
	 * Timestamp when request was processed (approved/rejected)
	 */
	private LocalDateTime processedAt;
	
	/**
	 * Username of the person who requested
	 */
	private String requestedUserName;
	
	/**
	 * Full name of the person who requested
	 */
	private String requestedUserFullName;
	
	/**
	 * Username of the person who approved/rejected
	 */
	private String approvedUserName;
	
	/**
	 * Full name of the person who approved/rejected
	 */
	private String approvedUserFullName;
}
