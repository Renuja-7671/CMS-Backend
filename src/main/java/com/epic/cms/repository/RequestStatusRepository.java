package com.epic.cms.repository;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epic.cms.model.RequestStatus;

@Repository
public interface RequestStatusRepository extends CrudRepository<RequestStatus, String> {
	
	@Query("SELECT Description FROM RequestStatus WHERE StatusCode = :code")
	Optional<String> findDescriptionByCode(@Param("code") String code);
}
