package com.wheelGo.model.enums;

@PgEnumType(value = "notification_channel", scope = PgEnumScope.TENANT)
public enum NotificationChannel {
    EMAIL,
    PUSH,
    SMS
}
