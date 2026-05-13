package com.wheelGo.controller;

import com.wheelGo.model.audit_logs.AuditLogResponse;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for managing audit logs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<List<AuditLogResponse>> getAll() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    @GetMapping("/user")
    @Operation(summary = "Get audit logs by user email")
    public ResponseEntity<List<AuditLogResponse>> getByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByUserEmail(email));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit logs by action")
    public ResponseEntity<List<AuditLogResponse>> getByAction(
            @Parameter(
                    description = "Audit action",
                    schema = @Schema(allowableValues = {"CREATE", "LOGIN", "UPDATE"})
            )
            @PathVariable AuditAction action) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByAction(action));
    }
}
