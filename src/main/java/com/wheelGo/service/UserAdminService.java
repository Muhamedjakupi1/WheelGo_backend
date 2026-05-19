package com.wheelGo.service;

import com.wheelGo.model.enums.Role;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user.UserResponse;
import com.wheelGo.model.user.UserUpdateRequest;
import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleResponse;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccessService adminAccessService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        return userRepository.findAllByTenantIdAndRoleOrderByCreatedAtDesc(tenantId, Role.USER)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        return toResponse(findUser(id, tenantId));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        CustomUserPrincipal principal = adminAccessService.requireCurrentPrincipal();
        User user = findUser(id, tenantId);

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndTenantIdAndIdNot(normalizedEmail, tenantId, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A user with this email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!PasswordPolicy.isValid(request.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            validateRoleChange(principal, user, request.getRole());
            user.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            if (principal.getUserId().equals(user.getId()) && !request.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
            }
            user.setActive(request.getIsActive());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        CustomUserPrincipal principal = adminAccessService.requireCurrentPrincipal();
        User user = findUser(id, tenantId);

        if (principal.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        entityManager.createNativeQuery("DELETE FROM ticket_messages WHERE sender_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("""
                UPDATE addons addon
                SET quantity = addon.quantity + reserved.total_quantity,
                    updated_at = NOW()
                FROM (
                    SELECT ba.addon_id, SUM(ba.quantity) AS total_quantity
                    FROM booking_addons ba
                    JOIN bookings b ON b.id = ba.booking_id
                    JOIN addons managed_addon ON managed_addon.id = ba.addon_id
                    WHERE b.user_id = :userId
                      AND b.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
                      AND LOWER(managed_addon.name) IN ('baby seat', 'bluetooth')
                    GROUP BY ba.addon_id
                ) reserved
                WHERE addon.id = reserved.addon_id
                """)
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("""
                DELETE FROM ticket_messages
                WHERE ticket_id IN (
                    SELECT id FROM support_tickets WHERE user_id = :userId
                    UNION
                    SELECT id FROM support_tickets WHERE booking_id IN (
                        SELECT id FROM bookings WHERE user_id = :userId
                    )
                )
                """)
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("""
                DELETE FROM support_tickets
                WHERE user_id = :userId
                   OR booking_id IN (SELECT id FROM bookings WHERE user_id = :userId)
                """)
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reviews WHERE user_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM chat_sessions WHERE user_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM audit_logs WHERE user_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM bookings WHERE user_id = :userId")
                .setParameter("userId", id)
                .executeUpdate();
        userRepository.delete(user);
    }

    private void validateRoleChange(CustomUserPrincipal principal, User targetUser, Role newRole) {
        if (principal.getUserId().equals(targetUser.getId())
                && targetUser.getRole() == Role.ADMIN
                && newRole == Role.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot demote your own admin account");
        }

        if (!"SUPER_ADMIN".equals(principal.getRole()) && newRole == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only super admins can assign the SUPER_ADMIN role");
        }
    }

    private User findUser(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setEmailVerified(user.isEmailVerified());
        response.setTenantId(user.getTenant() != null ? user.getTenant().getId() : null);
        response.setImpersonate(user.isImpersonate());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private List<UserResponse> toResponses(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> searchUser(String keyword) {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        List<User> users = userRepository.searchUser(tenantId, Role.USER, keyword.trim());
        return toResponses(users);
    }

}
