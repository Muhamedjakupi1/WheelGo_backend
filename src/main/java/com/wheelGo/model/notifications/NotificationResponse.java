package com.wheelGo.model.notifications;

import com.wheelGo.model.enums.NotificationChannel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String body;
    private NotificationChannel channel;
    private boolean isRead;
    private LocalDateTime sentAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setChannel(notification.getChannel());
        response.setRead(notification.isRead());
        response.setSentAt(notification.getSentAt());
        return response;
    }
}
