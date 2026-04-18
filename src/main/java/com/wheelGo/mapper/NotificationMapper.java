package com.wheelGo.mapper;

import com.wheelGo.model.notifications.Notification;
import com.wheelGo.model.notifications.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper extends BaseMapper<NotificationResponse, Notification> {
}
