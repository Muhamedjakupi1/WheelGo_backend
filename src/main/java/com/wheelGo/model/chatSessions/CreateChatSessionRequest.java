package com.wheelGo.model.chatSessions;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateChatSessionRequest {
    @Size(max = 150, message = "Title is too long")
    private String title;
}
