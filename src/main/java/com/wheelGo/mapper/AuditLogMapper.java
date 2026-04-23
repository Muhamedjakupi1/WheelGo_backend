package com.wheelGo.mapper;

import com.wheelGo.model.auditlogs.AuditLog;
import com.wheelGo.model.auditlogs.AuditLogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse (AuditLog auditLog);
}
