package com.wheelGo.model.chatMessages;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.UUID;

@Getter @Size
public class CreateChatMessageRequest {
    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotBlank(message = "Role is required")
    private String role; // USER, ASSISTANT, ose SYSTEM

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private Integer tokensUsed;
}
