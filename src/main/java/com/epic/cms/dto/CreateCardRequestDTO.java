package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new card request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequestDTO {
	
	@NotBlank(message = "Display card number is required")
	private String displayCardNumber; // First6 + Encrypted middle + Last4
	
	@NotBlank(message = "Encryption key is required")
	private String encryptionKey; // Key to decrypt the card number
	
	@NotBlank(message = "Request type is required")
	@Pattern(regexp = "^(ACTI|CDCL)$", message = "Request type must be either ACTI (Activation) or CDCL (Card Close)")
	private String requestType;
	
	private String reason;
}
