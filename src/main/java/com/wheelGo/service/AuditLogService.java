package com.wheelGo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wheelGo.mapper.AuditLogMapper;
import com.wheelGo.model.audit_logs.AuditLog;
import com.wheelGo.model.audit_logs.AuditLogRequest;
import com.wheelGo.model.audit_logs.AuditLogResponse;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.repository.AuditLogRepository;
import com.wheelGo.tools.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public AuditLogResponse createAuditLog(AuditLogRequest request) {
        AuditLog auditLog = new AuditLog();
        applyRequest(auditLog, request);

        AuditLog savedAuditLog = auditLogRepository.save(auditLog);
        return auditLogMapper.toResponse(savedAuditLog);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Audit log not found"));

        return auditLogMapper.toResponse(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogRepository.findAll()
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByUserId(UUID userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByAction(AuditAction action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, UUID entityId, Object oldData, Object newData) {
        saveAuditLog(SecurityUtils.getCurrentUserId(), action, entityType, entityId, oldData, newData);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, AuditAction action, String entityType, UUID entityId, Object oldData, Object newData) {
        saveAuditLog(userId, action, entityType, entityId, oldData, newData);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logForSchema(String schemaName, AuditAction action, String entityType, UUID entityId, Object oldData, Object newData) {
        setLocalSearchPath(schemaName);
        saveAuditLog(SecurityUtils.getCurrentUserId(), action, entityType, entityId, oldData, newData);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logForSchema(String schemaName, UUID userId, AuditAction action, String entityType, UUID entityId, Object oldData, Object newData) {
        setLocalSearchPath(schemaName);
        saveAuditLog(userId, action, entityType, entityId, oldData, newData);
    }

    private void saveAuditLog(UUID userId, AuditAction action, String entityType, UUID entityId, Object oldData, Object newData) {
        AuditLogRequest request = new AuditLogRequest();
        request.setUserId(userId);
        request.setAction(action);
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setOldValues(toMap(oldData));
        request.setNewValues(toMap(newData));
        request.setIpAddress(resolveIpAddress());
        request.setUserAgent(resolveUserAgent());

        createAuditLog(request);
    }

    private void setLocalSearchPath(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        entityManager.createNativeQuery("SET LOCAL search_path TO \"" + schemaName + "\", public")
                .executeUpdate();
    }

    private void applyRequest(AuditLog auditLog, AuditLogRequest request) {
        auditLog.setUserId(request.getUserId());
        auditLog.setAction(request.getAction());
        auditLog.setEntityType(request.getEntityType());
        auditLog.setEntityId(request.getEntityId());
        auditLog.setOldValues(request.getOldValues());
        auditLog.setNewValues(request.getNewValues());
        auditLog.setIpAddress(request.getIpAddress());
        auditLog.setUserAgent(request.getUserAgent());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return null;
        }

        return objectMapper.convertValue(value, Map.class);
    }

    private String resolveIpAddress() {
        HttpServletRequest currentRequest = getCurrentRequest();
        if (currentRequest == null) return null;

        String xForwardedFor = currentRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0]; //
        }
        return currentRequest.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest currentRequest = getCurrentRequest();
        return currentRequest != null ? currentRequest.getHeader("User-Agent") : null;
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }

        return null;
    }
}
