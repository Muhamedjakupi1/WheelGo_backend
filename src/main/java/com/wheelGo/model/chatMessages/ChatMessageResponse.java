package com.wheelGo.model.chatMessages;

import com.wheelGo.model.enums.ChatRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class ChatMessageResponse {
    private UUID id;
    private UUID sessionId;
    private ChatRole role;
    private String content;
    private Integer tokensUsed;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage m) {
        ChatMessageResponse res = new ChatMessageResponse();
        res.setId(m.getId());
        res.setSessionId(m.getSessionId());
        res.setRole(m.getRole());
        res.setContent(m.getContent());
        res.setTokensUsed(m.getTokensUsed());
        res.setCreatedAt(m.getCreatedAt());
        return res;
    }
}