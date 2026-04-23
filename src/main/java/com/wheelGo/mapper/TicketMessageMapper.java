package com.wheelGo.mapper;

import com.wheelGo.model.ticketmessages.TicketMessage;
import com.wheelGo.model.ticketmessages.TicketMessageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMessageMapper extends BaseMapper<TicketMessageResponse, TicketMessage> {
}
