package com.wheelGo.service;

import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateRequest;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.tools.SecurityUtils;
import com.wheelGo.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public void changePassword(UUID userId, UserSettingsPasswordUpdateRequest request) {
        User user = findCurrentUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (!PasswordPolicy.isValid(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.logForSchema(
                user.getTenant().getSchemaName(),
                user.getId(),
                AuditAction.UPDATE,
                "UserSettingsPassword",
                user.getId(),
                null,
                new PasswordChangedSnapshot(true)
        );
    }

    private User findCurrentUser(UUID userId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated tenant context found");
        }

        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private record PasswordChangedSnapshot(Boolean passwordChanged) {}
}
