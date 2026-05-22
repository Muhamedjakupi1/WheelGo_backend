package com.wheelGo.controller;

import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateRequest;
import com.wheelGo.model.user_settings.UserSettingsPasswordUpdateResponse;
import com.wheelGo.service.UserSettingsService;
import com.wheelGo.tools.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/me")
    public ResponseEntity<UserSettingsPasswordUpdateResponse> getMySettings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userSettingsService.getSettings(userId));
    }

    @PutMapping("/me/password")
    public ResponseEntity<UserSettingsPasswordUpdateResponse> changePassword(@RequestBody @Valid UserSettingsPasswordUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.OK).body(userSettingsService.changePassword(userId, request));
    }
}
