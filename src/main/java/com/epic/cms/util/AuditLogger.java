package com.epic.cms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Centralized audit logging utility for financial transactions.
 * Logs all critical operations including card creation, status changes,
 * request approvals/rejections, and limit modifications.
 * 
 * Audit logs are kept for 7 years for financial compliance (PCI-DSS, SOX).
 */
@Component
public class AuditLogger {

	private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
	private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");
	private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS");

	/**
	 * Log card creation.
	 */
	public void logCardCreated(String cardNumberMasked, String status, String creditLimit, String cashLimit, String user) {
		setAuditContext("CARD_CREATE", user);
		AUDIT_LOG.info("CARD_CREATED | CardNumber={} | Status={} | CreditLimit={} | CashLimit={} | Timestamp={}",
				cardNumberMasked, status, creditLimit, cashLimit, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card status change.
	 */
	public void logCardStatusChange(String cardNumberMasked, String oldStatus, String newStatus, String reason, String user) {
		setAuditContext("CARD_STATUS_CHANGE", user);
		AUDIT_LOG.info("CARD_STATUS_CHANGED | CardNumber={} | OldStatus={} | NewStatus={} | Reason={} | Timestamp={}",
				cardNumberMasked, oldStatus, newStatus, reason, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card deletion.
	 */
	public void logCardDeleted(String cardNumberMasked, String user) {
		setAuditContext("CARD_DELETE", user);
		AUDIT_LOG.warn("CARD_DELETED | CardNumber={} | Timestamp={}",
				cardNumberMasked, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card limit update.
	 */
	public void logCardLimitUpdate(String cardNumberMasked, String limitType, String oldValue, String newValue, String user) {
		setAuditContext("CARD_LIMIT_UPDATE", user);
		AUDIT_LOG.info("CARD_LIMIT_UPDATED | CardNumber={} | LimitType={} | OldValue={} | NewValue={} | Timestamp={}",
				cardNumberMasked, limitType, oldValue, newValue, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card request creation.
	 */
	public void logRequestCreated(Long requestId, String cardNumberMasked, String requestType, String user) {
		setAuditContext("REQUEST_CREATE", user);
		AUDIT_LOG.info("REQUEST_CREATED | RequestId={} | CardNumber={} | RequestType={} | Timestamp={}",
				requestId, cardNumberMasked, requestType, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card request approval.
	 */
	public void logRequestApproved(Long requestId, String cardNumberMasked, String requestType, String newCardStatus, String user) {
		setAuditContext("REQUEST_APPROVE", user);
		AUDIT_LOG.info("REQUEST_APPROVED | RequestId={} | CardNumber={} | RequestType={} | NewCardStatus={} | Timestamp={}",
				requestId, cardNumberMasked, requestType, newCardStatus, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log card request rejection.
	 */
	public void logRequestRejected(Long requestId, String cardNumberMasked, String requestType, String reason, String user) {
		setAuditContext("REQUEST_REJECT", user);
		AUDIT_LOG.info("REQUEST_REJECTED | RequestId={} | CardNumber={} | RequestType={} | Reason={} | Timestamp={}",
				requestId, cardNumberMasked, requestType, reason, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log transaction (credit/debit).
	 */
	public void logTransaction(String cardNumberMasked, String transactionType, String amount, String transactionId, String user) {
		setAuditContext("TRANSACTION", user);
		AUDIT_LOG.info("TRANSACTION | CardNumber={} | Type={} | Amount={} | TransactionId={} | Timestamp={}",
				cardNumberMasked, transactionType, amount, transactionId, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log encryption operation (for security audit).
	 */
	public void logEncryption(String operation, String dataType, boolean success) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", operation);
		SECURITY_LOG.info("ENCRYPTION | Operation={} | DataType={} | Success={} | Timestamp={}",
				operation, dataType, success, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log decryption operation (for security audit).
	 */
	public void logDecryption(String operation, String dataType, boolean success) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", operation);
		SECURITY_LOG.info("DECRYPTION | Operation={} | DataType={} | Success={} | Timestamp={}",
				operation, dataType, success, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log failed decryption attempt (potential security breach).
	 */
	public void logDecryptionFailure(String dataType, String reason) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", "DECRYPTION_FAILED");
		SECURITY_LOG.error("DECRYPTION_FAILED | DataType={} | Reason={} | Timestamp={}",
				dataType, reason, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log API access.
	 */
	public void logApiAccess(String method, String endpoint, String ipAddress, int statusCode, long responseTime) {
		MDC.put("correlationId", generateCorrelationId());
		ACCESS_LOG.info("API_ACCESS | Method={} | Endpoint={} | IP={} | Status={} | ResponseTime={}ms | Timestamp={}",
				method, endpoint, ipAddress, statusCode, responseTime, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log validation error.
	 */
	public void logValidationError(String operation, String fieldName, String errorMessage, String user) {
		setAuditContext("VALIDATION_ERROR", user);
		AUDIT_LOG.warn("VALIDATION_ERROR | Operation={} | Field={} | Error={} | Timestamp={}",
				operation, fieldName, errorMessage, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log business rule violation.
	 */
	public void logBusinessRuleViolation(String rule, String details, String user) {
		setAuditContext("BUSINESS_RULE_VIOLATION", user);
		AUDIT_LOG.warn("BUSINESS_RULE_VIOLATION | Rule={} | Details={} | Timestamp={}",
				rule, details, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log suspicious activity (potential fraud).
	 */
	public void logSuspiciousActivity(String activityType, String details, String cardNumberMasked) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", "SUSPICIOUS_ACTIVITY");
		SECURITY_LOG.warn("SUSPICIOUS_ACTIVITY | Type={} | CardNumber={} | Details={} | Timestamp={}",
				activityType, cardNumberMasked, details, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log security event (admin access, password verification, etc.).
	 */
	public void logSecurityEvent(String eventType, String description, String details) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", eventType);
		SECURITY_LOG.info("SECURITY_EVENT | Type={} | Description={} | Details={} | Timestamp={}",
				eventType, description, details, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Log system error.
	 */
	public void logSystemError(String operation, String errorMessage, String stackTrace) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", operation);
		AUDIT_LOG.error("SYSTEM_ERROR | Operation={} | Error={} | StackTrace={} | Timestamp={}",
				operation, errorMessage, stackTrace, LocalDateTime.now());
		clearContext();
	}

	/**
	 * Set MDC context for audit logging.
	 */
	private void setAuditContext(String operation, String user) {
		MDC.put("correlationId", generateCorrelationId());
		MDC.put("operation", operation);
		MDC.put("user", user != null ? user : "SYSTEM");
	}

	/**
	 * Clear MDC context after logging.
	 */
	private void clearContext() {
		MDC.clear();
	}

	/**
	 * Generate unique correlation ID for request tracking.
	 */
	private String generateCorrelationId() {
		// Check if correlation ID already exists in MDC
		String existingId = MDC.get("correlationId");
		if (existingId != null && !existingId.isEmpty()) {
			return existingId;
		}
		return UUID.randomUUID().toString();
	}

	/**
	 * Set correlation ID for tracking requests across multiple operations.
	 */
	public void setCorrelationId(String correlationId) {
		if (correlationId != null && !correlationId.isEmpty()) {
			MDC.put("correlationId", correlationId);
		}
	}

	/**
	 * Get current correlation ID.
	 */
	public String getCorrelationId() {
		return MDC.get("correlationId");
	}
}
