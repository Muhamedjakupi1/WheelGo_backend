package com.wheelGo.mapper;


import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper extends BaseMapper<UserProfileResponse, UserProfile> {
}
