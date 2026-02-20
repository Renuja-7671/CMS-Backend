package com.epic.cms.config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.epic.cms.util.AuditLogger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Intercepts all HTTP requests to:
 * 1. Add correlation ID for request tracking
 * 2. Log API access
 * 3. Measure response time
 */
@Component
@RequiredArgsConstructor
public class RequestLoggingInterceptor implements HandlerInterceptor {

	private final AuditLogger auditLogger;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// Generate or retrieve correlation ID
		String correlationId = request.getHeader("X-Correlation-ID");
		if (correlationId == null || correlationId.isEmpty()) {
			correlationId = UUID.randomUUID().toString();
		}
		
		// Set correlation ID in MDC for all subsequent logs
		MDC.put("correlationId", correlationId);
		auditLogger.setCorrelationId(correlationId);
		
		// Add correlation ID to response header
		response.setHeader("X-Correlation-ID", correlationId);
		
		// Store request start time
		request.setAttribute("startTime", System.currentTimeMillis());
		
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		// Calculate response time
		Long startTime = (Long) request.getAttribute("startTime");
		long responseTime = startTime != null ? System.currentTimeMillis() - startTime : 0;
		
		// Get client IP address
		String ipAddress = getClientIpAddress(request);
		
		// Log API access
		auditLogger.logApiAccess(
			request.getMethod(),
			request.getRequestURI(),
			ipAddress,
			response.getStatus(),
			responseTime
		);
		
		// Clear MDC
		MDC.clear();
	}

	/**
	 * Get the real client IP address (handles proxies and load balancers).
	 */
	private String getClientIpAddress(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		
		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty()) {
			return xRealIp;
		}
		
		return request.getRemoteAddr();
	}
}
