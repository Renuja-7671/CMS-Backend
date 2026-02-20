package com.epic.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String cardNumber; // Plain card number (only for internal use, excluded from JSON by default)
	
	private String displayCardNumber; // Formatted card number: first6 + encrypted_middle + last4
	private String encryptionKey; // Key to decrypt the middle digits (sent with response for later use)
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
