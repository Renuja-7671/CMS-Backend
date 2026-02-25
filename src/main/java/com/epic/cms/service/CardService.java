package com.epic.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epic.cms.dto.CardDTO;
import com.epic.cms.dto.CreateCardRequest;
import com.epic.cms.dto.PageRequest;
import com.epic.cms.dto.PageResponse;
import com.epic.cms.dto.UpdateCardRequest;
import com.epic.cms.dto.ViewCardNumberRequest;
import com.epic.cms.dto.ViewCardNumberResponse;
import com.epic.cms.exception.DuplicateResourceException;
import com.epic.cms.exception.InvalidOperationException;
import com.epic.cms.exception.ResourceNotFoundException;
import com.epic.cms.model.Card;
import com.epic.cms.model.CardStatus;
import com.epic.cms.model.User;
import com.epic.cms.repository.CardRepository;
import com.epic.cms.repository.CardStatusRepository;
import com.epic.cms.repository.UserRepository;
import com.epic.cms.util.AuditLogger;
import com.epic.cms.util.CardEncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

	private final CardRepository cardRepository;
	private final CardStatusRepository cardStatusRepository;
	private final UserRepository userRepository;
	private final CardEncryptionUtil cardEncryptionUtil;
	private final AuditLogger auditLogger;

	@Value("${card.admin-password}")
	private String adminPassword;

	/**
	 * Get all cards.
	 */
	@Transactional(readOnly = true)
	public List<CardDTO> getAllCards() {
		log.info("Fetching all cards");
		List<Card> cards = cardRepository.findAllCards();
		return cards.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}
	
	/**
	 * Get all cards with pagination and optional filtering.
	 * 
	 * @param pageRequest Pagination parameters
	 * @param status Optional card status filter (null = all statuses)
	 * @param searchQuery Optional card number search (null = no search)
	 */
	@Transactional(readOnly = true)
	public PageResponse<CardDTO> getAllCardsWithPagination(PageRequest pageRequest, String status, String searchQuery) {
		log.info("Fetching cards with pagination - page: {}, size: {}, status: {}, search: {}", 
				pageRequest.getPage(), pageRequest.getSize(), status, searchQuery);
		
		// Normalize empty strings to null for SQL query
		String normalizedStatus = (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) ? null : status;
		String normalizedSearch = (searchQuery == null || searchQuery.trim().isEmpty()) ? null : searchQuery;
		
		// Get total count with filters
		long totalElements = cardRepository.countAllCards(normalizedStatus, normalizedSearch);
		
		// Get paginated cards with filters
		List<Card> cards = cardRepository.findAllCardsWithPagination(
				normalizedStatus,
				normalizedSearch,
				pageRequest.getSize(), 
				pageRequest.getOffset());
		
		// Convert to DTOs
		List<CardDTO> cardDTOs = cards.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
		
		// Return paginated response
		return PageResponse.of(cardDTOs, pageRequest.getPage(), pageRequest.getSize(), totalElements);
	}

	/**
	 * Get card by plain card number.
	 * Encrypts the plain card number and searches in database.
	 */
	@Transactional(readOnly = true)
	public CardDTO getCardByNumber(String plainCardNumber) {
		log.info("Fetching card with number ending in: {}", plainCardNumber.substring(plainCardNumber.length() - 4));
		
		// Encrypt the plain card number to search in database
		String encryptedCardNumber = cardEncryptionUtil.encryptCardNumberForDatabase(plainCardNumber);
		
		Card card = cardRepository.findByCardNumber(encryptedCardNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", 
						"****" + plainCardNumber.substring(plainCardNumber.length() - 4)));
		
		return convertToDTO(card);
	}
	
	/**
	 * Get card by display card number and encryption key (for decryption).
	 */
	@Transactional(readOnly = true)
	public CardDTO getCardByDisplayNumber(String displayCardNumber, String encryptionKey) {
		log.info("Fetching card by display number");
		
		// Decrypt the display card number to get the plain card number
		String plainCardNumber = cardEncryptionUtil.decryptMiddleDigits(displayCardNumber, encryptionKey);
		
		return getCardByNumber(plainCardNumber);
	}

	/**
	 * Create a new card.
	 */
	@Transactional
	public CardDTO createCard(CreateCardRequest request) {
		log.info("Creating new card with number ending in: {}", 
				request.getCardNumber().substring(request.getCardNumber().length() - 4));

		// Validate lastUpdatedUser exists and is active
		validateUserExistsAndActive(request.getLastUpdatedUser());

		// Encrypt the card number for database storage
		String encryptedCardNumber = cardEncryptionUtil.encryptCardNumberForDatabase(request.getCardNumber());

		// Validate card doesn't already exist
		if (cardRepository.existsByCardNumber(encryptedCardNumber)) {
			throw new DuplicateResourceException("Card", "cardNumber", 
					"****" + request.getCardNumber().substring(request.getCardNumber().length() - 4));
		}

		// Validate expiry date is in the future
		if (request.getExpiryDate().isBefore(LocalDate.now())) {
			throw new InvalidOperationException("Expiry date must be in the future");
		}

		// Validate cash limit doesn't exceed credit limit
		if (request.getCashLimit().compareTo(request.getCreditLimit()) > 0) {
			throw new InvalidOperationException("Cash limit cannot exceed credit limit");
		}

		// Insert card using native SQL (store encrypted card number in database)
		cardRepository.insertCard(
				encryptedCardNumber,
				request.getExpiryDate().toString(),
				request.getCreditLimit().toString(),
				request.getCashLimit().toString(),
				request.getLastUpdatedUser());

		log.info("Card created successfully");
		
		// Audit log: Card creation
		String maskedCardNumber = "****" + request.getCardNumber().substring(request.getCardNumber().length() - 4);
		auditLogger.logCardCreated(
				maskedCardNumber,
				"IACT",
				request.getCreditLimit().toString(),
				request.getCashLimit().toString(),
				request.getLastUpdatedUser());

		// Fetch and return the created card
		return getCardByNumber(request.getCardNumber());
	}

	/**
	 * Update card details using display card number and encryption key.
	 */
	@Transactional
	public CardDTO updateCard(String displayCardNumber, String encryptionKey, UpdateCardRequest request) {
		log.info("Updating card");

		// Validate lastUpdatedUser exists and is active
		validateUserExistsAndActive(request.getLastUpdatedUser());

		// Decrypt the display card number to get the plain card number
		String plainCardNumber = cardEncryptionUtil.decryptMiddleDigits(displayCardNumber, encryptionKey);
		
		// Encrypt the plain card number to search in database
		String encryptedCardNumber = cardEncryptionUtil.encryptCardNumberForDatabase(plainCardNumber);
		
		// Fetch existing card to track changes
		Card existingCard = cardRepository.findByCardNumber(encryptedCardNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", 
						"****" + plainCardNumber.substring(plainCardNumber.length() - 4)));

		// Validate expiry date
		if (request.getExpiryDate().isBefore(LocalDate.now())) {
			throw new InvalidOperationException("Expiry date must be in the future");
		}

		// Validate limits
		if (request.getCashLimit().compareTo(request.getCreditLimit()) > 0) {
			throw new InvalidOperationException("Cash limit cannot exceed credit limit");
		}

		if (request.getAvailableCreditLimit().compareTo(request.getCreditLimit()) > 0) {
			throw new InvalidOperationException("Available credit limit cannot exceed credit limit");
		}

		if (request.getAvailableCashLimit().compareTo(request.getCashLimit()) > 0) {
			throw new InvalidOperationException("Available cash limit cannot exceed cash limit");
		}

		// Update card using native SQL
		cardRepository.updateCard(
				encryptedCardNumber,
				request.getExpiryDate().toString(),
				request.getCreditLimit().toString(),
				request.getCashLimit().toString(),
				request.getAvailableCreditLimit().toString(),
				request.getAvailableCashLimit().toString(),
				request.getLastUpdatedUser());

		log.info("Card updated successfully");
		
		// Audit log: Track limit changes
		String maskedCardNumber = "****" + plainCardNumber.substring(plainCardNumber.length() - 4);
		if (!existingCard.getCreditLimit().equals(request.getCreditLimit())) {
			auditLogger.logCardLimitUpdate(
					maskedCardNumber,
					"CREDIT_LIMIT",
					existingCard.getCreditLimit().toString(),
					request.getCreditLimit().toString(),
					request.getLastUpdatedUser());
		}
		if (!existingCard.getCashLimit().equals(request.getCashLimit())) {
			auditLogger.logCardLimitUpdate(
					maskedCardNumber,
					"CASH_LIMIT",
					existingCard.getCashLimit().toString(),
					request.getCashLimit().toString(),
					request.getLastUpdatedUser());
		}

		// Fetch and return the updated card
		return getCardByNumber(plainCardNumber);
	}

	/**
	 * Get cards by status.
	 */
	@Transactional(readOnly = true)
	public List<CardDTO> getCardsByStatus(String status) {
		log.info("Fetching cards with status: {}", status);
		List<Card> cards = cardRepository.findByCardStatus(status);
		return cards.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Validate that the user exists and is active.
	 * Throws exception if user not found or not active.
	 */
	private void validateUserExistsAndActive(String userName) {
		User user = userRepository.findByUserName(userName)
				.orElseThrow(() -> new ResourceNotFoundException("User", "userName", userName));
		
		if (!"ACT".equals(user.getStatus())) {
			throw new InvalidOperationException("User is not active: " + userName);
		}
	}

	/**
	 * Convert Card entity to CardDTO with encrypted middle digits.
	 * The card.cardNumber from database is encrypted, so we decrypt it first.
	 */
	private CardDTO convertToDTO(Card card) {
		// Decrypt the card number from database
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(card.getCardNumber());
		
		// Fetch status description
		String statusDescription = cardStatusRepository.findByStatusCode(card.getCardStatus())
				.map(CardStatus::getDescription)
				.orElse("Unknown Status");

		// Calculate used limits
		BigDecimal usedCreditLimit = card.getCreditLimit().subtract(card.getAvailableCreditLimit());
		BigDecimal usedCashLimit = card.getCashLimit().subtract(card.getAvailableCashLimit());

		// Encrypt middle digits for API response and get encryption key
		Map<String, String> encryptionResult = cardEncryptionUtil.encryptMiddleDigits(plainCardNumber);
		String displayCardNumber = encryptionResult.get("displayCardNumber");
		String encryptionKey = encryptionResult.get("encryptionKey");

		return CardDTO.builder()
				.cardNumber(card.getCardNumber()) // Include encrypted card number from DB (used as ID)
				.displayCardNumber(displayCardNumber) // First 6 + encrypted middle + last 4
				.encryptionKey(encryptionKey) // Send key for later decryption
				.expiryDate(card.getExpiryDate())
				.cardStatus(card.getCardStatus())
				.cardStatusDescription(statusDescription)
				.creditLimit(card.getCreditLimit())
				.cashLimit(card.getCashLimit())
				.availableCreditLimit(card.getAvailableCreditLimit())
			.availableCashLimit(card.getAvailableCashLimit())
			.usedCreditLimit(usedCreditLimit)
			.usedCashLimit(usedCashLimit)
			.lastUpdatedUser(card.getLastUpdatedUser())
			.lastUpdateTime(card.getLastUpdateTime())
			.build();
	}

	/**
	 * View plain card number with admin password verification.
	 * 
	 * @param request ViewCardNumberRequest containing cardId and admin password
	 * @return ViewCardNumberResponse with plain and masked card numbers
	 * @throws InvalidOperationException if admin password is incorrect
	 * @throws ResourceNotFoundException if card not found
	 */
	@Transactional(readOnly = true)
	public ViewCardNumberResponse viewPlainCardNumber(ViewCardNumberRequest request) {
		log.info("Request to view plain card number for card ID: {}", request.getCardId());
		log.debug("CardID type: {}, Value: '{}'", request.getCardId().getClass().getName(), request.getCardId());

		// Verify admin password
		if (!adminPassword.equals(request.getAdminPassword())) {
			log.warn("Incorrect admin password attempt for card ID: {}", request.getCardId());
			auditLogger.logSecurityEvent(
				"CARD_VIEW_UNAUTHORIZED",
				"Incorrect admin password attempt for viewing card number",
				"CardID=" + request.getCardId()
			);
			throw new InvalidOperationException("Invalid admin password");
		}

		// Fetch card from database
		log.debug("Attempting to find card with ID: '{}'", request.getCardId());
		Card card = cardRepository.findById(request.getCardId())
			.orElseThrow(() -> {
				log.error("Card not found with ID: {}", request.getCardId());
				return new ResourceNotFoundException("Card", "ID", request.getCardId());
			});
		log.debug("Card found successfully: {}", card);

		// Decrypt the plain card number from database
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(card.getCardNumber());

		// Generate masked card number for reference
		String maskedCardNumber = maskCardNumber(plainCardNumber);

		// Log the successful view attempt
		log.info("Admin successfully viewed plain card number for card ID: {}", request.getCardId());
		auditLogger.logSecurityEvent(
			"CARD_VIEW_SUCCESS",
			"Admin viewed plain card number",
			"CardID=" + request.getCardId() + " | MaskedNumber=" + maskedCardNumber
		);

		return ViewCardNumberResponse.builder()
			.cardId(card.getCardNumber())
			.plainCardNumber(plainCardNumber)
			.maskedCardNumber(maskedCardNumber)
			.build();
	}

	/**
	 * Mask card number showing first 6 and last 4 digits.
	 */
	private String maskCardNumber(String plainCardNumber) {
		if (plainCardNumber == null || plainCardNumber.length() < 10) {
			return "****";
		}
		String first6 = plainCardNumber.substring(0, 6);
		String last4 = plainCardNumber.substring(plainCardNumber.length() - 4);
		return first6 + "****" + last4;
	}
}
