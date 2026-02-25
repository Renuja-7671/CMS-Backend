package com.epic.cms.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.epic.cms.dto.CardReportDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom repository for generating card reports with proper DTO mapping.
 * Uses JdbcTemplate since Spring Data JDBC @Query doesn't support DTO projections well.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CardReportRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	/**
	 * RowMapper for CardReportDTO.
	 */
	private static final RowMapper<CardReportDTO> CARD_REPORT_ROW_MAPPER = new RowMapper<CardReportDTO>() {
		@Override
		public CardReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
			return CardReportDTO.builder()
					.encryptedCardNumber(rs.getString("encryptedCardNumber"))
					.expiryDate(rs.getDate("expiryDate") != null ? rs.getDate("expiryDate").toLocalDate() : null)
					.statusCode(rs.getString("statusCode"))
					.statusDescription(rs.getString("statusDescription"))
					.creditLimit(rs.getDouble("creditLimit"))
					.cashLimit(rs.getDouble("cashLimit"))
					.availableCreditLimit(rs.getDouble("availableCreditLimit"))
					.availableCashLimit(rs.getDouble("availableCashLimit"))
					.lastUpdatedTime(rs.getTimestamp("lastUpdatedTime") != null 
							? rs.getTimestamp("lastUpdatedTime").toLocalDateTime() : null)
					.lastUpdatedUserName(rs.getString("lastUpdatedUserName"))
					.lastUpdatedUserFullName(rs.getString("lastUpdatedUserFullName"))
					.build();
		}
	};

	/**
	 * Find all cards for report generation.
	 */
	public List<CardReportDTO> findAllCardsForReport() {
		String sql = """
				SELECT 
					c.CardNumber AS encryptedCardNumber,
					c.ExpiryDate AS expiryDate,
					c.CardStatus AS statusCode,
					s.Description AS statusDescription,
					c.CreditLimit AS creditLimit,
					c.CashLimit AS cashLimit,
					c.AvailableCreditLimit AS availableCreditLimit,
					c.AvailableCashLimit AS availableCashLimit,
					c.LastUpdateTime AS lastUpdatedTime,
					c.LastUpdatedUser AS lastUpdatedUserName,
					u.Name AS lastUpdatedUserFullName
				FROM Card c
				LEFT JOIN CardStatus s ON c.CardStatus = s.StatusCode
				LEFT JOIN User u ON c.LastUpdatedUser = u.UserName
				ORDER BY c.LastUpdateTime DESC
				""";

		log.debug("Executing findAllCardsForReport query");
		return jdbcTemplate.query(sql, CARD_REPORT_ROW_MAPPER);
	}

	/**
	 * Find cards for report with filters.
	 */
	public List<CardReportDTO> findCardsForReportWithFilters(
			String status, String searchQuery, 
			String expiryDateFrom, String expiryDateTo,
			String creditLimitMin, String creditLimitMax, 
			String cashLimitMin, String cashLimitMax) {

		StringBuilder sql = new StringBuilder("""
				SELECT 
					c.CardNumber AS encryptedCardNumber,
					c.ExpiryDate AS expiryDate,
					c.CardStatus AS statusCode,
					s.Description AS statusDescription,
					c.CreditLimit AS creditLimit,
					c.CashLimit AS cashLimit,
					c.AvailableCreditLimit AS availableCreditLimit,
					c.AvailableCashLimit AS availableCashLimit,
					c.LastUpdateTime AS lastUpdatedTime,
					c.LastUpdatedUser AS lastUpdatedUserName,
					u.Name AS lastUpdatedUserFullName
				FROM Card c
				LEFT JOIN CardStatus s ON c.CardStatus = s.StatusCode
				LEFT JOIN User u ON c.LastUpdatedUser = u.UserName
				WHERE 1=1
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();

		// Add filter conditions dynamically
		if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			conditions.add("c.CardStatus = :status");
			params.addValue("status", status);
		}

		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			conditions.add("c.CardNumber LIKE CONCAT('%', :searchQuery, '%')");
			params.addValue("searchQuery", searchQuery);
		}

		if (expiryDateFrom != null && !expiryDateFrom.trim().isEmpty()) {
			conditions.add("c.ExpiryDate >= :expiryDateFrom");
			params.addValue("expiryDateFrom", expiryDateFrom);
		}

		if (expiryDateTo != null && !expiryDateTo.trim().isEmpty()) {
			conditions.add("c.ExpiryDate <= :expiryDateTo");
			params.addValue("expiryDateTo", expiryDateTo);
		}

		if (creditLimitMin != null && !creditLimitMin.trim().isEmpty()) {
			conditions.add("c.CreditLimit >= :creditLimitMin");
			params.addValue("creditLimitMin", creditLimitMin);
		}

		if (creditLimitMax != null && !creditLimitMax.trim().isEmpty()) {
			conditions.add("c.CreditLimit <= :creditLimitMax");
			params.addValue("creditLimitMax", creditLimitMax);
		}

		if (cashLimitMin != null && !cashLimitMin.trim().isEmpty()) {
			conditions.add("c.CashLimit >= :cashLimitMin");
			params.addValue("cashLimitMin", cashLimitMin);
		}

		if (cashLimitMax != null && !cashLimitMax.trim().isEmpty()) {
			conditions.add("c.CashLimit <= :cashLimitMax");
			params.addValue("cashLimitMax", cashLimitMax);
		}

		// Append conditions to SQL
		if (!conditions.isEmpty()) {
			sql.append(" AND ").append(String.join(" AND ", conditions));
		}

		sql.append(" ORDER BY c.LastUpdateTime DESC");

		log.debug("Executing findCardsForReportWithFilters with params: {}", params.getValues());
		return jdbcTemplate.query(sql.toString(), params, CARD_REPORT_ROW_MAPPER);
	}
}
