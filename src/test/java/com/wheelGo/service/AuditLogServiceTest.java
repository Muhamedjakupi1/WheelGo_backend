package com.wheelGo.service;

import com.wheelGo.mapper.AuditLogMapper;
import com.wheelGo.model.audit_logs.AuditLog;
import com.wheelGo.model.audit_logs.AuditLogRequest;
import com.wheelGo.model.audit_logs.AuditLogResponse;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.AuditLogRepository;
import com.wheelGo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminAccessService adminAccessService;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private EntityManager entityManager;
    @Mock private Query query;
    @InjectMocks private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditLogService, "entityManager", entityManager);
    }

    @Test
    void should_create_audit_log_when_request_valid() {
        AuditLogRequest request = new AuditLogRequest();
        request.setAction(AuditAction.CREATE);
        AuditLog auditLog = new AuditLog();
        AuditLogResponse response = new AuditLogResponse();
        response.setAction(AuditAction.CREATE);

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(response);

        AuditLogResponse result = auditLogService.createAuditLog(request);

        assertThat(result.getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void should_return_audit_logs_by_email_when_user_exists() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        AuditLog auditLog = new AuditLog();
        AuditLogResponse response = new AuditLogResponse();

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByEmailAndTenantId("user@example.com", tenantId)).thenReturn(Optional.of(user));
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(auditLog));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(response);

        List<AuditLogResponse> result = auditLogService.getAuditLogsByUserEmail(" User@Example.com ");

        assertThat(result).hasSize(1);
    }

    @Test
    void should_throw_bad_request_when_email_blank() {
        assertThatThrownBy(() -> auditLogService.getAuditLogsByUserEmail(" "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    void should_throw_illegal_argument_when_schema_invalid_in_log_for_schema() {
        assertThatThrownBy(() -> auditLogService.logForSchema("bad-schema", UUID.randomUUID(), AuditAction.CREATE, "User", UUID.randomUUID(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid schema name");
    }

    @Test
    void should_log_for_schema_when_schema_valid() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());
        when(auditLogMapper.toResponse(any())).thenReturn(new AuditLogResponse());

        auditLogService.logForSchema("tenant_one", UUID.randomUUID(), AuditAction.CREATE, "User", UUID.randomUUID(), null, null);

        verify(entityManager).createNativeQuery("SET LOCAL search_path TO \"tenant_one\", public");
    }
}
