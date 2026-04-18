package com.wheelGo.mapper;

import com.wheelGo.model.supportTickets.SupportTicket;
import com.wheelGo.model.supportTickets.SupportTicketResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportTicketMapper extends BaseMapper<SupportTicketResponse, SupportTicket> {
}
