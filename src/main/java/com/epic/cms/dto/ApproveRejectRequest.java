package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for approve/reject card request operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRejectRequest {

	/**
	 * Username of the user approving/rejecting the request.
	 */
	@NotBlank(message = "Approved user is required")
	private String approvedUser;
}
