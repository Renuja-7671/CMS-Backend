package com.epic.cms.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epic.cms.dto.CreateUserRequest;
import com.epic.cms.dto.UpdateUserRequest;
import com.epic.cms.dto.UserDTO;
import com.epic.cms.exception.DuplicateResourceException;
import com.epic.cms.exception.InvalidOperationException;
import com.epic.cms.exception.ResourceNotFoundException;
import com.epic.cms.exception.ValidationException;
import com.epic.cms.model.User;
import com.epic.cms.repository.UserRepository;
import com.epic.cms.util.AuditLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final AuditLogger auditLogger;

	/**
	 * Create a new user
	 */
	@Transactional
	public UserDTO createUser(CreateUserRequest request) {
		log.info("Creating new user: {}", request.getUserName());

		// Validate username doesn't already exist
		if (userRepository.existsByUserName(request.getUserName())) {
			auditLogger.logBusinessRuleViolation("USER_CREATE", 
				"Duplicate username: " + request.getUserName(), "SYSTEM");
			throw new DuplicateResourceException("User already exists with username: " + request.getUserName());
		}

		// Validate status
		if (!request.getStatus().matches("^(ACT|DACT)$")) {
			throw new ValidationException("Invalid status. Must be ACT or DACT");
		}

		// Create user entity
		User user = User.builder()
				.userName(request.getUserName())
				.status(request.getStatus())
				.name(request.getName())
				.description(request.getDescription())
				.createdAt(LocalDateTime.now())
				.lastUpdateTime(LocalDateTime.now())
				.build();

		// Save user
		User savedUser = userRepository.save(user);

		// Audit log
		auditLogger.logBusinessRuleViolation("USER_CREATED", 
			"User created: " + savedUser.getUserName() + " | Status: " + savedUser.getStatus(), 
			"SYSTEM");

		log.info("User created successfully: {}", savedUser.getUserName());
		return mapToDTO(savedUser);
	}

	/**
	 * Get all users
	 */
	@Transactional(readOnly = true)
	public List<UserDTO> getAllUsers() {
		log.debug("Fetching all users");
		List<User> users = userRepository.findAllUsers();
		return users.stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Get user by username
	 */
	@Transactional(readOnly = true)
	public UserDTO getUserByUserName(String userName) {
		log.debug("Fetching user: {}", userName);
		User user = userRepository.findByUserName(userName)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userName));
		return mapToDTO(user);
	}

	/**
	 * Get users by status
	 */
	@Transactional(readOnly = true)
	public List<UserDTO> getUsersByStatus(String status) {
		log.debug("Fetching users with status: {}", status);
		
		// Validate status
		if (!status.matches("^(ACT|DACT)$")) {
			throw new ValidationException("Invalid status. Must be ACT or DACT");
		}

		List<User> users = userRepository.findByStatus(status);
		return users.stream()
				.map(this::mapToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Update user information
	 */
	@Transactional
	public UserDTO updateUser(UpdateUserRequest request) {
		log.info("Updating user: {}", request.getUserName());

		// Check if user exists
		User existingUser = userRepository.findByUserName(request.getUserName())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserName()));

		// Update fields if provided
		String newName = request.getName() != null ? request.getName() : existingUser.getName();
		String newDescription = request.getDescription() != null ? request.getDescription() : existingUser.getDescription();
		String newStatus = request.getStatus() != null ? request.getStatus() : existingUser.getStatus();

		// Validate status if provided
		if (request.getStatus() != null && !request.getStatus().matches("^(ACT|DACT)$")) {
			throw new ValidationException("Invalid status. Must be ACT or DACT");
		}

		// Update user
		int updatedRows = userRepository.updateUser(request.getUserName(), newName, newDescription);
		if (updatedRows == 0) {
			throw new InvalidOperationException("Failed to update user: " + request.getUserName());
		}

		// Update status if changed
		if (request.getStatus() != null && !request.getStatus().equals(existingUser.getStatus())) {
			int statusUpdated = userRepository.updateStatus(request.getUserName(), newStatus);
			if (statusUpdated == 0) {
				throw new InvalidOperationException("Failed to update user status: " + request.getUserName());
			}
			
			// Audit log for status change
			auditLogger.logBusinessRuleViolation("USER_STATUS_CHANGE", 
				"User: " + request.getUserName() + " | Old Status: " + existingUser.getStatus() + 
				" | New Status: " + newStatus, "SYSTEM");
		}

		// Audit log
		auditLogger.logBusinessRuleViolation("USER_UPDATED", 
			"User updated: " + request.getUserName(), "SYSTEM");

		log.info("User updated successfully: {}", request.getUserName());

		// Fetch and return updated user
		return getUserByUserName(request.getUserName());
	}

	/**
	 * Update user status
	 */
	@Transactional
	public UserDTO updateUserStatus(String userName, String newStatus) {
		log.info("Updating user status: {} to {}", userName, newStatus);

		// Validate status
		if (!newStatus.matches("^(ACT|DACT)$")) {
			throw new ValidationException("Invalid status. Must be ACT or DACT");
		}

		// Check if user exists
		User existingUser = userRepository.findByUserName(userName)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userName));

		// Check if status is already the same
		if (existingUser.getStatus().equals(newStatus)) {
			throw new InvalidOperationException("User is already in status: " + newStatus);
		}

		// Update status
		int updatedRows = userRepository.updateStatus(userName, newStatus);
		if (updatedRows == 0) {
			throw new InvalidOperationException("Failed to update user status: " + userName);
		}

		// Audit log
		auditLogger.logBusinessRuleViolation("USER_STATUS_CHANGE", 
			"User: " + userName + " | Old Status: " + existingUser.getStatus() + 
			" | New Status: " + newStatus, "SYSTEM");

		log.info("User status updated successfully: {}", userName);

		// Fetch and return updated user
		return getUserByUserName(userName);
	}

	/**
	 * Delete user (soft delete by deactivating)
	 */
	@Transactional
	public void deleteUser(String userName) {
		log.info("Deleting (deactivating) user: {}", userName);

		// Check if user exists
		User existingUser = userRepository.findByUserName(userName)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userName));

		// Soft delete by setting status to DACT
		if (existingUser.getStatus().equals("DACT")) {
			throw new InvalidOperationException("User is already deactivated: " + userName);
		}

		int updatedRows = userRepository.updateStatus(userName, "DACT");
		if (updatedRows == 0) {
			throw new InvalidOperationException("Failed to deactivate user: " + userName);
		}

		// Audit log
		auditLogger.logBusinessRuleViolation("USER_DELETED", 
			"User deactivated: " + userName, "SYSTEM");

		log.info("User deactivated successfully: {}", userName);
	}

	/**
	 * Get user count by status
	 */
	@Transactional(readOnly = true)
	public long getUserCountByStatus(String status) {
		log.debug("Counting users with status: {}", status);
		
		// Validate status
		if (!status.matches("^(ACT|DACT)$")) {
			throw new ValidationException("Invalid status. Must be ACT or DACT");
		}

		return userRepository.countByStatus(status);
	}

	/**
	 * Map User entity to UserDTO
	 */
	private UserDTO mapToDTO(User user) {
		return UserDTO.builder()
				.userName(user.getUserName())
				.status(user.getStatus())
				.name(user.getName())
				.description(user.getDescription())
				.createdAt(user.getCreatedAt())
				.lastUpdateTime(user.getLastUpdateTime())
				.build();
	}
}
