package com.wheelGo.mapper;

import com.wheelGo.model.user.User;
import com.wheelGo.model.user.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<UserResponse, User> {
}
