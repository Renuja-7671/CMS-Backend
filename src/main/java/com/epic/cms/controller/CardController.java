package com.epic.cms.controller;

import java.security.PrivateKey;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epic.cms.dto.ApiResponse;
import com.epic.cms.dto.CardDTO;
import com.epic.cms.dto.CreateCardRequest;
import com.epic.cms.dto.EncryptedPayloadRequest;
import com.epic.cms.dto.PageRequest;
import com.epic.cms.dto.PageResponse;
import com.epic.cms.dto.PaginatedQueryRequest;
import com.epic.cms.dto.SecureEncryptedPayloadRequest;
import com.epic.cms.dto.UpdateCardRequest;
import com.epic.cms.dto.ViewCardNumberRequest;
import com.epic.cms.dto.ViewCardNumberResponse;
import com.epic.cms.exception.InvalidOperationException;
import com.epic.cms.service.CardService;
import com.epic.cms.service.KeyManagementService;
import com.epic.cms.util.PayloadEncryptionUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
public class CardController {

	private final CardService cardService;
	private final PayloadEncryptionUtil payloadEncryptionUtil;
	private final KeyManagementService keyManagementService;

	/**
	 * Get all cards.
	 * 
	 * @return List of all cards
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<CardDTO>>> getAllCards() {
		log.info("GET /api/cards - Fetch all cards");
		List<CardDTO> cards = cardService.getAllCards();
		return ResponseEntity.ok(
				ApiResponse.success("Cards retrieved successfully", cards));
	}
	
	/**
	 * Get all cards with pagination and optional filtering.
	 * 
	 * @param page Page number (default 0)
	 * @param size Page size (default 10)
	 * @param status Optional card status filter (e.g., IACT, CACT, DACT, or ALL for no filter)
	 * @param search Optional card number search query
	 * @return Paginated list of cards
	 */
	@GetMapping("/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardDTO>>> getAllCardsPaginated(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search) {
		log.info("GET /api/cards/paginated - Fetch cards with pagination: page={}, size={}, status={}, search={}", 
				page, size, status, search);
		
		PageRequest pageRequest = PageRequest.of(page, size);
		PageResponse<CardDTO> cards = cardService.getAllCardsWithPagination(pageRequest, status, search);
		
		return ResponseEntity.ok(
				ApiResponse.success("Cards retrieved successfully", cards));
	}

	/**
	 * Get card by card number.
	 * 
	 * @param cardNumber The card number
	 * @return Card details
	 */
	@GetMapping("/{cardNumber}")
	public ResponseEntity<ApiResponse<CardDTO>> getCardByNumber(@PathVariable String cardNumber) {
		log.info("GET /api/cards/{} - Fetch card by number", "****" + cardNumber.substring(cardNumber.length() - 4));
		CardDTO card = cardService.getCardByNumber(cardNumber);
		return ResponseEntity.ok(
				ApiResponse.success("Card retrieved successfully", card));
	}
	
	/**
	 * Get card by display card number and encryption key.
	 * 
	 * @param displayCardNumber Display card number (first6 + encrypted + last4)
	 * @param encryptionKey Encryption key to decrypt middle digits
	 * @return Card details
	 */
	@GetMapping("/display/{displayCardNumber}")
	public ResponseEntity<ApiResponse<CardDTO>> getCardByDisplayNumber(
			@PathVariable String displayCardNumber,
			@RequestParam String encryptionKey) {
		log.info("GET /api/cards/display - Fetch card by display number");
		CardDTO card = cardService.getCardByDisplayNumber(displayCardNumber, encryptionKey);
		return ResponseEntity.ok(
				ApiResponse.success("Card retrieved successfully", card));
	}

	/**
	 * Get cards by status.
	 * 
	 * @param status Card status (IACT, CACT, DACT)
	 * @return List of cards with the specified status
	 */
	@GetMapping("/status/{status}")
	public ResponseEntity<ApiResponse<List<CardDTO>>> getCardsByStatus(@PathVariable String status) {
		log.info("GET /api/cards/status/{} - Fetch cards by status", status);
		List<CardDTO> cards = cardService.getCardsByStatus(status);
		return ResponseEntity.ok(
				ApiResponse.success("Cards retrieved successfully", cards));
	}

	/**
	 * Create a new card.
	 * 
	 * @param request Card creation request
	 * @return Created card details
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<CardDTO>> createCard(@Valid @RequestBody CreateCardRequest request) {
		log.info("POST /api/cards - Create new card: {}", request.getCardNumber());
		CardDTO createdCard = cardService.createCard(request);
		return ResponseEntity.ok(
				ApiResponse.success("Card created successfully", createdCard));
	}

	/**
	 * Create a new card with encrypted payload.
	 * Accepts an encrypted payload for enhanced security during transmission.
	 * 
	 * @param encryptedRequest Encrypted payload request containing encrypted data and encryption key
	 * @return Created card details
	 * @deprecated Use {@link #createCardSecure} for better security (RSA key exchange)
	 */
	@Deprecated
	@PostMapping("/encrypted")
	public ResponseEntity<ApiResponse<CardDTO>> createCardEncrypted(
			@Valid @RequestBody EncryptedPayloadRequest encryptedRequest) {
		log.info("POST /api/cards/encrypted - Create new card with encrypted payload (legacy mode)");
		
		// Decrypt the payload
		CreateCardRequest request = payloadEncryptionUtil.decryptPayload(
			encryptedRequest.getEncryptedData(),
			encryptedRequest.getEncryptionKey(),
			CreateCardRequest.class
		);
		
		log.info("Decrypted payload successfully for card: {}", request.getCardNumber());
		
		// Process the decrypted request
		CardDTO createdCard = cardService.createCard(request);
		
		return ResponseEntity.ok(
				ApiResponse.success("Card created successfully", createdCard));
	}

