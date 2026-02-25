package com.epic.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning plain card number after admin password verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewCardNumberResponse {

	/**
	 * Card ID
	 */
	private String cardId;

	/**
	 * Plain (unmasked) card number
	 */
	private String plainCardNumber;

	/**
	 * Masked card number (for reference)
	 */
	private String maskedCardNumber;
}
