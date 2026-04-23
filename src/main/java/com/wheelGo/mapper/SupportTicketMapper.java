package com.wheelGo.mapper;

import com.wheelGo.model.support_tickets.SupportTicket;
import com.wheelGo.model.support_tickets.SupportTicketResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketMapper extends BaseMapper<SupportTicketResponse, SupportTicket> {
}
