package com.epic.cms.exception;

/**
 * Exception thrown when external service calls fail or timeout.
 * This includes network issues, service unavailability, etc.
 */
public class ExternalServiceException extends RuntimeException {
	
	private String serviceName;
	
	public ExternalServiceException(String serviceName, String message) {
		super("External service '" + serviceName + "' error: " + message);
		this.serviceName = serviceName;
	}
	
	public ExternalServiceException(String serviceName, String message, Throwable cause) {
		super("External service '" + serviceName + "' error: " + message, cause);
		this.serviceName = serviceName;
	}
	
	public String getServiceName() {
		return serviceName;
	}
}
