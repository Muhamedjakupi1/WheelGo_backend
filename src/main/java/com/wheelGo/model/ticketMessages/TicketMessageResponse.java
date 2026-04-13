package com.wheelGo.model.ticketMessages;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class TicketMessageResponse {
    private UUID id;
    private UUID ticketId;
    private UUID senderId;
    private String message;
    private boolean isStaff;
    private LocalDateTime sentAt;

    public static TicketMessageResponse from(TicketMessage ticketMessage) {
        TicketMessageResponse ticketMessageResponse = new TicketMessageResponse();
        ticketMessageResponse.setId(ticketMessage.getId());
        ticketMessageResponse.setTicketId(ticketMessage.getTicketId());
        ticketMessageResponse.setSenderId(ticketMessage.getSenderId());
        ticketMessageResponse.setMessage(ticketMessage.getMessage());
        ticketMessageResponse.setSentAt(ticketMessage.getSentAt());
        return ticketMessageResponse;
    }
}
