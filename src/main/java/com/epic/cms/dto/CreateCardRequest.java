package com.epic.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

	@NotBlank(message = "Card number is required")
	@Size(min = 13, max = 16, message = "Card number must be between 13 and 16 digits")
	@Pattern(regexp = "^[0-9]+$", message = "Card number must contain only digits")
	private String cardNumber;

	@NotBlank(message = "Name on card is required")
	@Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
	private String nameOnCard;

	@NotNull(message = "Expiry date is required")
	private LocalDate expiryDate;

	@NotBlank(message = "CVV is required")
	@Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits")
	@Pattern(regexp = "^[0-9]+$", message = "CVV must contain only digits")
	private String cvv;

	@NotNull(message = "Credit limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Credit limit must be positive")
	private BigDecimal creditLimit;

	@NotNull(message = "Cash limit is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Cash limit must be positive")
	private BigDecimal cashLimit;

	@NotBlank(message = "Last updated user is required")
	private String lastUpdatedUser;
}
