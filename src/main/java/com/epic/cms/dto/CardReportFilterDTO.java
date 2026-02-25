package com.epic.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for filtering card reports.
 * All fields are optional - null values mean no filter applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardReportFilterDTO {

	/**
	 * Filter by card status (IACT, CACT, DACT)
	 */
	private String cardStatus;

	/**
	 * Filter by expiry date - cards expiring on or after this date
	 */
	private LocalDate expiryDateFrom;

	/**
	 * Filter by expiry date - cards expiring on or before this date
	 */
	private LocalDate expiryDateTo;

	/**
	 * Filter by minimum credit limit
	 */
	private BigDecimal creditLimitMin;

	/**
	 * Filter by maximum credit limit
	 */
	private BigDecimal creditLimitMax;

	/**
	 * Filter by minimum cash limit
	 */
	private BigDecimal cashLimitMin;

	/**
	 * Filter by maximum cash limit
	 */
	private BigDecimal cashLimitMax;

	/**
	 * Search query for card number (encrypted value)
	 */
	private String searchQuery;
}
