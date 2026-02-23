package com.epic.cms.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CardRequest")
public class CardRequest {

	@Id
	@Column("RequestID")
	private Long requestId;

	@Column("CardNumber")
	private String cardNumber;

	@Column("RequestTypeCode")
	private String requestTypeCode;

	@Column("RequestStatusCode")
	private String requestStatusCode;

	@Column("Reason")
	private String reason;

	@Column("RequestedAt")
	private LocalDateTime requestedAt;

	@Column("ProcessedAt")
	private LocalDateTime processedAt;

	@Column("RequestedUser")
	private String requestedUser;

	@Column("ApprovedUser")
	private String approvedUser;
}
