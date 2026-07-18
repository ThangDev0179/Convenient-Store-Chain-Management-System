package com.retail.audit.service.impl;

import com.retail.audit.entity.AuditLog;
import com.retail.audit.repository.AuditLogRepository;
import com.retail.audit.service.AuditLogService;
import com.retail.employee.Employee;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void logAction(Long employeeId, String actionType, String entityName, Long entityId,
                          String oldValue, String newValue, String reason, String ipAddress, String deviceId) {
        
        AuditLog auditLog = new AuditLog();
        if (employeeId != null) {
            auditLog.setEmployee(entityManager.getReference(Employee.class, employeeId));
        }
        auditLog.setActionType(actionType);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setReason(reason);
        auditLog.setIpAddress(ipAddress);
        auditLog.setDeviceId(deviceId);

        auditLogRepository.save(auditLog);
    }
}
