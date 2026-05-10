package com.wheelGo.controller;

import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.model.user_profiles.UserProfileUpdateRequest;
import com.wheelGo.service.UserProfileService;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/user-profile")
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

}
