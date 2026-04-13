package com.wheelGo.model.notifications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateNotificationRequest {

    @NotBlank(message = "Notification type is required")
    private String type;

    @NotBlank (message = "Title is required")
    private String title;

    @NotBlank (message = "Body is required")
    private String body;

    private String channel = "EMAIL";

}
