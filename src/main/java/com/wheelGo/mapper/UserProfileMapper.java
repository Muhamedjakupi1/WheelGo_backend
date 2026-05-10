package com.wheelGo.mapper;


import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper extends BaseMapper<UserProfileResponse, UserProfile> {
    @Override
    @Mapping(target = "userId", source = "user.id")
    UserProfileResponse toResponse(UserProfile entity);
}
