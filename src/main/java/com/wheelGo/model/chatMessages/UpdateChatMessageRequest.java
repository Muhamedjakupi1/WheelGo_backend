package com.wheelGo.model.chatMessages;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateChatMessageRequest {
    @NotBlank (message = "Content cannot be empty")
    private String content;

    private Integer tokensUsed;
}
