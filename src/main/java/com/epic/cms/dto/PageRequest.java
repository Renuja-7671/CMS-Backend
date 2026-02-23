package com.epic.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pagination request parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
	
	/**
	 * Page number (0-indexed)
	 */
	@Min(value = 0, message = "Page number must be >= 0")
	private int page;
	
	/**
	 * Number of items per page
	 */
	@Min(value = 1, message = "Page size must be >= 1")
	@Max(value = 100, message = "Page size must be <= 100")
	private int size;
	
	/**
	 * Sort field (optional)
	 */
	private String sortBy;
	
	/**
	 * Sort direction: ASC or DESC (optional)
	 */
	private String sortDirection;
	
	/**
	 * Get offset for SQL query
	 */
	public int getOffset() {
		return page * size;
	}
	
	/**
	 * Create default page request
	 */
	public static PageRequest of(int page, int size) {
		return PageRequest.builder()
				.page(page)
				.size(size)
				.build();
	}
}
