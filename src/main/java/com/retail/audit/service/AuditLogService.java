package com.retail.audit.service;

public interface AuditLogService {
    void logAction(Long employeeId, String actionType, String entityName, Long entityId, 
                   String oldValue, String newValue, String reason, String ipAddress, String deviceId);
}
