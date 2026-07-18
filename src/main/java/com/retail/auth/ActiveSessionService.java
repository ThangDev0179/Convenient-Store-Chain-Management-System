package com.retail.auth;

import com.retail.employee.Employee;
import java.time.LocalDateTime;

public interface ActiveSessionService {
    void createOrReplace(Employee employee, String sessionToken, String deviceId, String ipAddress, LocalDateTime expiresAt);
    void invalidate(Long employeeId);
    boolean isValid(Long employeeId, String sessionToken);
}
