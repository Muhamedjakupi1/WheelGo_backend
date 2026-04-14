package com.wheelGo.model.chatSessions;

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

    public static ChatSessionResponse from(ChatSession session) {
        ChatSessionResponse res = new ChatSessionResponse();
        res.setId(session.getId());
        res.setUserId(session.getUserId());
        res.setTitle(session.getTitle());
        res.setStartedAt(session.getStartedAt());
        res.setEndedAt(session.getEndedAt());
        return res;
    }
}
