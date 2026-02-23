package com.epic.cms.exception;

/**
 * Exception thrown when request validation fails.
 * This is used for custom validation logic beyond Jakarta Bean Validation.
 */
public class ValidationException extends RuntimeException {
	
	private String fieldName;
	private Object rejectedValue;
	
	public ValidationException(String message) {
		super(message);
	}
	
	public ValidationException(String fieldName, String message) {
		super(fieldName + ": " + message);
		this.fieldName = fieldName;
	}
	
	public ValidationException(String fieldName, Object rejectedValue, String message) {
		super(fieldName + ": " + message + " (rejected value: " + rejectedValue + ")");
		this.fieldName = fieldName;
		this.rejectedValue = rejectedValue;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public Object getRejectedValue() {
		return rejectedValue;
	}
}
