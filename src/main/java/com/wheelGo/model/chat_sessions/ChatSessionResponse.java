package com.wheelGo.model.chat_sessions;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class ChatSessionResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
