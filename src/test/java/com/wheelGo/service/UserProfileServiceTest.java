package com.wheelGo.service;

import com.wheelGo.mapper.UserProfileMapper;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.model.user_profiles.UserProfileRequest;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.model.user_profiles.UserProfileUpdateRequest;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private UserProfileService userProfileService;

    private UUID userId;
    private User user;
    private UserProfile profile;
    private UserProfileResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        user = new User();
        user.setId(userId);
        user.setTenant(tenant);

        profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setFirstName("John");
        profile.setLastName("Doe");

        response = new UserProfileResponse();
        response.setId(profile.getId());
        response.setFirstName("John");

        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId,
                "user@example.com",
                "hash",
                "v1",
                "USER",
                tenant.getId(),
                "tenant",
                false,
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_create_profile_when_request_valid() {
        UserProfileRequest request = new UserProfileRequest();
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.findByIdAndTenantId(userId, user.getTenant().getId())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        UserProfileResponse result = userProfileService.createProfile(userId, request);

        assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void should_throw_not_found_when_creating_profile_for_missing_user() {
        when(userRepository.findByIdAndTenantId(userId, user.getTenant().getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.createProfile(userId, new UserProfileRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void should_throw_bad_request_when_profile_already_exists() {
        when(userRepository.findByIdAndTenantId(userId, user.getTenant().getId())).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> userProfileService.createProfile(userId, new UserProfileRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User profile already exists");
    }

    @Test
    void should_return_existing_profile_when_get_profile_by_user_id_found() {
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        UserProfileResponse result = userProfileService.getProfileByUserId(userId);

        assertThat(result.getId()).isEqualTo(profile.getId());
    }

    @Test
    void should_create_default_profile_when_get_profile_by_user_id_missing() {
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(userRepository.findByIdAndTenantId(userId, user.getTenant().getId())).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        UserProfileResponse result = userProfileService.getProfileByUserId(userId);

        assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void should_update_profile_when_request_valid() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setPhone(" 123 ");
        request.setCity("  ");

        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        userProfileService.updateProfile(userId, request);

        assertThat(profile.getPhone()).isEqualTo("123");
        assertThat(profile.getCity()).isNull();
    }

    @Test
    void should_throw_not_found_when_updating_missing_profile() {
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateProfile(userId, new UserProfileUpdateRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User profile not found");
    }

    @Test
    void should_upload_avatar_when_profile_exists() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));
        when(fileStorageService.storeProfileAvatar(file)).thenReturn("/uploads/avatar.png");
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        UserProfileResponse result = userProfileService.uploadAvatar(userId, file);

        assertThat(profile.getAvatarUrl()).isEqualTo("/uploads/avatar.png");
        assertThat(result.getId()).isEqualTo(profile.getId());
    }

    @Test
    void should_delete_profile_when_found() {
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(profile));

        userProfileService.deleteProfile(userId);

        verify(userProfileRepository).delete(profile);
    }

    @Test
    void should_throw_not_found_when_deleting_missing_profile() {
        when(userProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.deleteProfile(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User profile not found");
    }
}
