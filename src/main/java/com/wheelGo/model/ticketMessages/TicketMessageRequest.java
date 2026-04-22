package com.wheelGo.model.ticketMessages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class TicketMessageRequest {

    @NotNull(message = "Ticket ID is required")
    private UUID ticketId;

    @NotNull (message = "Sender ID is required")
    private UUID senderId;

    @NotBlank (message = "Message content cannot be empty ")
    private String message;

    private boolean isStaff;
}
