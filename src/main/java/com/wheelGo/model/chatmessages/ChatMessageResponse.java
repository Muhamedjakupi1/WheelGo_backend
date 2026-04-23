package com.wheelGo.model.chatmessages;

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
}