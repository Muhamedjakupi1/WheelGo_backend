package com.wheelGo.model.ticketmessages;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TicketMessageUpdateRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
}
