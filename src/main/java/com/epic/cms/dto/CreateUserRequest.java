package com.epic.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
	private String userName;

	@NotBlank(message = "Status is required")
	@Pattern(regexp = "^(ACT|DACT)$", message = "Status must be ACT or DACT")
	private String status;

	@NotBlank(message = "Name is required")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	private String name;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;
}
