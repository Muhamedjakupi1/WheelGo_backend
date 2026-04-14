package com.wheelGo.model.supportTickets;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class CreateSupportTicketRequest {

    private UUID bookingId;
    @NotBlank (message = "Subject is required")
    @Size (max = 150, message = "Subject cannot exceed 150 characters")
    private String subject;

    private String priority;
}

