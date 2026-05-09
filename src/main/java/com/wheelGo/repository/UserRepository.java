package com.wheelGo.repository;

import com.wheelGo.model.user.User;
import com.wheelGo.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantIdAndIdNot(String email, UUID tenantId, UUID id);
    List<User> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<User> findFirstByRoleAndIsActiveTrueOrderByCreatedAtAsc(Role role);
    Optional<User> findFirstByTenantIdAndRoleAndIsActiveTrueOrderByCreatedAtAsc(UUID tenantId, Role role);
    Optional<User> findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtAsc(UUID tenantId);
    Optional<User> findFirstByIsActiveTrueOrderByCreatedAtAsc();
}
