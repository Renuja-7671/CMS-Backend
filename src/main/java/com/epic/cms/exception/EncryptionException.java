package com.epic.cms.exception;

/**
 * Exception thrown when encryption or decryption operations fail.
 * This includes invalid keys, corrupted data, algorithm issues, etc.
 */
public class EncryptionException extends RuntimeException {
	
	public EncryptionException(String message) {
		super(message);
	}
	
	public EncryptionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public EncryptionException(String operation, String details) {
		super(operation + " failed: " + details);
	}
	
	public EncryptionException(String operation, String details, Throwable cause) {
		super(operation + " failed: " + details, cause);
	}
}
