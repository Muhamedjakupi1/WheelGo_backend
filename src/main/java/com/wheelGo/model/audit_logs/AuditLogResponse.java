package com.wheelGo.model.audit_logs;

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

    public static AuditLogResponse from(AuditLog log) {
        AuditLogResponse res = new AuditLogResponse();
        res.setId(log.getId());
        res.setUserId(log.getUserId());
        res.setAction(log.getAction());
        res.setEntityType(log.getEntityType());
        res.setEntityId(log.getEntityId());
        res.setOldValues(log.getOldValues());
        res.setNewValues(log.getNewValues());
        res.setIpAddress(log.getIpAddress());
        res.setUserAgent(log.getUserAgent());
        res.setCreatedAt(log.getCreatedAt());
        return res;
    }
}
