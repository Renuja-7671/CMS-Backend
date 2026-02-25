package com.epic.cms.service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.epic.cms.dto.CardReportDTO;
import com.epic.cms.dto.CardRequestReportDTO;
import com.epic.cms.exception.ReportGenerationException;
import com.epic.cms.repository.CardReportRepository;
import com.epic.cms.repository.CardRequestReportRepository;
import com.epic.cms.util.CardEncryptionUtil;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating reports in PDF and CSV formats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

	private final CardReportRepository cardReportRepository;
	private final CardRequestReportRepository cardRequestReportRepository;
	private final CardEncryptionUtil cardEncryptionUtil;

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Generate Card Report in PDF format.
	 * 
	 * @param status Optional status filter (e.g., "IACT", "CACT", "DACT")
	 * @param search Optional search query for card number
	 * @param expiryDateFrom Optional expiry date from filter (ISO format: yyyy-MM-dd)
	 * @param expiryDateTo Optional expiry date to filter (ISO format: yyyy-MM-dd)
	 * @param creditLimitMin Optional minimum credit limit
	 * @param creditLimitMax Optional maximum credit limit
	 * @param cashLimitMin Optional minimum cash limit
	 * @param cashLimitMax Optional maximum cash limit
	 * @return PDF file as byte array
	 * @throws ReportGenerationException if PDF generation fails
	 */
	public byte[] generateCardReportPDF(String status, String search, String expiryDateFrom, String expiryDateTo,
			String creditLimitMin, String creditLimitMax, String cashLimitMin, String cashLimitMax) {
		try {
		log.info("Starting Card Report PDF generation with filters - status: {}, search: {}, expiryFrom: {}, expiryTo: {}, creditMin: {}, creditMax: {}, cashMin: {}, cashMax: {}",
				status, search, expiryDateFrom, expiryDateTo, creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

		// Fetch card data with optional filters
		List<CardReportDTO> cards = cardReportRepository.findCardsForReportWithFilters(
				status, search, expiryDateFrom, expiryDateTo,
				creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

			// Process each card to decrypt and mask card numbers
			for (CardReportDTO card : cards) {
				String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(card.getEncryptedCardNumber());
				String maskedCardNumber = maskCardNumber(plainCardNumber);
				card.setMaskedCardNumber(maskedCardNumber);
			}

			log.info("Found {} cards for report", cards.size());

			// Create PDF document in memory
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfWriter writer = new PdfWriter(baos);
			PdfDocument pdfDoc = new PdfDocument(writer);
			Document document = new Document(pdfDoc);

			// Determine title based on status filter
			String reportTitle = buildCardReportTitle(status);
			Paragraph title = new Paragraph(reportTitle)
					.setFontSize(18)
					.setBold()
					.setTextAlignment(TextAlignment.CENTER);
			document.add(title);

			// Add generation timestamp
			Paragraph timestamp = new Paragraph(
					"Generated on: " + LocalDateTime.now().format(DATETIME_FORMATTER))
					.setFontSize(10)
					.setTextAlignment(TextAlignment.CENTER)
					.setMarginBottom(10);
			document.add(timestamp);

			// Add filter summary section
			addCardFilterSummary(document, status, search, expiryDateFrom, expiryDateTo,
					creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

		// Create table with 8 columns (removed Status Code)
		float[] columnWidths = { 1.5f, 1f, 1.2f, 1f, 1f, 1.2f, 1.2f, 1.5f, 1.5f };
		Table table = new Table(UnitValue.createPercentArray(columnWidths));
		table.setWidth(UnitValue.createPercentValue(100));

		// Add table headers
		addTableHeader(table, "Masked Card Number");
		addTableHeader(table, "Expiry Date");
		addTableHeader(table, "Status");
		addTableHeader(table, "Credit Limit");
		addTableHeader(table, "Cash Limit");
		addTableHeader(table, "Available Credit");
		addTableHeader(table, "Available Cash");
		addTableHeader(table, "Last Updated");
		addTableHeader(table, "Updated By");

		// Add data rows
		for (CardReportDTO card : cards) {
			table.addCell(new Cell().add(new Paragraph(card.getMaskedCardNumber()).setFontSize(9)));
			table.addCell(new Cell().add(new Paragraph(
					card.getExpiryDate() != null ? card.getExpiryDate().format(DATE_FORMATTER) : "N/A")
					.setFontSize(9)));
			table.addCell(new Cell().add(new Paragraph(
					card.getStatusDescription() != null ? card.getStatusDescription() : "N/A")
					.setFontSize(9)));
			table.addCell(new Cell().add(new Paragraph(
					card.getCreditLimit() != null ? String.format("%.2f", card.getCreditLimit()) : "0.00")
					.setFontSize(9)
					.setTextAlignment(TextAlignment.RIGHT)));
			table.addCell(new Cell().add(new Paragraph(
					card.getCashLimit() != null ? String.format("%.2f", card.getCashLimit()) : "0.00")
					.setFontSize(9)
					.setTextAlignment(TextAlignment.RIGHT)));
			table.addCell(new Cell().add(new Paragraph(
					card.getAvailableCreditLimit() != null ? String.format("%.2f", card.getAvailableCreditLimit())
							: "0.00")
					.setFontSize(9)
					.setTextAlignment(TextAlignment.RIGHT)));
			table.addCell(new Cell().add(new Paragraph(
					card.getAvailableCashLimit() != null ? String.format("%.2f", card.getAvailableCashLimit())
							: "0.00")
					.setFontSize(9)
					.setTextAlignment(TextAlignment.RIGHT)));
			table.addCell(new Cell().add(new Paragraph(
					card.getLastUpdatedTime() != null ? card.getLastUpdatedTime().format(DATETIME_FORMATTER)
							: "N/A")
					.setFontSize(9)));
			table.addCell(new Cell().add(new Paragraph(
					card.getLastUpdatedUserFullName() != null ? card.getLastUpdatedUserFullName()
							: card.getLastUpdatedUserName())
					.setFontSize(9)));
		}

			document.add(table);

			// Add footer with total count
			Paragraph footer = new Paragraph("Total Cards: " + cards.size())
					.setFontSize(10)
					.setMarginTop(20);
			document.add(footer);

			// Close document
			document.close();

			log.info("Card Report PDF generated successfully with {} cards", cards.size());
			return baos.toByteArray();

		} catch (Exception e) {
			log.error("Failed to generate Card Report PDF", e);
			throw new ReportGenerationException("Card Report PDF generation", "Failed to generate PDF report", e);
		}
	}

	/**
	 * Generate Card Report in CSV format.
	 * 
	 * @param status Optional status filter (e.g., "IACT", "CACT", "DACT")
	 * @param search Optional search query for card number
	 * @param expiryDateFrom Optional expiry date from filter (ISO format: yyyy-MM-dd)
	 * @param expiryDateTo Optional expiry date to filter (ISO format: yyyy-MM-dd)
	 * @param creditLimitMin Optional minimum credit limit
	 * @param creditLimitMax Optional maximum credit limit
	 * @param cashLimitMin Optional minimum cash limit
	 * @param cashLimitMax Optional maximum cash limit
	 * @return CSV file content as byte array
	 * @throws ReportGenerationException if CSV generation fails
	 */
	public byte[] generateCardReportCSV(String status, String search, String expiryDateFrom, String expiryDateTo,
			String creditLimitMin, String creditLimitMax, String cashLimitMin, String cashLimitMax) {
		try {
		log.info("Starting Card Report CSV generation with filters - status: {}, search: {}, expiryFrom: {}, expiryTo: {}, creditMin: {}, creditMax: {}, cashMin: {}, cashMax: {}",
				status, search, expiryDateFrom, expiryDateTo, creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

		// Fetch card data with optional filters
		List<CardReportDTO> cards = cardReportRepository.findCardsForReportWithFilters(
				status, search, expiryDateFrom, expiryDateTo,
				creditLimitMin, creditLimitMax, cashLimitMin, cashLimitMax);

			// Process each card to decrypt and mask card numbers
			for (CardReportDTO card : cards) {
				String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(card.getEncryptedCardNumber());
				String maskedCardNumber = maskCardNumber(plainCardNumber);
				card.setMaskedCardNumber(maskedCardNumber);
			}

			log.info("Found {} cards for report", cards.size());

		// Create CSV in memory
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);

		// Add report title
		String reportTitle = buildCardReportTitle(status);
		csvWriter.writeNext(new String[] { reportTitle });
		csvWriter.writeNext(new String[] { "Generated on: " + LocalDateTime.now().format(DATETIME_FORMATTER) });
		csvWriter.writeNext(new String[] { "" }); // Empty line

		// Add filter summary
		csvWriter.writeNext(new String[] { "Applied Filters:" });
		csvWriter.writeNext(new String[] { "Status", getStatusDescription(status) });
		csvWriter.writeNext(new String[] { "Search Query", search != null && !search.trim().isEmpty() ? search : "None" });
		csvWriter.writeNext(new String[] { "Expiry Date From", expiryDateFrom != null && !expiryDateFrom.trim().isEmpty() ? expiryDateFrom : "Not Applied" });
		csvWriter.writeNext(new String[] { "Expiry Date To", expiryDateTo != null && !expiryDateTo.trim().isEmpty() ? expiryDateTo : "Not Applied" });
		csvWriter.writeNext(new String[] { "Credit Limit Min", creditLimitMin != null && !creditLimitMin.trim().isEmpty() ? "LKR " + creditLimitMin : "Not Applied" });
		csvWriter.writeNext(new String[] { "Credit Limit Max", creditLimitMax != null && !creditLimitMax.trim().isEmpty() ? "LKR " + creditLimitMax : "Not Applied" });
		csvWriter.writeNext(new String[] { "Cash Limit Min", cashLimitMin != null && !cashLimitMin.trim().isEmpty() ? "LKR " + cashLimitMin : "Not Applied" });
		csvWriter.writeNext(new String[] { "Cash Limit Max", cashLimitMax != null && !cashLimitMax.trim().isEmpty() ? "LKR " + cashLimitMax : "Not Applied" });
		csvWriter.writeNext(new String[] { "" }); // Empty line

		// Add header row (without Status Code)
		String[] header = {
				"Masked Card Number",
				"Expiry Date",
				"Status Description",
				"Credit Limit",
				"Cash Limit",
				"Available Credit Limit",
				"Available Cash Limit",
				"Last Updated Time",
				"Last Updated User Name",
				"Last Updated User Full Name"
		};
		csvWriter.writeNext(header);

		// Add data rows (without Status Code)
		for (CardReportDTO card : cards) {
			String[] row = {
					card.getMaskedCardNumber() != null ? card.getMaskedCardNumber() : "",
					card.getExpiryDate() != null ? card.getExpiryDate().format(DATE_FORMATTER) : "",
					card.getStatusDescription() != null ? card.getStatusDescription() : "",
					card.getCreditLimit() != null ? String.format("%.2f", card.getCreditLimit()) : "0.00",
					card.getCashLimit() != null ? String.format("%.2f", card.getCashLimit()) : "0.00",
					card.getAvailableCreditLimit() != null ? String.format("%.2f", card.getAvailableCreditLimit())
							: "0.00",
					card.getAvailableCashLimit() != null ? String.format("%.2f", card.getAvailableCashLimit())
							: "0.00",
					card.getLastUpdatedTime() != null ? card.getLastUpdatedTime().format(DATETIME_FORMATTER) : "",
					card.getLastUpdatedUserName() != null ? card.getLastUpdatedUserName() : "",
					card.getLastUpdatedUserFullName() != null ? card.getLastUpdatedUserFullName() : ""
			};
			csvWriter.writeNext(row);
		}

			csvWriter.close();

			log.info("Card Report CSV generated successfully with {} cards", cards.size());
			return stringWriter.toString().getBytes();

		} catch (Exception e) {
			log.error("Failed to generate Card Report CSV", e);
			throw new ReportGenerationException("Card Report CSV generation", "Failed to generate CSV report", e);
		}
	}

	/**
	 * Mask card number showing first 6 digits and last 4 digits.
	 * 
	 * @param plainCardNumber Full plain card number
	 * @return Masked card number (e.g., 123456****1234)
	 */
	private String maskCardNumber(String plainCardNumber) {
		if (plainCardNumber == null || plainCardNumber.length() < 10) {
			return "****";
		}
		// Show first 6 digits + **** + last 4 digits (e.g., 123456****1234)
		String first6 = plainCardNumber.substring(0, 6);
		String last4 = plainCardNumber.substring(plainCardNumber.length() - 4);
		return first6 + "****" + last4;
	}

	/**
	 * Helper method to add styled header cells to PDF table.
	 */
	private void addTableHeader(Table table, String headerText) {
		// Light blue color (RGB: 173, 216, 230)
		DeviceRgb lightBlue = new DeviceRgb(173, 216, 230);
		
		Cell header = new Cell()
				.add(new Paragraph(headerText).setBold().setFontSize(10))
				.setBackgroundColor(lightBlue)
				.setTextAlignment(TextAlignment.CENTER);
		table.addHeaderCell(header);
	}

	/**
	 * Build card report title based on status filter.
	 */
	private String buildCardReportTitle(String status) {
		if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
			return "Card Report - All Cards";
		}
		switch (status.toUpperCase()) {
			case "IACT":
				return "Card Report - Inactive Cards";
			case "CACT":
				return "Card Report - Active Cards";
			case "DACT":
				return "Card Report - Deactivated Cards";
			default:
				return "Card Report - " + status;
		}
	}

	/**
	 * Build card request report title based on status filter.
	 */
	private String buildCardRequestReportTitle(String status) {
		if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
			return "Card Request Report - All Requests";
		}
		switch (status.toUpperCase()) {
			case "PEND":
				return "Card Request Report - Pending Requests";
			case "APPR":
				return "Card Request Report - Approved Requests";
			case "RJCT":
				return "Card Request Report - Rejected Requests";
			default:
				return "Card Request Report - " + status;
		}
	}

	/**
	 * Add filter summary section to PDF document for card reports.
	 */
	private void addCardFilterSummary(Document document, String status, String search,
			String expiryDateFrom, String expiryDateTo, String creditLimitMin, String creditLimitMax,
			String cashLimitMin, String cashLimitMax) {
		
		Paragraph filterHeader = new Paragraph("Applied Filters:")
				.setFontSize(12)
				.setBold()
				.setMarginTop(10);
		document.add(filterHeader);

		StringBuilder filterText = new StringBuilder();
		filterText.append("Status: ").append(getStatusDescription(status)).append("\n");
		filterText.append("Search Query: ").append(search != null && !search.trim().isEmpty() ? search : "None").append("\n");
		filterText.append("Expiry Date From: ").append(expiryDateFrom != null && !expiryDateFrom.trim().isEmpty() ? expiryDateFrom : "Not Applied").append("\n");
		filterText.append("Expiry Date To: ").append(expiryDateTo != null && !expiryDateTo.trim().isEmpty() ? expiryDateTo : "Not Applied").append("\n");
		filterText.append("Credit Limit Min: ").append(creditLimitMin != null && !creditLimitMin.trim().isEmpty() ? "LKR " + creditLimitMin : "Not Applied").append("\n");
		filterText.append("Credit Limit Max: ").append(creditLimitMax != null && !creditLimitMax.trim().isEmpty() ? "LKR " + creditLimitMax : "Not Applied").append("\n");
		filterText.append("Cash Limit Min: ").append(cashLimitMin != null && !cashLimitMin.trim().isEmpty() ? "LKR " + cashLimitMin : "Not Applied").append("\n");
		filterText.append("Cash Limit Max: ").append(cashLimitMax != null && !cashLimitMax.trim().isEmpty() ? "LKR " + cashLimitMax : "Not Applied");

		Paragraph filters = new Paragraph(filterText.toString())
				.setFontSize(9)
				.setMarginBottom(15);
		document.add(filters);
	}

	/**
	 * Add filter summary section to PDF document for card request reports.
	 */
	private void addCardRequestFilterSummary(Document document, String status, String requestType, String search) {
		Paragraph filterHeader = new Paragraph("Applied Filters:")
				.setFontSize(12)
				.setBold()
				.setMarginTop(10);
		document.add(filterHeader);

		StringBuilder filterText = new StringBuilder();
		filterText.append("Status: ").append(getRequestStatusDescription(status)).append("\n");
		filterText.append("Request Type: ").append(getRequestTypeDescription(requestType)).append("\n");
		filterText.append("Search Query: ").append(search != null && !search.trim().isEmpty() ? search : "None");

		Paragraph filters = new Paragraph(filterText.toString())
				.setFontSize(9)
				.setMarginBottom(15);
		document.add(filters);
	}

	/**
	 * Get human-readable status description.
	 */
	private String getStatusDescription(String status) {
		if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
			return "All Cards";
		}
		switch (status.toUpperCase()) {
			case "IACT":
				return "Inactive";
			case "CACT":
				return "Active";
			case "DACT":
				return "Deactivated";
			default:
				return status;
		}
	}

	/**
	 * Get human-readable request status description.
	 */
	private String getRequestStatusDescription(String status) {
		if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
			return "All Statuses";
		}
		switch (status.toUpperCase()) {
			case "PEND":
				return "Pending";
			case "APPR":
				return "Approved";
			case "RJCT":
				return "Rejected";
			default:
				return status;
		}
	}

	/**
	 * Get human-readable request type description.
	 */
	private String getRequestTypeDescription(String requestType) {
		if (requestType == null || requestType.trim().isEmpty() || "ALL".equalsIgnoreCase(requestType)) {
			return "All Types";
		}
		switch (requestType.toUpperCase()) {
			case "ACTI":
				return "Activation";
			case "CDCL":
				return "Card Closure";
			default:
				return requestType;
		}
	}

	/**
	 * Generate filename for card report based on status and format.
	 */
	public String generateCardReportFilename(String status, String format) {
		String statusPart = "";
		if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			switch (status.toUpperCase()) {
				case "IACT":
					statusPart = "-inactive";
					break;
				case "CACT":
					statusPart = "-active";
					break;
				case "DACT":
					statusPart = "-deactivated";
					break;
				default:
					statusPart = "-" + status.toLowerCase();
			}
		}
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		return "card-report" + statusPart + "-" + timestamp + "." + format;
	}

	/**
	 * Generate filename for card request report based on status and format.
	 */
	public String generateCardRequestReportFilename(String status, String format) {
		String statusPart = "";
		if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			switch (status.toUpperCase()) {
				case "PEND":
					statusPart = "-pending";
					break;
				case "APPR":
					statusPart = "-approved";
					break;
				case "RJCT":
					statusPart = "-rejected";
					break;
				default:
					statusPart = "-" + status.toLowerCase();
			}
		}
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		return "card-request-report" + statusPart + "-" + timestamp + "." + format;
	}

	// ============================================================
	// Card Request Report Generation Methods
	// ============================================================

	/**
	 * Generate Card Request Report in PDF format.
	 * 
	 * @param status Optional status filter (e.g., "PEND", "APPR", "RJCT")
	 * @param requestType Optional request type filter (e.g., "ACTI", "CDCL")
	 * @param search Optional search query for card number
	 * @return PDF file as byte array
	 * @throws ReportGenerationException if PDF generation fails
	 */
	public byte[] generateCardRequestReportPDF(String status, String requestType, String search) {
		try {
			log.info("Starting Card Request Report PDF generation with filters - status: {}, requestType: {}, search: {}",
					status, requestType, search);

			// Fetch card request data with optional filters
			List<CardRequestReportDTO> requests = cardRequestReportRepository.findCardRequestsForReportWithFilters(
					status, requestType, search);

			// Process each request to decrypt and mask card numbers
			for (CardRequestReportDTO request : requests) {
				String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(request.getEncryptedCardNumber());
				String maskedCardNumber = maskCardNumber(plainCardNumber);
				request.setMaskedCardNumber(maskedCardNumber);
			}

			log.info("Found {} card requests for report", requests.size());

		// Create PDF document in memory
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdfDoc = new PdfDocument(writer);
		Document document = new Document(pdfDoc);

		// Determine title based on status filter
		String reportTitle = buildCardRequestReportTitle(status);
		Paragraph title = new Paragraph(reportTitle)
				.setFontSize(18)
				.setBold()
				.setTextAlignment(TextAlignment.CENTER);
		document.add(title);

		// Add generation timestamp
		Paragraph timestamp = new Paragraph(
				"Generated on: " + LocalDateTime.now().format(DATETIME_FORMATTER))
				.setFontSize(10)
				.setTextAlignment(TextAlignment.CENTER)
				.setMarginBottom(10);
		document.add(timestamp);

		// Add filter summary section
		addCardRequestFilterSummary(document, status, requestType, search);

			// Create table with 10 columns
			float[] columnWidths = { 0.8f, 1.5f, 1f, 1f, 1.5f, 1.2f, 1.2f, 1f, 1f };
			Table table = new Table(UnitValue.createPercentArray(columnWidths));
			table.setWidth(UnitValue.createPercentValue(100));

			// Add table headers
			addTableHeader(table, "Request ID");
			addTableHeader(table, "Card Number");
			addTableHeader(table, "Request Type");
			addTableHeader(table, "Status");
			addTableHeader(table, "Reason");
			addTableHeader(table, "Requested At");
			addTableHeader(table, "Processed At");
			addTableHeader(table, "Requested By");
			addTableHeader(table, "Approved By");

			// Add data rows
			for (CardRequestReportDTO request : requests) {
				table.addCell(new Cell().add(new Paragraph(String.valueOf(request.getRequestId())).setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(request.getMaskedCardNumber()).setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getRequestTypeDescription() != null ? request.getRequestTypeDescription() : "N/A")
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getRequestStatusDescription() != null ? request.getRequestStatusDescription() : "N/A")
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getReason() != null ? request.getReason() : "N/A")
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getRequestedAt() != null ? request.getRequestedAt().format(DATETIME_FORMATTER) : "N/A")
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getProcessedAt() != null ? request.getProcessedAt().format(DATETIME_FORMATTER) : "N/A")
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getRequestedUserFullName() != null ? request.getRequestedUserFullName()
								: (request.getRequestedUserName() != null ? request.getRequestedUserName() : "N/A"))
						.setFontSize(9)));
				table.addCell(new Cell().add(new Paragraph(
						request.getApprovedUserFullName() != null ? request.getApprovedUserFullName()
								: (request.getApprovedUserName() != null ? request.getApprovedUserName() : "N/A"))
						.setFontSize(9)));
			}

			document.add(table);

			// Add footer with total count
			Paragraph footer = new Paragraph("Total Requests: " + requests.size())
					.setFontSize(10)
					.setMarginTop(20);
			document.add(footer);

			// Close document
			document.close();

			log.info("Card Request Report PDF generated successfully with {} requests", requests.size());
			return baos.toByteArray();

		} catch (Exception e) {
			log.error("Failed to generate Card Request Report PDF", e);
			throw new ReportGenerationException("Card Request Report PDF generation", "Failed to generate PDF report", e);
		}
	}

	/**
	 * Generate Card Request Report in CSV format.
	 * 
	 * @param status Optional status filter (e.g., "PEND", "APPR", "RJCT")
	 * @param requestType Optional request type filter (e.g., "ACTI", "CDCL")
	 * @param search Optional search query for card number
	 * @return CSV file content as byte array
	 * @throws ReportGenerationException if CSV generation fails
	 */
	public byte[] generateCardRequestReportCSV(String status, String requestType, String search) {
		try {
			log.info("Starting Card Request Report CSV generation with filters - status: {}, requestType: {}, search: {}",
					status, requestType, search);

			// Fetch card request data with optional filters
			List<CardRequestReportDTO> requests = cardRequestReportRepository.findCardRequestsForReportWithFilters(
					status, requestType, search);

			// Process each request to decrypt and mask card numbers
			for (CardRequestReportDTO request : requests) {
				String plainCardNumber = cardEncryptionUtil.decryptCardNumberFromDatabase(request.getEncryptedCardNumber());
				String maskedCardNumber = maskCardNumber(plainCardNumber);
				request.setMaskedCardNumber(maskedCardNumber);
			}

			log.info("Found {} card requests for report", requests.size());

		// Create CSV in memory
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);

		// Add report title
		String reportTitle = buildCardRequestReportTitle(status);
		csvWriter.writeNext(new String[] { reportTitle });
		csvWriter.writeNext(new String[] { "Generated on: " + LocalDateTime.now().format(DATETIME_FORMATTER) });
		csvWriter.writeNext(new String[] { "" }); // Empty line

		// Add filter summary
		csvWriter.writeNext(new String[] { "Applied Filters:" });
		csvWriter.writeNext(new String[] { "Status", getRequestStatusDescription(status) });
		csvWriter.writeNext(new String[] { "Request Type", getRequestTypeDescription(requestType) });
		csvWriter.writeNext(new String[] { "Search Query", search != null && !search.trim().isEmpty() ? search : "None" });
		csvWriter.writeNext(new String[] { "" }); // Empty line

		// Add header row (without Encrypted Card Number)
		String[] header = {
				"Request ID",
				"Masked Card Number",
				"Request Type",
				"Request Status",
				"Reason",
				"Requested At",
				"Processed At",
				"Requested User Name",
				"Requested User Full Name",
				"Approved User Name",
				"Approved User Full Name"
		};
		csvWriter.writeNext(header);

			// Add data rows (without Encrypted Card Number)
			for (CardRequestReportDTO request : requests) {
				String[] row = {
						String.valueOf(request.getRequestId()),
						request.getMaskedCardNumber() != null ? request.getMaskedCardNumber() : "",
						request.getRequestTypeDescription() != null ? request.getRequestTypeDescription() : "",
						request.getRequestStatusDescription() != null ? request.getRequestStatusDescription() : "",
						request.getReason() != null ? request.getReason() : "",
						request.getRequestedAt() != null ? request.getRequestedAt().format(DATETIME_FORMATTER) : "",
						request.getProcessedAt() != null ? request.getProcessedAt().format(DATETIME_FORMATTER) : "",
						request.getRequestedUserName() != null ? request.getRequestedUserName() : "",
						request.getRequestedUserFullName() != null ? request.getRequestedUserFullName() : "",
						request.getApprovedUserName() != null ? request.getApprovedUserName() : "",
						request.getApprovedUserFullName() != null ? request.getApprovedUserFullName() : ""
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();

			log.info("Card Request Report CSV generated successfully with {} requests", requests.size());
			return stringWriter.toString().getBytes();

		} catch (Exception e) {
			log.error("Failed to generate Card Request Report CSV", e);
			throw new ReportGenerationException("Card Request Report CSV generation", "Failed to generate CSV report", e);
		}
	}
}
