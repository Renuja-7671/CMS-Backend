package com.epic.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic DTO for paginated responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
	
	/**
	 * List of items for current page
	 */
	private List<T> content;
	
	/**
	 * Current page number (0-indexed)
	 */
	private int page;
	
	/**
	 * Number of items per page
	 */
	private int size;
	
	/**
	 * Total number of items across all pages
	 */
	private long totalElements;
	
	/**
	 * Total number of pages
	 */
	private int totalPages;
	
	/**
	 * Whether this is the first page
	 */
	private boolean first;
	
	/**
	 * Whether this is the last page
	 */
	private boolean last;
	
	/**
	 * Whether there are more items after this page
	 */
	private boolean hasNext;
	
	/**
	 * Whether there are items before this page
	 */
	private boolean hasPrevious;
	
	/**
	 * Create a PageResponse from content and pagination info
	 */
	public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = (int) Math.ceil((double) totalElements / size);
		
		return PageResponse.<T>builder()
				.content(content)
				.page(page)
				.size(size)
				.totalElements(totalElements)
				.totalPages(totalPages)
				.first(page == 0)
				.last(page >= totalPages - 1)
				.hasNext(page < totalPages - 1)
				.hasPrevious(page > 0)
				.build();
	}
}
