package com.epic.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epic.cms.model.Card;

@Repository
public interface CardRepository extends CrudRepository<Card, String> {

	/**
	 * Find all cards.
	 * Note: CardNumber column contains encrypted card numbers.
	 */
	@Query("""
			SELECT 
				c.CardNumber,
				c.ExpiryDate,
				c.CardStatus,
				c.CreditLimit,
				c.CashLimit,
				c.AvailableCreditLimit,
				c.AvailableCashLimit,
				c.LastUpdateTime
			FROM Card c
			ORDER BY c.LastUpdateTime DESC
			""")
	List<Card> findAllCards();

	/**
	 * Find a card by encrypted card number.
	 * Client sends plain card number, service encrypts it, then searches by encrypted value.
	 */
	@Query("""
			SELECT 
				c.CardNumber,
				c.ExpiryDate,
				c.CardStatus,
				c.CreditLimit,
				c.CashLimit,
				c.AvailableCreditLimit,
				c.AvailableCashLimit,
				c.LastUpdateTime
			FROM Card c
			WHERE c.CardNumber = :encryptedCardNumber
			""")
	Optional<Card> findByCardNumber(@Param("encryptedCardNumber") String encryptedCardNumber);

	/**
	 * Check if a card exists by encrypted card number.
	 */
	@Query("""
			SELECT COUNT(*) > 0
			FROM Card
			WHERE CardNumber = :encryptedCardNumber
			""")
	boolean existsByCardNumber(@Param("encryptedCardNumber") String encryptedCardNumber);

	/**
	 * Insert a new card using native SQL.
	 * CardNumber parameter is already encrypted by the service layer.
	 */
	@Modifying
	@Query("""
			INSERT INTO Card (
				CardNumber,
				ExpiryDate, 
				CardStatus, 
				CreditLimit, 
				CashLimit, 
				AvailableCreditLimit, 
				AvailableCashLimit
			) VALUES (
				:encryptedCardNumber,
				:expiryDate,
				'IACT',
				:creditLimit,
				:cashLimit,
				:creditLimit,
				:cashLimit
			)
			""")
	void insertCard(
			@Param("encryptedCardNumber") String encryptedCardNumber,
			@Param("expiryDate") String expiryDate,
			@Param("creditLimit") String creditLimit,
			@Param("cashLimit") String cashLimit);

	/**
	 * Update card details using encrypted card number for WHERE clause.
	 */
	@Modifying
	@Query("""
			UPDATE Card
			SET 
				ExpiryDate = :expiryDate,
				CreditLimit = :creditLimit,
				CashLimit = :cashLimit,
				AvailableCreditLimit = :availableCreditLimit,
				AvailableCashLimit = :availableCashLimit
			WHERE CardNumber = :encryptedCardNumber
			""")
	void updateCard(
			@Param("encryptedCardNumber") String encryptedCardNumber,
			@Param("expiryDate") String expiryDate,
			@Param("creditLimit") String creditLimit,
			@Param("cashLimit") String cashLimit,
			@Param("availableCreditLimit") String availableCreditLimit,
			@Param("availableCashLimit") String availableCashLimit);

	/**
	 * Update card status using encrypted card number for WHERE clause.
	 */
	@Modifying
	@Query("""
			UPDATE Card
			SET CardStatus = :status
			WHERE CardNumber = :encryptedCardNumber
			""")
	void updateCardStatus(
			@Param("encryptedCardNumber") String encryptedCardNumber,
			@Param("status") String status);

	/**
	 * Find cards by status.
	 */
	@Query("""
			SELECT 
				c.CardNumber,
				c.ExpiryDate,
				c.CardStatus,
				c.CreditLimit,
				c.CashLimit,
				c.AvailableCreditLimit,
				c.AvailableCashLimit,
				c.LastUpdateTime
			FROM Card c
			WHERE c.CardStatus = :status
			ORDER BY c.LastUpdateTime DESC
			""")
	List<Card> findByCardStatus(@Param("status") String status);
	
	/**
	 * Find all cards with pagination and optional filtering.
	 */
	@Query("""
			SELECT 
				c.CardNumber,
				c.ExpiryDate,
				c.CardStatus,
				c.CreditLimit,
				c.CashLimit,
				c.AvailableCreditLimit,
				c.AvailableCashLimit,
				c.LastUpdateTime
			FROM Card c
			WHERE (:status IS NULL OR c.CardStatus = :status)
			  AND (:searchQuery IS NULL OR c.CardNumber LIKE CONCAT('%', :searchQuery, '%'))
			ORDER BY c.LastUpdateTime DESC
			LIMIT :limit OFFSET :offset
			""")
	List<Card> findAllCardsWithPagination(
			@Param("status") String status,
			@Param("searchQuery") String searchQuery,
			@Param("limit") int limit,
			@Param("offset") int offset);
	
	/**
	 * Count total number of cards with optional filtering.
	 */
	@Query("""
			SELECT COUNT(*) 
			FROM Card c
			WHERE (:status IS NULL OR c.CardStatus = :status)
			  AND (:searchQuery IS NULL OR c.CardNumber LIKE CONCAT('%', :searchQuery, '%'))
			""")
	long countAllCards(
			@Param("status") String status,
			@Param("searchQuery") String searchQuery);
}

