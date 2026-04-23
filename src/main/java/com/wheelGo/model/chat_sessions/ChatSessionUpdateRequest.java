package com.wheelGo.model.chat_sessions;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ChatSessionUpdateRequest {
    @Size(max = 150, message = "Title is too long")
    private String title;

    private LocalDateTime endedAt;
}
