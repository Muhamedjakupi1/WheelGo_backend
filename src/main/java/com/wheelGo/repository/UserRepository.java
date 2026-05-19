package com.wheelGo.repository;

import com.wheelGo.model.user.User;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    List<User> findAllByTenantIdAndRoleOrderByCreatedAtDesc(UUID tenantId, Role role);
    Optional<User> findFirstByRoleAndIsActiveTrueOrderByCreatedAtAsc(Role role);

    @Query("SELECT u from User u WHERE u.tenant.id = :tenantId AND u.role = :role AND ("+
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(u.role AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY u.createdAt DESC")
    List<User> searchUser(UUID tenantId, Role role, String keyword);
}
