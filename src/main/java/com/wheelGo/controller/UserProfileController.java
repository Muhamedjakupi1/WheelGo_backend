package com.wheelGo.controller;

import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.model.user_profiles.UserProfileUpdateRequest;
import com.wheelGo.service.UserProfileService;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(){
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getProfileByUserId(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@RequestBody UserProfileUpdateRequest request){
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadMyAvatar(@RequestParam MultipartFile file) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.uploadAvatar(userId, file));
    }

}
