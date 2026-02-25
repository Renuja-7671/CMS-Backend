package com.epic.cms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Card")
public class Card {

	@Id
	@Column("CardNumber")
	private String cardNumber; // Encrypted card number (primary key)

	@Column("ExpiryDate")
	private LocalDate expiryDate;

	@Column("CardStatus")
	private String cardStatus;

	@Column("CreditLimit")
	private BigDecimal creditLimit;

	@Column("CashLimit")
	private BigDecimal cashLimit;

	@Column("AvailableCreditLimit")
	private BigDecimal availableCreditLimit;

	@Column("AvailableCashLimit")
	private BigDecimal availableCashLimit;

	@Column("LastUpdatedUser")
	private String lastUpdatedUser;

	@Column("LastUpdateTime")
	private LocalDateTime lastUpdateTime;
}
