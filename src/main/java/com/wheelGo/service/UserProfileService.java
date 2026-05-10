package com.wheelGo.service;

import com.wheelGo.mapper.UserProfileMapper;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.model.user_profiles.UserProfileRequest;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.model.user_profiles.UserProfileUpdateRequest;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional
    public UserProfileResponse createProfile(UUID userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (userProfileRepository.findByUser_Id(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User profile already exists");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setCountry(request.getCountry());

        UserProfile savedProfile = userProfileRepository.save(profile);
        return userProfileMapper.toResponse(savedProfile);
    }

    @Transactional
    public UserProfileResponse getProfileByUserId(UUID userId) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        return userProfileMapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(normalizeOptionalText(request.getPhone()));
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(normalizeOptionalText(request.getAvatarUrl()));
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) {
            profile.setAddress(normalizeOptionalText(request.getAddress()));
        }
        if (request.getCity() != null) {
            profile.setCity(normalizeOptionalText(request.getCity()));
        }
        if (request.getCountry() != null) {
            profile.setCountry(normalizeOptionalText(request.getCountry()));
        }

        profile.setUpdatedAt(LocalDateTime.now());

        UserProfile updatedProfile = userProfileRepository.save(profile);
        return userProfileMapper.toResponse(updatedProfile);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserProfile createDefaultProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("User");
        profile.setLastName("User");

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        userProfileRepository.delete(profile);
    }
}
