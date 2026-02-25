package com.epic.cms.controller;

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
import com.epic.cms.dto.ApproveRejectRequest;
import com.epic.cms.dto.CardRequestDTO;
import com.epic.cms.dto.CardRequestDetailDTO;
import com.epic.cms.dto.CreateCardRequestDTO;
import com.epic.cms.dto.PageRequest;
import com.epic.cms.dto.PageResponse;
import com.epic.cms.dto.PaginatedQueryRequest;
import com.epic.cms.service.CardRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for card request operations.
 * Handles activation and deactivation requests for cards.
 */
@RestController
@RequestMapping("/api/card-requests")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CardRequestController {
	
	private final CardRequestService cardRequestService;
	
	/**
	 * Create a new card request (activation or deactivation).
	 * 
	 * POST /api/card-requests
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<CardRequestDTO>> createCardRequest(
			@Valid @RequestBody CreateCardRequestDTO request) {
		
		log.info("Received request to create card request");
		
		CardRequestDTO createdRequest = cardRequestService.createCardRequest(request);
		
		ApiResponse<CardRequestDTO> response = ApiResponse.<CardRequestDTO>builder()
			.success(true)
			.message("Card request created successfully")
			.data(createdRequest)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get all card requests.
	 * 
	 * GET /api/card-requests
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<List<CardRequestDTO>>> getAllCardRequests() {
		log.info("Received request to get all card requests");
		
		List<CardRequestDTO> requests = cardRequestService.getAllCardRequests();
		
		ApiResponse<List<CardRequestDTO>> response = ApiResponse.<List<CardRequestDTO>>builder()
			.success(true)
			.message("Card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get all card requests with pagination and optional filtering.
	 * 
	 * GET /api/card-requests/paginated
	 * 
	 * @param page Page number (default 0)
	 * @param size Page size (default 10)
	 * @param status Optional request status filter (e.g., PEND, APPR, RJCT, or ALL for no filter)
	 * @param search Optional card number search query
	 */
	@GetMapping("/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardRequestDTO>>> getAllCardRequestsPaginated(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search) {
		log.info("Received request to get all card requests with pagination: page={}, size={}, status={}, search={}", 
				page, size, status, search);
		
		PageRequest pageRequest = PageRequest.of(page, size);
		PageResponse<CardRequestDTO> requests = cardRequestService.getAllCardRequestsWithPagination(pageRequest, status, search);
		
		ApiResponse<PageResponse<CardRequestDTO>> response = ApiResponse.<PageResponse<CardRequestDTO>>builder()
			.success(true)
			.message("Card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get all pending card requests.
	 * 
	 * GET /api/card-requests/pending
	 */
	@GetMapping("/pending")
	public ResponseEntity<ApiResponse<List<CardRequestDTO>>> getPendingCardRequests() {
		log.info("Received request to get pending card requests");
		
		List<CardRequestDTO> requests = cardRequestService.getPendingCardRequests();
		
		ApiResponse<List<CardRequestDTO>> response = ApiResponse.<List<CardRequestDTO>>builder()
			.success(true)
			.message("Pending card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get all pending card requests with pagination.
	 * 
	 * GET /api/card-requests/pending/paginated
	 */
	@GetMapping("/pending/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardRequestDTO>>> getPendingCardRequestsPaginated(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		log.info("Received request to get pending card requests with pagination: page={}, size={}", page, size);
		
		PageRequest pageRequest = PageRequest.of(page, size);
		PageResponse<CardRequestDTO> requests = cardRequestService.getPendingCardRequestsWithPagination(pageRequest);
		
		ApiResponse<PageResponse<CardRequestDTO>> response = ApiResponse.<PageResponse<CardRequestDTO>>builder()
			.success(true)
			.message("Pending card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get a card request by ID.
	 * 
	 * GET /api/card-requests/{requestId}
	 */
	@GetMapping("/{requestId}")
	public ResponseEntity<ApiResponse<CardRequestDTO>> getCardRequestById(
			@PathVariable Long requestId) {
		
		log.info("Received request to get card request with ID: {}", requestId);
		
		CardRequestDTO request = cardRequestService.getCardRequestById(requestId);
		
		ApiResponse<CardRequestDTO> response = ApiResponse.<CardRequestDTO>builder()
			.success(true)
			.message("Card request retrieved successfully")
			.data(request)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Approve a card request.
	 * Updates the request status to APPR and changes the card status accordingly.
	 * 
	 * PUT /api/card-requests/{requestId}/approve?approvedUser=username
	 */
	@PutMapping("/{requestId}/approve")
	public ResponseEntity<ApiResponse<CardRequestDTO>> approveCardRequest(
			@PathVariable Long requestId,
			@RequestParam String approvedUser) {
		
		log.info("Received request to approve card request with ID: {} by user: {}", requestId, approvedUser);
		
		CardRequestDTO approvedRequest = cardRequestService.approveCardRequest(requestId, approvedUser);
		
		ApiResponse<CardRequestDTO> response = ApiResponse.<CardRequestDTO>builder()
			.success(true)
			.message("Card request approved successfully")
			.data(approvedRequest)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Reject a card request.
	 * Updates the request status to RJCT without changing the card status.
	 * 
	 * PUT /api/card-requests/{requestId}/reject?approvedUser=username
	 */
	@PutMapping("/{requestId}/reject")
	public ResponseEntity<ApiResponse<CardRequestDTO>> rejectCardRequest(
			@PathVariable Long requestId,
			@RequestParam String approvedUser) {
		
		log.info("Received request to reject card request with ID: {} by user: {}", requestId, approvedUser);
		
		CardRequestDTO rejectedRequest = cardRequestService.rejectCardRequest(requestId, approvedUser);
		
		ApiResponse<CardRequestDTO> response = ApiResponse.<CardRequestDTO>builder()
			.success(true)
			.message("Card request rejected successfully")
			.data(rejectedRequest)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get pending requests for inactive/deactivated cards with full card details.
	 * This is used for the request confirmation page.
	 * 
	 * GET /api/card-requests/pending/details
	 */
	@GetMapping("/pending/details")
	public ResponseEntity<ApiResponse<List<CardRequestDetailDTO>>> getPendingRequestsWithDetails() {
		log.info("Received request to get pending requests with card details");
		
		List<CardRequestDetailDTO> requests = cardRequestService.getPendingRequestsWithCardDetails();
		
		ApiResponse<List<CardRequestDetailDTO>> response = ApiResponse.<List<CardRequestDetailDTO>>builder()
			.success(true)
			.message("Pending requests with card details retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Get pending requests with full card details with pagination.
	 * 
	 * GET /api/card-requests/pending/details/paginated
	 */
	@GetMapping("/pending/details/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardRequestDetailDTO>>> getPendingRequestsWithDetailsPaginated(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "PEND") String status) {
		log.info("Received request to get requests with card details and pagination: page={}, size={}, status={}", 
				page, size, status);
		
		PageRequest pageRequest = PageRequest.of(page, size);
		PageResponse<CardRequestDetailDTO> requests = cardRequestService.getRequestsWithCardDetailsPaginated(
				status, pageRequest);
		
		ApiResponse<PageResponse<CardRequestDetailDTO>> response = ApiResponse.<PageResponse<CardRequestDetailDTO>>builder()
			.success(true)
			.message("Requests with card details retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Approve a card request with full card details response.
	 * Updates the request status to APPR and changes the card status accordingly.
	 * For ACTI requests: Card status IACT/DACT -> CACT
	 * 
	 * PUT /api/card-requests/{requestId}/approve/details
	 */
	@PutMapping("/{requestId}/approve/details")
	public ResponseEntity<ApiResponse<CardRequestDetailDTO>> approveRequestWithDetails(
			@PathVariable Long requestId,
			@Valid @RequestBody ApproveRejectRequest request) {
		
		log.info("Received request to approve card request with ID: {} by user: {} (with details)", 
				requestId, request.getApprovedUser());
		
		CardRequestDetailDTO approvedRequest = cardRequestService.approveRequestWithDetails(
			requestId, 
			request.getApprovedUser()
		);
		
		ApiResponse<CardRequestDetailDTO> response = ApiResponse.<CardRequestDetailDTO>builder()
			.success(true)
			.message("Card request approved successfully and card activated")
			.data(approvedRequest)
			.build();
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Reject a card request with card status update.
	 * Updates the request status to RJCT and sets card status to DACT.
	 * 
	 * PUT /api/card-requests/{requestId}/reject/details
	 */
	@PutMapping("/{requestId}/reject/details")
	public ResponseEntity<ApiResponse<CardRequestDetailDTO>> rejectRequestWithDetails(
			@PathVariable Long requestId,
			@Valid @RequestBody ApproveRejectRequest request) {
		
		log.info("Received request to reject card request with ID: {} by user: {} (with details)", 
				requestId, request.getApprovedUser());
		
		CardRequestDetailDTO rejectedRequest = cardRequestService.rejectRequestWithDetails(
			requestId, 
			request.getApprovedUser()
		);
		
		ApiResponse<CardRequestDetailDTO> response = ApiResponse.<CardRequestDetailDTO>builder()
			.success(true)
			.message("Card request rejected and card deactivated")
			.data(rejectedRequest)
			.build();
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Get all card requests with pagination and filtering (POST version for encryption).
	 * 
	 * POST /api/card-requests/query/paginated
	 */
	@PostMapping("/query/paginated")
	public ResponseEntity<ApiResponse<PageResponse<CardRequestDTO>>> queryCardRequestsPaginated(
			@RequestBody PaginatedQueryRequest queryRequest) {
		log.info("POST /api/card-requests/query/paginated - Query card requests: page={}, size={}, status={}, search={}", 
				queryRequest.getPage(), queryRequest.getSize(), queryRequest.getStatus(), queryRequest.getSearch());
		
		PageRequest pageRequest = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());
		PageResponse<CardRequestDTO> requests = cardRequestService.getAllCardRequestsWithPagination(
			pageRequest, 
			queryRequest.getStatus(), 
			queryRequest.getSearch()
		);
		
		ApiResponse<PageResponse<CardRequestDTO>> response = ApiResponse.<PageResponse<CardRequestDTO>>builder()
			.success(true)
			.message("Card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Get all card requests (POST version for encryption).
	 * 
	 * POST /api/card-requests/query/all
	 */
	@PostMapping("/query/all")
	public ResponseEntity<ApiResponse<List<CardRequestDTO>>> queryAllCardRequests() {
		log.info("POST /api/card-requests/query/all - Fetch all card requests");
		
		List<CardRequestDTO> requests = cardRequestService.getAllCardRequests();
		
		ApiResponse<List<CardRequestDTO>> response = ApiResponse.<List<CardRequestDTO>>builder()
			.success(true)
			.message("Card requests retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Get pending requests with card details (POST version for encryption with filtering).
	 * 
	 * POST /api/card-requests/query/pending/details
	 */
	@PostMapping("/query/pending/details")
	public ResponseEntity<ApiResponse<PageResponse<CardRequestDetailDTO>>> queryPendingRequestsWithDetails(
			@RequestBody PaginatedQueryRequest queryRequest) {
		log.info("POST /api/card-requests/query/pending/details - Query requests with card details: page={}, size={}, status={}", 
				queryRequest.getPage(), queryRequest.getSize(), queryRequest.getStatus());
		
		String status = queryRequest.getStatus() != null ? queryRequest.getStatus() : "PEND";
		PageRequest pageRequest = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());
		PageResponse<CardRequestDetailDTO> requests = cardRequestService.getRequestsWithCardDetailsPaginated(
				status, pageRequest);
		
		ApiResponse<PageResponse<CardRequestDetailDTO>> response = ApiResponse.<PageResponse<CardRequestDetailDTO>>builder()
			.success(true)
			.message("Requests with card details retrieved successfully")
			.data(requests)
			.build();
		
		return ResponseEntity.ok(response);
	}
}
