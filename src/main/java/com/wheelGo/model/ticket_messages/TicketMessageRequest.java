package com.wheelGo.model.ticket_messages;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class TicketMessageRequest {

    private UUID ticketId;

    private UUID senderId;

    @NotBlank (message = "Message content cannot be empty ")
    private String message;

    private boolean isStaff;
}
