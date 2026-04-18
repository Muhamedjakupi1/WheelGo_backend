package com.wheelGo.model.supportTickets;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateSupportTicketRequest {
    private String subject;

    private String status;

    private String priority;
}
