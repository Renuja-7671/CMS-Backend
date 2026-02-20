package com.epic.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epic.cms.dto.CardRequestDTO;
import com.epic.cms.dto.CardRequestDetailDTO;
import com.epic.cms.dto.CreateCardRequestDTO;
import com.epic.cms.exception.InvalidOperationException;
import com.epic.cms.exception.ResourceNotFoundException;
import com.epic.cms.model.Card;
import com.epic.cms.model.CardRequest;
import com.epic.cms.repository.CardRepository;
import com.epic.cms.repository.CardRequestRepository;
import com.epic.cms.repository.CardRequestTypeRepository;
import com.epic.cms.repository.CardStatusRepository;
import com.epic.cms.repository.RequestStatusRepository;
import com.epic.cms.util.AuditLogger;
import com.epic.cms.util.CardEncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for card request operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardRequestService {
	
	private final CardRequestRepository cardRequestRepository;
	private final CardRepository cardRepository;
	private final RequestStatusRepository requestStatusRepository;
	private final CardRequestTypeRepository cardRequestTypeRepository;
	private final CardStatusRepository cardStatusRepository;
	private final CardEncryptionUtil cardEncryptionUtil;
	private final AuditLogger auditLogger;
	
	/**
	 * Create a new card request.
	 */
	@Transactional
	public CardRequestDTO createCardRequest(CreateCardRequestDTO request) {
		// Decrypt the display card number to get the plain card number
		String plainCardNumber = cardEncryptionUtil.decryptMiddleDigits(
			request.getDisplayCardNumber(), 
			request.getEncryptionKey()
		);
		
		log.info("Creating card request for card ending in: {}, type: {}", 
			plainCardNumber.substring(plainCardNumber.length() - 4), 
			request.getRequestType());
		
		// Encrypt the plain card number for database operations
		String encryptedCardNumber = cardEncryptionUtil.encryptCardNumberForDatabase(plainCardNumber);
		
		// 1. Validate card exists
		Card card = cardRepository.findByCardNumber(encryptedCardNumber)
			.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
		
		// 2. Check for pending requests
		if (cardRequestRepository.countPendingRequestsForCard(encryptedCardNumber) > 0) {
			throw new InvalidOperationException("There is already a pending request for this card");
		}
		
	// 3. Validate business rules
	String requestType = request.getRequestType();
	String cardStatus = card.getCardStatus();
	
	if ("ACTI".equals(requestType)) {
		if (!"IACT".equals(cardStatus) && !"DACT".equals(cardStatus)) {
			throw new InvalidOperationException(
				"Activation request can only be created for Inactive (IACT) or Deactivated (DACT) cards. " +
				"Current card status: " + cardStatus
			);
		}
	} else if ("CDCL".equals(requestType)) {
		if (!"CACT".equals(cardStatus)) {
			throw new InvalidOperationException(
				"Card close request can only be created for Active (CACT) cards. Current card status: " + cardStatus
			);
		}
		
		BigDecimal creditLimit = card.getCreditLimit();
		BigDecimal availableCreditLimit = card.getAvailableCreditLimit();
		
		// Check if credit balance equals available credit balance (no outstanding debt)
		if (availableCreditLimit.compareTo(creditLimit) == 0) {
			// Equal: No debt - create request as PEND and immediately set card to IACT
			log.info("CDCL request for card {} has no outstanding debt. Setting card to IACT and request to PEND", 
				plainCardNumber.substring(plainCardNumber.length() - 4));
			
			// 4a. Insert request with PEND status (use encrypted card number)
			cardRequestRepository.insertCardRequest(
				encryptedCardNumber,
				request.getRequestType(),
				request.getReason()
			);
			
			// 4b. Update card status to IACT immediately (use encrypted card number)
			cardRequestRepository.updateCardStatus(encryptedCardNumber, "IACT");
			
			// 5a. Retrieve and return (use encrypted card number)
			CardRequest createdRequest = cardRequestRepository.findLatestByCardNumber(encryptedCardNumber)
				.orElseThrow(() -> new InvalidOperationException("Failed to retrieve created card request"));
			
			return toDTO(createdRequest);
			
		} else {
			// Not equal: Has outstanding debt - create request but immediately set to REJD
			BigDecimal debt = creditLimit.subtract(availableCreditLimit);
			log.warn("CDCL request for card {} rejected due to outstanding debt of {}", 
				plainCardNumber.substring(plainCardNumber.length() - 4), debt);
			
			// 4c. Insert request (use encrypted card number)
			cardRequestRepository.insertCardRequest(
				encryptedCardNumber,
				request.getRequestType(),
				request.getReason()
			);
			
			// 4d. Immediately reject the request
			CardRequest createdRequest = cardRequestRepository.findLatestByCardNumber(encryptedCardNumber)
				.orElseThrow(() -> new InvalidOperationException("Failed to retrieve created card request"));
			
			cardRequestRepository.rejectRequest(createdRequest.getRequestId());
			
			// 5b. Retrieve and return the rejected request
			return getCardRequestById(createdRequest.getRequestId());
		}
	}
	
	// 4. Insert request (for ACTI requests) - use encrypted card number
	cardRequestRepository.insertCardRequest(
		encryptedCardNumber,
		request.getRequestType(),
		request.getReason()
	);
	
	// 5. Retrieve and return (use encrypted card number)
	CardRequest createdRequest = cardRequestRepository.findLatestByCardNumber(encryptedCardNumber)
		.orElseThrow(() -> new InvalidOperationException("Failed to retrieve created card request"));
	
	// Audit log: Request creation
	String maskedCardNumber = "****" + plainCardNumber.substring(plainCardNumber.length() - 4);
	auditLogger.logRequestCreated(
			createdRequest.getRequestId(),
			maskedCardNumber,
			request.getRequestType(),
			"SYSTEM");
	
	return toDTO(createdRequest);
	}
	
	/**
	 * Get all card requests.
	 */
	public List<CardRequestDTO> getAllCardRequests() {
		List<CardRequestDTO> result = new ArrayList<>();
		cardRequestRepository.findAll().forEach(cr -> result.add(toDTO(cr)));
		return result;
	}
	
	/**
	 * Get pending card requests.
	 */
	public List<CardRequestDTO> getPendingCardRequests() {
		List<CardRequestDTO> result = new ArrayList<>();
		cardRequestRepository.findAll().forEach(cr -> {
			if ("PEND".equals(cr.getRequestStatusCode())) {
				result.add(toDTO(cr));
			}
		});
		return result;
	}
	
	/**
	 * Get card request by ID.
	 */
	public CardRequestDTO getCardRequestById(Long requestId) {
		CardRequest request = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		return toDTO(request);
	}
	
	/**
	 * Approve a card request.
	 */
	@Transactional
	public CardRequestDTO approveCardRequest(Long requestId) {
		CardRequest request = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		if (!"PEND".equals(request.getRequestStatusCode())) {
			throw new InvalidOperationException("Only pending requests can be approved");
		}
		
		// Update request status
		cardRequestRepository.approveRequest(requestId);
		
		// Update card status
		// Note: request.getCardNumber() contains encrypted card number from database
		String newCardStatus = "ACTI".equals(request.getRequestTypeCode()) ? "CACT" : "DACT";
		cardRequestRepository.updateCardStatus(request.getCardNumber(), newCardStatus);
		
		return getCardRequestById(requestId);
	}
	
	/**
	 * Reject a card request.
	 */
	@Transactional
	public CardRequestDTO rejectCardRequest(Long requestId) {
		CardRequest request = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		if (!"PEND".equals(request.getRequestStatusCode())) {
			throw new InvalidOperationException("Only pending requests can be rejected");
		}
		
		cardRequestRepository.rejectRequest(requestId);
		return getCardRequestById(requestId);
	}
	
	/**
	 * Get pending requests for inactive/deactivated cards with full card details.
	 * Used for request confirmation page.
	 */
	public List<CardRequestDetailDTO> getPendingRequestsWithCardDetails() {
		List<CardRequestDetailDTO> result = new ArrayList<>();
		
		cardRequestRepository.findPendingRequestsForInactiveCards().forEach(request -> {
			result.add(toDetailDTO(request));
		});
		
		return result;
	}
	
	/**
	 * Approve a card request with proper status updates.
	 * For ACTI requests: Card status IACT/DACT -> CACT
	 * For CDCL requests: Card status CACT -> DACT
	 */
	@Transactional
	public CardRequestDetailDTO approveRequestWithDetails(Long requestId) {
		CardRequest request = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		if (!"PEND".equals(request.getRequestStatusCode())) {
			throw new InvalidOperationException("Only pending requests can be approved");
		}
		
		// Get old card status before update
		Card card = cardRepository.findByCardNumber(request.getCardNumber())
			.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
		String oldCardStatus = card.getCardStatus();
		
		// Determine new card status based on request type
		String newCardStatus;
		if ("ACTI".equals(request.getRequestTypeCode())) {
			// Activation: IACT/DACT -> CACT
			newCardStatus = "CACT";
		} else if ("CDCL".equals(request.getRequestTypeCode())) {
			// Deactivation/Card Close: CACT -> DACT
			newCardStatus = "DACT";
		} else {
			// Any other request type
			throw new InvalidOperationException("Unsupported request type for approval: " + request.getRequestTypeCode());
		}
		
		// Update request status to APPR
		cardRequestRepository.approveRequest(requestId);
		
		// Update card status
		cardRequestRepository.updateCardStatus(request.getCardNumber(), newCardStatus);
		
		// Audit log: Request approval and card status change
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(request.getCardNumber());
		String maskedCardNumber = "****" + plainCardNumber.substring(plainCardNumber.length() - 4);
		auditLogger.logRequestApproved(
				requestId,
				maskedCardNumber,
				request.getRequestTypeCode(),
				newCardStatus,
				"SYSTEM");
		auditLogger.logCardStatusChange(
				maskedCardNumber,
				oldCardStatus,
				newCardStatus,
				"Request approved: " + requestId,
				"SYSTEM");
		
		// Retrieve updated request and return with card details
		CardRequest updatedRequest = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		return toDetailDTO(updatedRequest);
	}
	
	/**
	 * Reject a card request with card status update.
	 * For ACTI requests: Card status changes to DACT, request status to RJCT
	 * For CDCL requests: Card status changes to CACT (kept active), request status to RJCT
	 */
	@Transactional
	public CardRequestDetailDTO rejectRequestWithDetails(Long requestId) {
		CardRequest request = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		if (!"PEND".equals(request.getRequestStatusCode())) {
			throw new InvalidOperationException("Only pending requests can be rejected");
		}
		
		// Get old card status before update
		Card card = cardRepository.findByCardNumber(request.getCardNumber())
			.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
		String oldCardStatus = card.getCardStatus();
		
		// Determine new card status based on request type
		String newCardStatus;
		if ("ACTI".equals(request.getRequestTypeCode())) {
			// Rejecting activation request: 
			// - Card should remain in Inactive state (IACT), not Deactivated state (DACT)
			// - Even if the card was DACT before, rejection means it goes back to IACT
			newCardStatus = "IACT";
		} else if ("CDCL".equals(request.getRequestTypeCode())) {
			// Rejecting deactivation request: keep card active (CACT)
			newCardStatus = "CACT";
		} else {
			// Default: keep current status for unknown request types
			newCardStatus = oldCardStatus;
		}
		
		// Update request status to RJCT
		cardRequestRepository.rejectRequest(requestId);
		
		// Update card status
		cardRequestRepository.updateCardStatus(request.getCardNumber(), newCardStatus);
		
		// Audit log: Request rejection
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(request.getCardNumber());
		String maskedCardNumber = "****" + plainCardNumber.substring(plainCardNumber.length() - 4);
		auditLogger.logRequestRejected(
				requestId,
				maskedCardNumber,
				request.getRequestTypeCode(),
				"Request rejected by administrator",
				"SYSTEM");
		if (!oldCardStatus.equals(newCardStatus)) {
			auditLogger.logCardStatusChange(
					maskedCardNumber,
					oldCardStatus,
					newCardStatus,
					"Request rejected: " + requestId,
					"SYSTEM");
		}
		
		// Retrieve updated request and return with card details
		CardRequest updatedRequest = cardRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("Card request not found with ID: " + requestId));
		
		return toDetailDTO(updatedRequest);
	}
	
	/**
	 * Convert entity to DTO.
	 * The entity.cardNumber from database is encrypted, so we decrypt it first.
	 */
	private CardRequestDTO toDTO(CardRequest entity) {
		String statusDesc = requestStatusRepository.findDescriptionByCode(entity.getRequestStatusCode()).orElse("");
		String typeDesc = cardRequestTypeRepository.findDescriptionByCode(entity.getRequestTypeCode()).orElse("");
		
		// Decrypt the encrypted card number from database
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(entity.getCardNumber());
		
		// Encrypt middle digits for API response
		Map<String, String> encryptionResult = cardEncryptionUtil.encryptMiddleDigits(plainCardNumber);
		
		return CardRequestDTO.builder()
			.requestId(entity.getRequestId())
			.displayCardNumber(encryptionResult.get("displayCardNumber"))
			.encryptionKey(encryptionResult.get("encryptionKey"))
			.requestType(entity.getRequestTypeCode())
			.requestTypeDescription(typeDesc)
			.requestStatus(entity.getRequestStatusCode())
			.requestStatusDescription(statusDesc)
			.reason(entity.getReason())
			.requestedAt(entity.getRequestedAt())
			.processedAt(entity.getProcessedAt())
			.build();
	}
	
	/**
	 * Convert entity to detailed DTO with card information.
	 * The entity.cardNumber from database is encrypted, so we decrypt it first.
	 */
	private CardRequestDetailDTO toDetailDTO(CardRequest entity) {
		String statusDesc = requestStatusRepository.findDescriptionByCode(entity.getRequestStatusCode()).orElse("");
		String typeDesc = cardRequestTypeRepository.findDescriptionByCode(entity.getRequestTypeCode()).orElse("");
		
		// Get card details using encrypted card number
		Card card = cardRepository.findByCardNumber(entity.getCardNumber())
			.orElseThrow(() -> new ResourceNotFoundException("Card not found"));
		
		String cardStatusDesc = cardStatusRepository.findByStatusCode(card.getCardStatus())
			.map(cs -> cs.getDescription())
			.orElse("");
		
		// Calculate used limits
		BigDecimal usedCreditLimit = card.getCreditLimit().subtract(card.getAvailableCreditLimit());
		BigDecimal usedCashLimit = card.getCashLimit().subtract(card.getAvailableCashLimit());
		
		// Decrypt the encrypted card number from database
		String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(entity.getCardNumber());
		
		// Encrypt middle digits for API response
		Map<String, String> encryptionResult = cardEncryptionUtil.encryptMiddleDigits(plainCardNumber);
		
		return CardRequestDetailDTO.builder()
			// Request information
			.requestId(entity.getRequestId())
			.requestType(entity.getRequestTypeCode())
			.requestTypeDescription(typeDesc)
			.requestStatus(entity.getRequestStatusCode())
			.requestStatusDescription(statusDesc)
			.reason(entity.getReason())
			.requestedAt(entity.getRequestedAt())
			.processedAt(entity.getProcessedAt())
			// Card information
			.displayCardNumber(encryptionResult.get("displayCardNumber"))
			.encryptionKey(encryptionResult.get("encryptionKey"))
			.expiryDate(card.getExpiryDate())
			.cardStatus(card.getCardStatus())
			.cardStatusDescription(cardStatusDesc)
			.creditLimit(card.getCreditLimit())
			.cashLimit(card.getCashLimit())
			.availableCreditLimit(card.getAvailableCreditLimit())
			.availableCashLimit(card.getAvailableCashLimit())
			.usedCreditLimit(usedCreditLimit)
			.usedCashLimit(usedCashLimit)
			.lastUpdateTime(card.getLastUpdateTime())
			.build();
	}
}
