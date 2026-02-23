package com.epic.cms.exception;

/**
 * Exception thrown for system-level errors that shouldn't normally occur.
 * This includes NullPointerException, ClassCastException, etc.
 */
public class SystemException extends RuntimeException {
	
	public SystemException(String message) {
		super(message);
	}
	
	public SystemException(String message, Throwable cause) {
		super(message, cause);
	}
}
