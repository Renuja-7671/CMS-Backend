package com.epic.cms.exception;

/**
 * Exception thrown when insufficient funds/limits are available.
 */
public class InsufficientLimitException extends RuntimeException {
	
	public InsufficientLimitException(String message) {
		super(message);
	}
	
	public InsufficientLimitException(String limitType, String required, String available) {
		super(String.format("Insufficient %s limit. Required: %s, Available: %s", 
				limitType, required, available));
	}
}
