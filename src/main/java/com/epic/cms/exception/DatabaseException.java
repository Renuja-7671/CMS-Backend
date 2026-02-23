package com.epic.cms.exception;

/**
 * Exception thrown when database operations fail.
 * This includes connection failures, query timeouts, constraint violations, etc.
 */
public class DatabaseException extends RuntimeException {
	
	public DatabaseException(String message) {
		super(message);
	}
	
	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}
}
