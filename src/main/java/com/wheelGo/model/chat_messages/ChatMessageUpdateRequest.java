package com.wheelGo.model.chat_messages;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChatMessageUpdateRequest {
    @NotBlank (message = "Content cannot be empty")
    private String content;

    private Integer tokensUsed;
}
