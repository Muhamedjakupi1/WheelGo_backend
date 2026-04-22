package com.wheelGo.model.supportTickets;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SupportTicketUpdateRequest {
    private String subject;

    private String status;

    private String priority;
}
