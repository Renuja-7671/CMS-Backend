package com.epic.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epic.cms.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

	/**
	 * Find all users ordered by last update time.
	 */
	@Query("""
			SELECT 
				u.UserName,
				u.Status,
				u.Name,
				u.Description,
				u.CreatedAt,
				u.LastUpdateTime
			FROM User u
			ORDER BY u.LastUpdateTime DESC
			""")
	List<User> findAllUsers();

	/**
	 * Find a user by username.
	 */
	@Query("""
			SELECT 
				u.UserName,
				u.Status,
				u.Name,
				u.Description,
				u.CreatedAt,
				u.LastUpdateTime
			FROM User u
			WHERE u.UserName = :userName
			""")
	Optional<User> findByUserName(@Param("userName") String userName);

	/**
	 * Find all users by status.
	 */
	@Query("""
			SELECT 
				u.UserName,
				u.Status,
				u.Name,
				u.Description,
				u.CreatedAt,
				u.LastUpdateTime
			FROM User u
			WHERE u.Status = :status
			ORDER BY u.LastUpdateTime DESC
			""")
	List<User> findByStatus(@Param("status") String status);

	/**
	 * Check if a user exists by username.
	 */
	@Query("""
			SELECT COUNT(*) > 0
			FROM User u
			WHERE u.UserName = :userName
			""")
	boolean existsByUserName(@Param("userName") String userName);

	/**
	 * Update user status.
	 */
	@Modifying
	@Query("""
			UPDATE User
			SET Status = :status,
			    LastUpdateTime = CURRENT_TIMESTAMP
			WHERE UserName = :userName
			""")
	int updateStatus(@Param("userName") String userName, @Param("status") String status);

	/**
	 * Update user information.
	 */
	@Modifying
	@Query("""
			UPDATE User
			SET Name = :name,
			    Description = :description,
			    LastUpdateTime = CURRENT_TIMESTAMP
			WHERE UserName = :userName
			""")
	int updateUser(@Param("userName") String userName, 
	               @Param("name") String name, 
	               @Param("description") String description);

	/**
	 * Count users by status.
	 */
	@Query("""
			SELECT COUNT(*)
			FROM User u
			WHERE u.Status = :status
			""")
	long countByStatus(@Param("status") String status);
}
