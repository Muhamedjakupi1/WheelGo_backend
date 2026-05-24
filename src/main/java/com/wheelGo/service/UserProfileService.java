package com.wheelGo.service;

import com.wheelGo.mapper.UserProfileMapper;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.model.user_profiles.UserProfileRequest;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.model.user_profiles.UserProfileUpdateRequest;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.ReviewRepository;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final UserProfileMapper userProfileMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public UserProfileResponse createProfile(UUID userId, UserProfileRequest request) {
        User user = findCurrentUser(userId);

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
        return enrichProfileResponse(userProfileMapper.toResponse(savedProfile), user);
    }

    @Transactional
    public UserProfileResponse getProfileByUserId(UUID userId) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId).orElse(null);
        User user;

        if (profile != null) {
            user = profile.getUser() != null ? profile.getUser() : findCurrentUser(userId);
        } else {
            user = findCurrentUser(userId);
            profile = createDefaultProfile(user);
        }

        return enrichProfileResponse(userProfileMapper.toResponse(profile), user);
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
        User user = findCurrentUser(userId);
        return enrichProfileResponse(userProfileMapper.toResponse(updatedProfile), user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> createDefaultProfile(findCurrentUser(userId)));

        String avatarUrl = fileStorageService.storeProfileAvatar(file);
        profile.setAvatarUrl(avatarUrl);
        profile.setUpdatedAt(LocalDateTime.now());

        UserProfile updatedProfile = userProfileRepository.save(profile);
        User user = findCurrentUser(userId);
        return enrichProfileResponse(userProfileMapper.toResponse(updatedProfile), user);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserProfile createDefaultProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("User");
        profile.setLastName("User");

        return userProfileRepository.save(profile);
    }

    private UserProfileResponse enrichProfileResponse(UserProfileResponse response, User user) {
        response.setTotalRides(bookingRepository.countByUserIdAndStatus(user.getId(), BookingStatus.CONFIRMED));
        response.setMemberSince(user.getCreatedAt());
        response.setAverageRating(reviewRepository.findAverageRatingByUserId(user.getId()));
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

    @Transactional
    public void deleteProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        userProfileRepository.delete(profile);
    }
}
