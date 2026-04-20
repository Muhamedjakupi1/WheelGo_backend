package com.wheelGo.model.enums;

@PgEnumType(value = "ticket_status", scope = PgEnumScope.TENANT)
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
