package com.epic.cms.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.epic.cms.dto.ApiResponse;
import com.epic.cms.util.AuditLogger;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler that catches ALL exceptions and returns 200 OK responses
 * with success: false flag.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final AuditLogger auditLogger;

	/**
	 * Handle resource not found errors (404).
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle duplicate resource errors (409).
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
		log.warn("Duplicate resource: {}", ex.getMessage());
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle invalid operation errors (business logic violations).
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(InvalidOperationException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidOperationException(InvalidOperationException ex) {
		log.warn("Invalid operation: {}", ex.getMessage());
		
		// Audit log: Business rule violation
		auditLogger.logBusinessRuleViolation(
			ex.getClass().getSimpleName(),
			ex.getMessage(),
			"SYSTEM"
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle insufficient limit errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(InsufficientLimitException.class)
	public ResponseEntity<ApiResponse<Void>> handleInsufficientLimitException(InsufficientLimitException ex) {
		log.warn("Insufficient limit: {}", ex.getMessage());
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle custom validation errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
		log.warn("Validation error: {}", ex.getMessage());
		
		// Audit log: Validation error
		auditLogger.logValidationError(
			"Field Validation",
			ex.getFieldName(),
			ex.getMessage(),
			"SYSTEM"
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle Jakarta Bean Validation errors (@Valid annotation).
	 * Returns 200 OK with success: false and detailed field errors.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		log.warn("Request validation failed: {}", errors);
		
		// Build detailed error message
		String message = "Invalid input data. Please check the following fields: " + String.join(", ", errors.keySet());
		
		return ResponseEntity.ok(
			ApiResponse.<Map<String, String>>builder()
				.success(false)
				.message(message)
				.data(errors)
				.build()
		);
	}

	/**
	 * Handle database exceptions.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(DatabaseException.class)
	public ResponseEntity<ApiResponse<Void>> handleDatabaseException(DatabaseException ex) {
		log.error("Database error: {}", ex.getMessage(), ex);
		
		// Audit log: System error
		auditLogger.logSystemError(
			"Database Operation",
			ex.getMessage(),
			getStackTraceAsString(ex)
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("A database error occurred. Please try again later or contact support if the issue persists.")
				.data(null)
				.build()
		);
	}

	/**
	 * Handle Spring Data Access exceptions (low-level database errors).
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
		log.error("Data access error: {}", ex.getMessage(), ex);
		
		// Determine specific error type
		String message;
		if (ex instanceof DataIntegrityViolationException) {
			message = "Data integrity violation. The operation violates a database constraint.";
		} else if (ex instanceof QueryTimeoutException) {
			message = "Database query timeout. Please try again later.";
		} else {
			message = "A database error occurred. Please try again later.";
		}
		
		// Audit log: System error
		auditLogger.logSystemError(
			"Database Access",
			ex.getMessage(),
			getStackTraceAsString(ex)
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message(message)
				.data(null)
				.build()
		);
	}

	/**
	 * Handle encryption/decryption errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(EncryptionException.class)
	public ResponseEntity<ApiResponse<Void>> handleEncryptionException(EncryptionException ex) {
		log.error("Encryption error: {}", ex.getMessage(), ex);
		
		// Audit log: Security issue
		auditLogger.logDecryptionFailure(
			"Encryption Operation",
			ex.getMessage()
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("An encryption error occurred. Please try again or request a new encryption key.")
				.data(null)
				.build()
		);
	}

	/**
	 * Handle external service errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException ex) {
		log.error("External service error: {}", ex.getMessage(), ex);
		
		// Audit log: System error
		auditLogger.logSystemError(
			"External Service: " + ex.getServiceName(),
			ex.getMessage(),
			getStackTraceAsString(ex)
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("An external service is temporarily unavailable. Please try again later.")
				.data(null)
				.build()
		);
	}

	/**
	 * Handle system-level errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(SystemException.class)
	public ResponseEntity<ApiResponse<Void>> handleSystemException(SystemException ex) {
		log.error("System error: {}", ex.getMessage(), ex);
		
		// Audit log: System error
		auditLogger.logSystemError(
			"System Error",
			ex.getMessage(),
			getStackTraceAsString(ex)
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("A system error occurred. Our team has been notified. Please try again later.")
				.data(null)
				.build()
		);
	}

	/**
	 * Handle IllegalArgumentException (common programming error).
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
		log.warn("Invalid argument: {}", ex.getMessage());
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("Invalid request: " + ex.getMessage())
				.data(null)
				.build()
		);
	}

	/**
	 * Handle NullPointerException.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
		log.error("Null pointer error: ", ex);
		
		// Audit log: System error
		auditLogger.logSystemError(
			"NullPointerException",
			"Unexpected null value encountered",
			getStackTraceAsString(ex)
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("An unexpected error occurred. Please try again or contact support.")
				.data(null)
				.build()
		);
	}

	/**
	 * Handle JSON parsing errors.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler({JsonMappingException.class, InvalidFormatException.class})
	public ResponseEntity<ApiResponse<Void>> handleJsonParsingException(Exception ex) {
		log.warn("JSON parsing error: {}", ex.getMessage());
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("Invalid JSON format in request. Please check your request data.")
				.data(null)
				.build()
		);
	}

	/**
	 * Catch-all handler for ANY other exception.
	 * This ensures NO exception ever returns an HTTP error code.
	 * Returns 200 OK with success: false.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
		log.error("Unexpected error occurred: ", ex);
		
		// Audit log: System error
		String stackTrace = getStackTraceAsString(ex);
		auditLogger.logSystemError(
			ex.getClass().getSimpleName(),
			ex.getMessage(),
			stackTrace
		);
		
		return ResponseEntity.ok(
			ApiResponse.<Void>builder()
				.success(false)
				.message("An unexpected error occurred. Epic team has been notified. Please try again later.")
				.data(null)
				.build()
		);
	}
	
	/**
	 * Convert exception stack trace to string for logging.
	 * Limits to first 1000 characters to avoid excessive log sizes.
	 */
	private String getStackTraceAsString(Throwable ex) {
		StringBuilder sb = new StringBuilder();
		sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
		for (StackTraceElement element : ex.getStackTrace()) {
			sb.append("\tat ").append(element.toString()).append("\n");
			if (sb.length() > 1000) {
				sb.append("\t... (truncated)");
				break;
			}
		}
		return sb.toString();
	}
}
