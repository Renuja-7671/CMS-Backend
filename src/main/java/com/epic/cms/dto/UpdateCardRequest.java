package com.epic.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating card details.
 * 
 * NOTE: Card number and card status cannot be updated through this endpoint.
 * - Card number is immutable (primary key)
 * - Card status can only be changed through the request approval process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {

	@NotNull(message = "Display card number is required")
	private String displayCardNumber; // Display card number (first6 + encrypted + last4)
	
	@NotNull(message = "Encryption key is required")
	private String encryptionKey; // Encryption key to decrypt the card number

	@NotNull(message = "Expiry date is required")
	private LocalDate expiryDate;

	@NotNull(message = "Credit limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Credit limit must be positive")
	private BigDecimal creditLimit;

	@NotNull(message = "Cash limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Cash limit must be positive")
	private BigDecimal cashLimit;

	@NotNull(message = "Available credit limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Available credit limit must be positive")
	private BigDecimal availableCreditLimit;

	@NotNull(message = "Available cash limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Available cash limit must be positive")
	private BigDecimal availableCashLimit;
}
