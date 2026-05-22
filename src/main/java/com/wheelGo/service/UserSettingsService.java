package com.wheelGo.service;

import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_settings.UserSettings;
import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateRequest;
import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateResponse;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.UserSettingsRepository;
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
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserSettingsPasswordUpdateResponse getSettings(UUID userId) {
        User user = findCurrentUser(userId);

        return userSettingsRepository.findByUser_Id(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    UserSettingsPasswordUpdateResponse response = new UserSettingsPasswordUpdateResponse();
                    response.setUserId(user.getId());
                    response.setPasswordChanged(false);
                    response.setUpdatedAt(null);
                    return response;
                });
    }

    @Transactional
    public UserSettingsPasswordUpdateResponse changePassword(UUID userId, UserSettingsPasswordUpdateRequest request) {
        User user = findCurrentUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (!PasswordPolicy.isValid(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The new password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        UserSettings settings = userSettingsRepository.findByUser_Id(savedUser.getId())
                .orElseGet(() -> {
                    UserSettings newSettings = new UserSettings();
                    newSettings.setUser(savedUser);
                    return newSettings;
                });
        settings.setPassword(savedUser.getPasswordHash());
        settings.setPasswordChanged(true);
        UserSettings savedSettings = userSettingsRepository.save(settings);

        UserSettingsPasswordUpdateResponse response = toResponse(savedSettings);

        auditLogService.logForSchema(
                savedUser.getTenant().getSchemaName(),
                savedUser.getId(),
                AuditAction.UPDATE,
                "UserSettingsPassword",
                savedUser.getId(),
                null,
                response
        );

        return response;
    }

    @Transactional
    public void createInitialSettings(User user) {
        if (userSettingsRepository.findByUser_Id(user.getId()).isPresent()) {
            return;
        }

        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setPassword(user.getPasswordHash());
        settings.setPasswordChanged(false);
        userSettingsRepository.save(settings);
    }

    private UserSettingsPasswordUpdateResponse toResponse(UserSettings settings) {
        UserSettingsPasswordUpdateResponse response = new UserSettingsPasswordUpdateResponse();
        response.setUserId(settings.getUser().getId());
        response.setPasswordChanged(settings.isPasswordChanged());
        response.setUpdatedAt(settings.getUpdatedAt());
        return response;
    }

    private User findCurrentUser(UUID userId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated tenant context found");
        }

        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
