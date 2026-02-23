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
@Table(name = "User")
public class User {

	@Id
	@Column("UserName")
	private String userName;

	@Column("Status")
	private String status;

	@Column("Name")
	private String name;

	@Column("Description")
	private String description;

	@Column("CreatedAt")
	private LocalDateTime createdAt;

	@Column("LastUpdateTime")
	private LocalDateTime lastUpdateTime;
}
