package com.wheelGo.controller;

import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateRequest;
import com.wheelGo.service.UserSettingsService;
import com.wheelGo.tools.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/user-settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid UserSettingsPasswordUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        userSettingsService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
