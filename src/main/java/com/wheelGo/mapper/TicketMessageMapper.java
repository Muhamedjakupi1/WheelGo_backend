package com.wheelGo.mapper;

import com.wheelGo.model.ticket_messages.TicketMessage;
import com.wheelGo.model.ticket_messages.TicketMessageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMessageMapper extends BaseMapper<TicketMessageResponse, TicketMessage> {
}
