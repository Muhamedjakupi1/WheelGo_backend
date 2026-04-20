package com.wheelGo.model.enums;

@PgEnumType(value = "ticket_priority", scope = PgEnumScope.TENANT)
public enum TicketPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
