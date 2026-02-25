package com.epic.cms.util;

/**
 * Thread-local storage for encryption session information.
 * Used to pass session data from request filter to response advice.
 */
public class EncryptionContext {

	private static final ThreadLocal<SessionContext> context = new ThreadLocal<>();

	/**
	 * Session context data
	 */
	public static class SessionContext {
		private final String sessionId;
		private final String encryptedAesKey;

		public SessionContext(String sessionId, String encryptedAesKey) {
			this.sessionId = sessionId;
			this.encryptedAesKey = encryptedAesKey;
		}

		public String getSessionId() {
			return sessionId;
		}

		public String getEncryptedAesKey() {
			return encryptedAesKey;
		}
	}

	/**
	 * Store session context for current request thread
	 */
	public static void setSessionContext(String sessionId, String encryptedAesKey) {
		context.set(new SessionContext(sessionId, encryptedAesKey));
	}

	/**
	 * Get session context for current request thread
	 */
	public static SessionContext getSessionContext() {
		return context.get();
	}

	/**
	 * Clear session context (should be called after response is sent)
	 */
	public static void clear() {
		context.remove();
	}

	/**
	 * Check if session context exists for current thread
	 */
	public static boolean hasSessionContext() {
		return context.get() != null;
	}
}
