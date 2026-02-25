package com.epic.cms.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epic.cms.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for report generation endpoints.
 * Provides endpoints to download Card Reports in PDF and CSV formats.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	/**
	 * Generate and download Card Report in PDF format.
	 * 
	 * GET /api/reports/cards/pdf
	 * 
	 * @param status Optional card status filter (IACT, CACT, DACT)
	 * @param search Optional search query for card number
	 * @param expiryDateFrom Optional expiry date from filter (yyyy-MM-dd)
	 * @param expiryDateTo Optional expiry date to filter (yyyy-MM-dd)
	 * @param creditLimitMin Optional minimum credit limit
	 * @param creditLimitMax Optional maximum credit limit
	 * @param cashLimitMin Optional minimum cash limit
	 * @param cashLimitMax Optional maximum cash limit
	 * @return PDF file with filtered card details
	 */
	@GetMapping("/cards/pdf")
	public ResponseEntity<byte[]> downloadCardReportPDF(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String expiryDateFrom,
			@RequestParam(required = false) String expiryDateTo,
			@RequestParam(required = false) String creditLimitMin,
			@RequestParam(required = false) String creditLimitMax,
			@RequestParam(required = false) String cashLimitMin,
			@RequestParam(required = false) String cashLimitMax) {
		log.info("Received request to download Card Report as PDF with filters - status: {}, search: {}, expiryFrom: {}, expiryTo: {}, creditMin: {}, creditMax: {}, cashMin: {}, cashMax: {}",
				status, search, expiryDateFrom, expiryDateTo, creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

		try {
			// Generate PDF report with filters
			byte[] pdfBytes = reportService.generateCardReportPDF(status, search, expiryDateFrom, expiryDateTo,
					creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

			// Generate dynamic filename based on status
			String filename = reportService.generateCardReportFilename(status, "pdf");

			// Set response headers for file download
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("attachment", filename);
			headers.setContentLength(pdfBytes.length);

			log.info("Card Report PDF downloaded successfully as {}", filename);
			return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Failed to generate Card Report PDF", e);
			// Return empty response with error status
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Generate and download Card Report in CSV format.
	 * 
	 * GET /api/reports/cards/csv
	 * 
	 * @param status Optional card status filter (IACT, CACT, DACT)
	 * @param search Optional search query for card number
	 * @param expiryDateFrom Optional expiry date from filter (yyyy-MM-dd)
	 * @param expiryDateTo Optional expiry date to filter (yyyy-MM-dd)
	 * @param creditLimitMin Optional minimum credit limit
	 * @param creditLimitMax Optional maximum credit limit
	 * @param cashLimitMin Optional minimum cash limit
	 * @param cashLimitMax Optional maximum cash limit
	 * @return CSV file with filtered card details
	 */
	@GetMapping("/cards/csv")
	public ResponseEntity<byte[]> downloadCardReportCSV(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String expiryDateFrom,
			@RequestParam(required = false) String expiryDateTo,
			@RequestParam(required = false) String creditLimitMin,
			@RequestParam(required = false) String creditLimitMax,
			@RequestParam(required = false) String cashLimitMin,
			@RequestParam(required = false) String cashLimitMax) {
		log.info("Received request to download Card Report as CSV with filters - status: {}, search: {}, expiryFrom: {}, expiryTo: {}, creditMin: {}, creditMax: {}, cashMin: {}, cashMax: {}",
				status, search, expiryDateFrom, expiryDateTo, creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

		try {
			// Generate CSV report with filters
			byte[] csvBytes = reportService.generateCardReportCSV(status, search, expiryDateFrom, expiryDateTo,
					creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

			// Generate dynamic filename based on status
			String filename = reportService.generateCardReportFilename(status, "csv");

			// Set response headers for file download
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment", filename);
			headers.setContentLength(csvBytes.length);

			log.info("Card Report CSV downloaded successfully as {}", filename);
			return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Failed to generate Card Report CSV", e);
			// Return empty response with error status
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	// ============================================================
	// Card Request Report Endpoints
	// ============================================================

	/**
	 * Generate and download Card Request Report in PDF format.
	 * 
	 * GET /api/reports/card-requests/pdf
	 * 
	 * @param status Optional request status filter (PEND, APPR, RJCT)
	 * @param requestType Optional request type filter (ACTI, CDCL)
	 * @param search Optional search query for card number
	 * @return PDF file with filtered card request details
	 */
	@GetMapping("/card-requests/pdf")
	public ResponseEntity<byte[]> downloadCardRequestReportPDF(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String requestType,
			@RequestParam(required = false) String search) {
		log.info("Received request to download Card Request Report as PDF with filters - status: {}, requestType: {}, search: {}",
				status, requestType, search);

		try {
			// Generate PDF report with filters
			byte[] pdfBytes = reportService.generateCardRequestReportPDF(status, requestType, search);

			// Generate dynamic filename based on status
			String filename = reportService.generateCardRequestReportFilename(status, "pdf");

			// Set response headers for file download
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("attachment", filename);
			headers.setContentLength(pdfBytes.length);

			log.info("Card Request Report PDF downloaded successfully as {}", filename);
			return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Failed to generate Card Request Report PDF", e);
			// Return empty response with error status
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Generate and download Card Request Report in CSV format.
	 * 
	 * GET /api/reports/card-requests/csv
	 * 
	 * @param status Optional request status filter (PEND, APPR, RJCT)
	 * @param requestType Optional request type filter (ACTI, CDCL)
	 * @param search Optional search query for card number
	 * @return CSV file with filtered card request details
	 */
	@GetMapping("/card-requests/csv")
	public ResponseEntity<byte[]> downloadCardRequestReportCSV(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String requestType,
			@RequestParam(required = false) String search) {
		log.info("Received request to download Card Request Report as CSV with filters - status: {}, requestType: {}, search: {}",
				status, requestType, search);

		try {
			// Generate CSV report with filters
			byte[] csvBytes = reportService.generateCardRequestReportCSV(status, requestType, search);

			// Generate dynamic filename based on status
			String filename = reportService.generateCardRequestReportFilename(status, "csv");

			// Set response headers for file download
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment", filename);
			headers.setContentLength(csvBytes.length);

			log.info("Card Request Report CSV downloaded successfully as {}", filename);
			return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

		} catch (Exception e) {
			log.error("Failed to generate Card Request Report CSV", e);
			// Return empty response with error status
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
