package com.wheelGo.mapper;


import com.wheelGo.model.userProfiles.UserProfile;
import com.wheelGo.model.userProfiles.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper extends BaseMapper<UserProfileResponse, UserProfile> {
}
