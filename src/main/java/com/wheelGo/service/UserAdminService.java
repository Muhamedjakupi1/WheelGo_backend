package com.wheelGo.service;

import com.wheelGo.model.enums.Role;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user.UserResponse;
import com.wheelGo.model.user.UserUpdateRequest;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccessService adminAccessService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        return userRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
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
}
