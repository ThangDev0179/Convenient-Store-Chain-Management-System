package com.retail.service.impl;
import com.retail.entity.AuditLog;
import com.retail.repository.AuditLogRepository;
import com.retail.service.AuditLogService;
import com.retail.entity.Employee;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long employeeId, String actionType, String entityName, Long entityId,
                          String oldValue, String newValue, String reason, String ipAddress, String deviceId) {
        AuditLog log = AuditLog.builder()
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .build();

        if (employeeId != null) {
            log.setEmployee(entityManager.getReference(Employee.class, employeeId));
        }

        auditLogRepository.save(log);
    }
}