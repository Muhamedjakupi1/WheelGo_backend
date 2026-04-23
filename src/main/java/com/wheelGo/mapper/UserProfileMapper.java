package com.wheelGo.mapper;


import com.wheelGo.model.userprofiles.UserProfile;
import com.wheelGo.model.userprofiles.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper extends BaseMapper<UserProfileResponse, UserProfile> {
}
