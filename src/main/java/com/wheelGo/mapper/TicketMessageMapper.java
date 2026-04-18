package com.wheelGo.mapper;

import com.wheelGo.model.ticketMessages.TicketMessage;
import com.wheelGo.model.ticketMessages.TicketMessageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMessageMapper extends BaseMapper<TicketMessageResponse, TicketMessage> {
}
