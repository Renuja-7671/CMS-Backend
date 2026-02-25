package com.epic.cms.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.epic.cms.dto.CardRequestReportDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom repository for generating card request reports with proper DTO mapping.
 * Uses JdbcTemplate since Spring Data JDBC @Query doesn't support DTO projections well.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CardRequestReportRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	/**
	 * RowMapper for CardRequestReportDTO.
	 */
	private static final RowMapper<CardRequestReportDTO> CARD_REQUEST_REPORT_ROW_MAPPER = new RowMapper<CardRequestReportDTO>() {
		@Override
		public CardRequestReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			return CardRequestReportDTO.builder()
					.requestId(rs.getLong("requestId"))
					.encryptedCardNumber(rs.getString("encryptedCardNumber"))
					.requestTypeCode(rs.getString("requestTypeCode"))
					.requestTypeDescription(rs.getString("requestTypeDescription"))
					.requestStatusCode(rs.getString("requestStatusCode"))
					.requestStatusDescription(rs.getString("requestStatusDescription"))
					.reason(rs.getString("reason"))
					.requestedAt(rs.getTimestamp("requestedAt") != null 
							? rs.getTimestamp("requestedAt").toLocalDateTime() : null)
					.processedAt(rs.getTimestamp("processedAt") != null 
							? rs.getTimestamp("processedAt").toLocalDateTime() : null)
					.requestedUserName(rs.getString("requestedUserName"))
					.requestedUserFullName(rs.getString("requestedUserFullName"))
					.approvedUserName(rs.getString("approvedUserName"))
					.approvedUserFullName(rs.getString("approvedUserFullName"))
					.build();
		}
	};

	/**
	 * Find all card requests for report generation.
	 */
	public List<CardRequestReportDTO> findAllCardRequestsForReport() {
		String sql = """
				SELECT 
					cr.RequestID AS requestId,
					cr.CardNumber AS encryptedCardNumber,
					cr.RequestTypeCode AS requestTypeCode,
					crt.Description AS requestTypeDescription,
					cr.RequestStatusCode AS requestStatusCode,
					rs.Description AS requestStatusDescription,
					cr.Reason AS reason,
					cr.RequestedAt AS requestedAt,
					cr.ProcessedAt AS processedAt,
					cr.RequestedUser AS requestedUserName,
					u1.Name AS requestedUserFullName,
					cr.ApprovedUser AS approvedUserName,
					u2.Name AS approvedUserFullName
				FROM CardRequest cr
				LEFT JOIN CardRequestType crt ON cr.RequestTypeCode = crt.Code
				LEFT JOIN RequestStatus rs ON cr.RequestStatusCode = rs.StatusCode
				LEFT JOIN User u1 ON cr.RequestedUser = u1.UserName
				LEFT JOIN User u2 ON cr.ApprovedUser = u2.UserName
				ORDER BY cr.RequestedAt DESC
				""";

		log.debug("Executing findAllCardRequestsForReport query");
		return jdbcTemplate.query(sql, CARD_REQUEST_REPORT_ROW_MAPPER);
	}

	/**
	 * Find card requests for report with filters.
	 * Supports filtering by status, request type, and search query.
	 */
	public List<CardRequestReportDTO> findCardRequestsForReportWithFilters(
			String status, String requestType, String searchQuery) {

		StringBuilder sql = new StringBuilder("""
				SELECT 
					cr.RequestID AS requestId,
					cr.CardNumber AS encryptedCardNumber,
					cr.RequestTypeCode AS requestTypeCode,
					crt.Description AS requestTypeDescription,
					cr.RequestStatusCode AS requestStatusCode,
					rs.Description AS requestStatusDescription,
					cr.Reason AS reason,
					cr.RequestedAt AS requestedAt,
					cr.ProcessedAt AS processedAt,
					cr.RequestedUser AS requestedUserName,
					u1.Name AS requestedUserFullName,
					cr.ApprovedUser AS approvedUserName,
					u2.Name AS approvedUserFullName
				FROM CardRequest cr
				LEFT JOIN CardRequestType crt ON cr.RequestTypeCode = crt.Code
				LEFT JOIN RequestStatus rs ON cr.RequestStatusCode = rs.StatusCode
				LEFT JOIN User u1 ON cr.RequestedUser = u1.UserName
				LEFT JOIN User u2 ON cr.ApprovedUser = u2.UserName
				WHERE 1=1
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();

		// Add filter conditions dynamically
		if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			conditions.add("cr.RequestStatusCode = :status");
			params.addValue("status", status);
		}

		if (requestType != null && !requestType.trim().isEmpty() && !"ALL".equalsIgnoreCase(requestType)) {
			conditions.add("cr.RequestTypeCode = :requestType");
			params.addValue("requestType", requestType);
		}

		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			conditions.add("cr.CardNumber LIKE CONCAT('%', :searchQuery, '%')");
			params.addValue("searchQuery", searchQuery);
		}

		// Append conditions to SQL
		if (!conditions.isEmpty()) {
			sql.append(" AND ").append(String.join(" AND ", conditions));
		}

		sql.append(" ORDER BY cr.RequestedAt DESC");

		log.debug("Executing findCardRequestsForReportWithFilters with params: {}", params.getValues());
		return jdbcTemplate.query(sql.toString(), params, CARD_REQUEST_REPORT_ROW_MAPPER);
	}
}
