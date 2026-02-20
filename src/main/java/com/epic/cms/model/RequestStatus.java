package com.epic.cms.model;

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
@Table(name = "RequestStatus")
public class RequestStatus {

	@Id
	@Column("StatusCode")
	private String statusCode;

	@Column("Description")
	private String description;
}
