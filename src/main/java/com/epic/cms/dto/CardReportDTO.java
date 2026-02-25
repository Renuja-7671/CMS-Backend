package com.epic.cms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Card Report generation.
 * Contains all card details needed for PDF and CSV export.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardReportDTO {
	
	/**
	 * Encrypted card number (full encrypted value)
	 */
	private String encryptedCardNumber;
	
	/**
	 * Masked card number for display (e.g., ****1234)
	 */
	private String maskedCardNumber;
	
	/**
	 * Card expiry date
	 */
	private LocalDate expiryDate;
	
	/**
	 * Card status code (e.g., ACT, IACT, DACT)
	 */
	private String statusCode;
	
	/**
	 * Card status description from Status table
	 */
	private String statusDescription;
	
	/**
	 * Credit limit amount
	 */
	private Double creditLimit;
	
	/**
	 * Cash limit amount
	 */
	private Double cashLimit;
	
	/**
	 * Available credit limit
	 */
	private Double availableCreditLimit;
	
	/**
	 * Available cash limit
	 */
	private Double availableCashLimit;
	
	/**
	 * Last updated timestamp
	 */
	private LocalDateTime lastUpdatedTime;
	
	/**
	 * Username of the person who last updated the card
	 */
	private String lastUpdatedUserName;
	
	/**
	 * Full name of the person who last updated the card
	 */
	private String lastUpdatedUserFullName;
}
