package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting to view plain card number.
 * Requires admin password verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewCardNumberRequest {

	/**
	 * Card ID to view the plain card number for
	 */
	@NotBlank(message = "Card ID is required")
	private String cardId;

	/**
	 * Admin password for verification
	 */
	@NotBlank(message = "Admin password is required")
	private String adminPassword;
}
