package com.wheelGo.mapper;

import com.wheelGo.model.audit_logs.AuditLog;
import com.wheelGo.model.audit_logs.AuditLogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse (AuditLog auditLog);
}
