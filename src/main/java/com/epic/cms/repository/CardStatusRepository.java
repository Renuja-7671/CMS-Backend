package com.epic.cms.repository;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epic.cms.model.CardStatus;

@Repository
public interface CardStatusRepository extends CrudRepository<CardStatus, String> {

	/**
	 * Find card status by status code.
	 */
	@Query("""
			SELECT StatusCode, Description
			FROM CardStatus
			WHERE StatusCode = :statusCode
			""")
	Optional<CardStatus> findByStatusCode(@Param("statusCode") String statusCode);
}
