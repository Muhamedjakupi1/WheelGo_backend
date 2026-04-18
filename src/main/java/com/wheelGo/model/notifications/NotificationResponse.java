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
}
