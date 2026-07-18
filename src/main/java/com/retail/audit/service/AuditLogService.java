package com.retail.audit.service;

public interface AuditLogService {
    /**
     * Ghi nhận hành động của nhân viên vào AuditLog.
     */
    void logAction(Long employeeId, String actionType, String entityName, Long entityId,
                   String oldValue, String newValue, String reason, String ipAddress, String deviceId);
}
