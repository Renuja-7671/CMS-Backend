package com.epic.cms.repository;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epic.cms.model.CardRequest;

/**
 * Repository for CardRequest entity using native SQL queries.
 * Note: CardNumber column contains encrypted card numbers.
 */
@Repository
public interface CardRequestRepository extends CrudRepository<CardRequest, Long> {
	
	/**
	 * Find the most recent card request for a specific card.
	 * Parameter is already encrypted by service layer.
	 */
	@Query("""
		SELECT *
		FROM CardRequest
		WHERE CardNumber = :encryptedCardNumber
		ORDER BY RequestedAt DESC
		LIMIT 1
		""")
	Optional<CardRequest> findLatestByCardNumber(@Param("encryptedCardNumber") String encryptedCardNumber);
	
	/**
	 * Insert a new card request.
	 * CardNumber parameter is already encrypted by service layer.
	 */
	@Modifying
	@Query("""
		INSERT INTO CardRequest (CardNumber, RequestTypeCode, RequestStatusCode, Reason, RequestedAt)
		VALUES (:encryptedCardNumber, :requestTypeCode, 'PEND', :reason, NOW())
		""")
	void insertCardRequest(
		@Param("encryptedCardNumber") String encryptedCardNumber,
		@Param("requestTypeCode") String requestTypeCode,
		@Param("reason") String reason
	);
	
	/**
	 * Update request status to approved.
	 */
	@Modifying
	@Query("""
		UPDATE CardRequest
		SET RequestStatusCode = 'APPR', ProcessedAt = NOW()
		WHERE RequestID = :requestId AND RequestStatusCode = 'PEND'
		""")
	void approveRequest(@Param("requestId") Long requestId);
	
	/**
	 * Update request status to rejected.
	 */
	@Modifying
	@Query("""
		UPDATE CardRequest
		SET RequestStatusCode = 'RJCT', ProcessedAt = NOW()
		WHERE RequestID = :requestId
		""")
	void rejectRequest(@Param("requestId") Long requestId);
	
	/**
	 * Check if there's a pending request for a card.
	 * Parameter is already encrypted by service layer.
	 */
	@Query("""
		SELECT COUNT(*)
		FROM CardRequest
		WHERE CardNumber = :encryptedCardNumber AND RequestStatusCode = 'PEND'
		""")
	int countPendingRequestsForCard(@Param("encryptedCardNumber") String encryptedCardNumber);
	
	/**
	 * Update card status.
	 * Parameter is already encrypted by service layer.
	 */
	@Modifying
	@Query("""
		UPDATE Card
		SET CardStatus = :status
		WHERE CardNumber = :encryptedCardNumber
		""")
	void updateCardStatus(
		@Param("encryptedCardNumber") String encryptedCardNumber,
		@Param("status") String status
	);
	
	/**
	 * Get all pending requests with their card details.
	 * Used for the request confirmation page.
	 * This includes both ACTI requests (for IACT/DACT cards) and CDCL requests (for CACT cards).
	 */
	@Query("""
		SELECT cr.*
		FROM CardRequest cr
		JOIN Card c ON cr.CardNumber = c.CardNumber
		WHERE cr.RequestStatusCode = 'PEND'
		ORDER BY cr.RequestedAt DESC
		""")
	Iterable<CardRequest> findPendingRequestsForInactiveCards();
	
	/**
	 * Get pending requests with pagination.
	 */
	@Query("""
		SELECT cr.*
		FROM CardRequest cr
		JOIN Card c ON cr.CardNumber = c.CardNumber
		WHERE cr.RequestStatusCode = 'PEND'
		ORDER BY cr.RequestedAt DESC
		LIMIT :limit OFFSET :offset
		""")
	Iterable<CardRequest> findPendingRequestsForInactiveCardsWithPagination(
			@Param("limit") int limit,
			@Param("offset") int offset);
	
	/**
	 * Find all card requests with pagination and optional filtering.
	 */
	@Query("""
		SELECT cr.*
		FROM CardRequest cr
		WHERE (:status IS NULL OR cr.RequestStatusCode = :status)
		  AND (:searchQuery IS NULL OR cr.CardNumber LIKE CONCAT('%', :searchQuery, '%'))
		ORDER BY cr.RequestedAt DESC
		LIMIT :limit OFFSET :offset
		""")
	Iterable<CardRequest> findAllWithPagination(
			@Param("status") String status,
			@Param("searchQuery") String searchQuery,
			@Param("limit") int limit,
			@Param("offset") int offset);
	
	/**
	 * Count total number of card requests with optional filtering.
	 */
	@Query("""
		SELECT COUNT(*) 
		FROM CardRequest cr
		WHERE (:status IS NULL OR cr.RequestStatusCode = :status)
		  AND (:searchQuery IS NULL OR cr.CardNumber LIKE CONCAT('%', :searchQuery, '%'))
		""")
	long countAllRequests(
			@Param("status") String status,
			@Param("searchQuery") String searchQuery);
	
	/**
	 * Find pending card requests with pagination (for backwards compatibility).
	 */
	@Query("""
		SELECT cr.*
		FROM CardRequest cr
		WHERE cr.RequestStatusCode = 'PEND'
		ORDER BY cr.RequestedAt DESC
		LIMIT :limit OFFSET :offset
		""")
	Iterable<CardRequest> findPendingRequestsWithPagination(
			@Param("limit") int limit,
			@Param("offset") int offset);
	
	/**
	 * Count pending card requests.
	 */
	@Query("SELECT COUNT(*) FROM CardRequest WHERE RequestStatusCode = 'PEND'")
	long countPendingRequests();
}

