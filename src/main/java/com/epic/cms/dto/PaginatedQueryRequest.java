package com.epic.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for paginated query requests (converted from GET to POST for encryption).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedQueryRequest {

	/**
	 * Page number (default 0).
	 */
	private int page = 0;

	/**
	 * Page size (default 10).
	 */
	private int size = 10;

	/**
	 * Optional status filter.
	 */
	private String status;

	/**
	 * Optional search query.
	 */
	private String search;

	/**
	 * Optional request type filter (for card requests).
	 */
	private String requestType;

	/**
	 * Optional month filter (for card reports).
	 */
	private Integer month;

	/**
	 * Optional year filter (for card reports).
	 */
	private Integer year;
}
