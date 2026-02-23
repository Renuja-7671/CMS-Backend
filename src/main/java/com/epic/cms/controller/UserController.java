package com.epic.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epic.cms.dto.ApiResponse;
import com.epic.cms.dto.CreateUserRequest;
import com.epic.cms.dto.UpdateUserRequest;
import com.epic.cms.dto.UserDTO;
import com.epic.cms.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

	private final UserService userService;

	/**
	 * Create a new user
	 * POST /api/users
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
		log.info("REST Request to create user: {}", request.getUserName());
		UserDTO user = userService.createUser(request);
		return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
	}

	/**
	 * Get all users
	 * GET /api/users
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
		log.info("REST Request to get all users");
		List<UserDTO> users = userService.getAllUsers();
		return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
	}

	/**
	 * Get user by username
	 * GET /api/users/{userName}
	 */
	@GetMapping("/{userName}")
	public ResponseEntity<ApiResponse<UserDTO>> getUserByUserName(@PathVariable String userName) {
		log.info("REST Request to get user: {}", userName);
		UserDTO user = userService.getUserByUserName(userName);
		return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
	}

	/**
	 * Get users by status
	 * GET /api/users/status/{status}
	 */
	@GetMapping("/status/{status}")
	public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByStatus(
			@PathVariable 
			@Pattern(regexp = "^(ACT|DACT)$", message = "Status must be ACT or DACT") 
			String status) {
		log.info("REST Request to get users by status: {}", status);
		List<UserDTO> users = userService.getUsersByStatus(status);
		return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
	}

	/**
	 * Update user information
	 * PUT /api/users
	 */
	@PutMapping
	public ResponseEntity<ApiResponse<UserDTO>> updateUser(@Valid @RequestBody UpdateUserRequest request) {
		log.info("REST Request to update user: {}", request.getUserName());
		UserDTO user = userService.updateUser(request);
		return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
	}

	/**
	 * Update user status
	 * PATCH /api/users/{userName}/status
	 */
	@PatchMapping("/{userName}/status")
	public ResponseEntity<ApiResponse<UserDTO>> updateUserStatus(
			@PathVariable String userName,
			@RequestParam @Pattern(regexp = "^(ACT|DACT)$", message = "Status must be ACT or DACT") String status) {
		log.info("REST Request to update user status: {} to {}", userName, status);
		UserDTO user = userService.updateUserStatus(userName, status);
		return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
	}

	/**
	 * Delete (deactivate) user
	 * DELETE /api/users/{userName}
	 */
	@DeleteMapping("/{userName}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userName) {
		log.info("REST Request to delete (deactivate) user: {}", userName);
		userService.deleteUser(userName);
		return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
	}

	/**
	 * Get user count by status
	 * GET /api/users/count/status/{status}
	 */
	@GetMapping("/count/status/{status}")
	public ResponseEntity<ApiResponse<Long>> getUserCountByStatus(
			@PathVariable 
			@Pattern(regexp = "^(ACT|DACT)$", message = "Status must be ACT or DACT") 
			String status) {
		log.info("REST Request to count users by status: {}", status);
		long count = userService.getUserCountByStatus(status);
		return ResponseEntity.ok(ApiResponse.success("User count retrieved successfully", count));
	}
}
