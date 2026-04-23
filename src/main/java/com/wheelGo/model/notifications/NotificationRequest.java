package com.wheelGo.model.notifications;

import com.wheelGo.model.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NotificationRequest {

    @NotBlank(message = "Notification type is required")
    private String type;

    @NotBlank (message = "Title is required")
    private String title;

    @NotBlank (message = "Body is required")
    private String body;

    private NotificationChannel channel = NotificationChannel.EMAIL;

}
