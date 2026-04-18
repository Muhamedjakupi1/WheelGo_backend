package com.wheelGo.model.ticketMessages;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateTicketMessageRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
}
