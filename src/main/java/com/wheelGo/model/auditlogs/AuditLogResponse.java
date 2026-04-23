package com.wheelGo.model.auditlogs;

import com.wheelGo.model.enums.AuditAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private AuditAction action;
    private String entityType;
    private UUID entityId;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
