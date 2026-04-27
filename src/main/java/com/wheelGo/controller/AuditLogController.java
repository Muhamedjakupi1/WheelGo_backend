package com.wheelGo.controller;

import com.wheelGo.model.audit_logs.AuditLogRequest;
import com.wheelGo.model.audit_logs.AuditLogResponse;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for managing audit logs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    @Operation(summary = "Create audit log")
    public ResponseEntity<AuditLogResponse> create(@RequestBody @Valid AuditLogRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(auditLogService.createAuditLog(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by id")
    public ResponseEntity<AuditLogResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogService.getAuditLogById(id));
    }

    @GetMapping
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<List<AuditLogResponse>> getAll() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    @GetMapping("/entity")
    @Operation(summary = "Get audit logs by entity")
    public ResponseEntity<List<AuditLogResponse>> getByEntity(@RequestParam String entityType,
                                                              @RequestParam UUID entityId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByEntity(entityType, entityId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<List<AuditLogResponse>> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByUserId(userId));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action")
    public ResponseEntity<List<AuditLogResponse>> getByAction(@PathVariable AuditAction action) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByAction(action));
    }
}