	/**
	 * Create a new card with secure encrypted payload using RSA + AES hybrid encryption.
	 * This is the RECOMMENDED secure endpoint.
	 * 
	 * Flow:
	 * 1. Client requests public key from GET /api/encryption/public-key
	 * 2. Client generates random AES-256 key
	 * 3. Client encrypts card data with AES key
	 * 4. Client encrypts AES key with RSA public key
	 * 5. Client sends sessionId, encryptedData, and encryptedKey
	 * 6. Backend decrypts AES key with RSA private key
	 * 7. Backend decrypts card data with AES key
	 * 8. Backend creates the card
	 * 
	 * @param secureRequest Secure encrypted payload request
	 * @return Created card details
	 */
	@PostMapping("/secure")
	public ResponseEntity<ApiResponse<CardDTO>> createCardSecure(
			@Valid @RequestBody SecureEncryptedPayloadRequest secureRequest) {
		log.info("POST /api/cards/secure - Create new card with secure encrypted payload (session: {})", 
				secureRequest.getSessionId());
		
		// Step 1: Retrieve private key for the session
		PrivateKey privateKey = keyManagementService.getPrivateKey(secureRequest.getSessionId());
		
		if (privateKey == null) {
			log.warn("Invalid or expired session ID: {}", secureRequest.getSessionId());
			throw new InvalidOperationException("Invalid or expired session ID. Please request a new public key.");
		}
		
		// Step 2: Decrypt the payload using RSA + AES
		CreateCardRequest request = payloadEncryptionUtil.decryptPayloadWithRSA(
			secureRequest.getEncryptedData(),
			secureRequest.getEncryptedKey(),
			privateKey,
			CreateCardRequest.class
		);
		
		log.info("Decrypted secure payload successfully for card: ****{}", 
				request.getCardNumber().substring(request.getCardNumber().length() - 4));
		
		// Step 3: Invalidate the key pair after successful decryption (one-time use)
		keyManagementService.invalidateKeyPair(secureRequest.getSessionId());
		log.debug("Invalidated key pair for session: {}", secureRequest.getSessionId());
		
		// Step 4: Process the decrypted request
		CardDTO createdCard = cardService.createCard(request);
		
		return ResponseEntity.ok(
				ApiResponse.success("Card created successfully", createdCard));
	}

	/**
	 * Update card details.
	 * 
	 * @param request Update request containing displayCardNumber, encryptionKey, and new values
	 * @return Updated card details
	 */
	@PutMapping
	public ResponseEntity<ApiResponse<CardDTO>> updateCard(@Valid @RequestBody UpdateCardRequest request) {
		log.info("PUT /api/cards - Update card");
		CardDTO updatedCard = cardService.updateCard(request.getDisplayCardNumber(), 
				request.getEncryptionKey(), request);
		return ResponseEntity.ok(
				ApiResponse.success("Card updated successfully", updatedCard));
	}

	/**
	 * Get all cards with pagination and optional filtering (POST version for encryption).
	 * This endpoint accepts encrypted payloads and supports the same filtering as the GET version.
	 * 
	 * @param queryRequest Paginated query request with filters
	 * @return Paginated list of cards
	 */
	@PostMapping("/query/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardDTO>>> queryCardsPaginated(
			@RequestBody PaginatedQueryRequest queryRequest) {
		log.info("POST /api/cards/query/paginated - Query cards with pagination: page={}, size={}, status={}, search={}", 
				queryRequest.getPage(), queryRequest.getSize(), queryRequest.getStatus(), queryRequest.getSearch());
		
		PageRequest pageRequest = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());
		PageResponse<CardDTO> cards = cardService.getAllCardsWithPagination(
			pageRequest, 
			queryRequest.getStatus(), 
			queryRequest.getSearch()
		);
		
		return ResponseEntity.ok(
				ApiResponse.success("Cards retrieved successfully", cards));
	}

	/**
	 * Get all cards (POST version for encryption).
	 * 
	 * @return List of all cards
	 */
	@PostMapping("/query/all")
	public ResponseEntity<ApiResponse<List<CardDTO>>> queryAllCards() {
		log.info("POST /api/cards/query/all - Fetch all cards");
		List<CardDTO> cards = cardService.getAllCards();
		return ResponseEntity.ok(
				ApiResponse.success("Cards retrieved successfully", cards));
	}

	/**
	 * View plain card number with admin password verification.
	 * Endpoint: POST /api/cards/view-plain-number
	 * 
	 * @param request ViewCardNumberRequest with cardId and admin password
	 * @return Plain card number if password is correct
	 */
	@PostMapping("/view-plain-number")
	public ResponseEntity<ApiResponse<ViewCardNumberResponse>> viewPlainCardNumber(
			@Valid @RequestBody ViewCardNumberRequest request) {
		log.info("POST /api/cards/view-plain-number - Request to view plain card number for card ID: {}", 
				request.getCardId());
		
		ViewCardNumberResponse response = cardService.viewPlainCardNumber(request);
		
		return ResponseEntity.ok(
				ApiResponse.success("Card number retrieved successfully", response));
	}
}
