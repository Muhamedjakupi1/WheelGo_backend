package com.wheelGo.repository;

import com.wheelGo.model.user.User;
import com.wheelGo.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.tenant.id = :tenantId AND u.role = :role AND u.isActive = true ORDER BY u.createdAt ASC LIMIT 1")
    Optional<User> findFirstByTenantIdAndRoleAndIsActiveTrueOrderByCreatedAtAsc(UUID tenantId, Role role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.tenant.id = :tenantId AND u.isActive = true ORDER BY u.createdAt ASC LIMIT 1")
    Optional<User> findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtAsc(UUID tenantId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tenant WHERE u.isActive = true ORDER BY u.createdAt ASC LIMIT 1")
    Optional<User> findFirstByIsActiveTrueOrderByCreatedAtAsc();
    boolean existsByEmail(String email);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantIdAndIdNot(String email, UUID tenantId, UUID id);
    List<User> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<User> findFirstByRoleAndIsActiveTrueOrderByCreatedAtAsc(Role role);
}
