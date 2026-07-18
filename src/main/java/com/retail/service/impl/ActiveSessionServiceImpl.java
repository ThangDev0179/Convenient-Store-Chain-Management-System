package com.retail.service.impl;
import com.retail.entity.ActiveSession;
import com.retail.repository.ActiveSessionRepository;
import com.retail.service.ActiveSessionService;
import com.retail.entity.Employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@Transactional
public class ActiveSessionServiceImpl implements ActiveSessionService {

    @Autowired
    private ActiveSessionRepository activeSessionRepository;

    @Override
    public void createOrReplace(Employee employee, String sessionToken, String deviceId, String ipAddress, LocalDateTime expiresAt) {
        if (employee == null) {
            throw new IllegalArgumentException("Nhân viên không được để trống");
        }
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Token phiên đăng nhập không được để trống");
        }

        activeSessionRepository.upsertSession(
                employee.getEmployeeId(),
                sessionToken,
                deviceId,
                ipAddress,
                LocalDateTime.now(),
                expiresAt
        );
    }

    @Override
    public void invalidate(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        if (activeSessionRepository.existsById(employeeId)) {
            activeSessionRepository.deleteById(employeeId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValid(Long employeeId, String sessionToken) {
        if (employeeId == null || sessionToken == null) {
            return false;
        }
        ActiveSession activeSession = activeSessionRepository.findById(employeeId).orElse(null);
        if (activeSession == null) {
            return false;
        }

        return activeSession.getSessionToken().equals(sessionToken) &&
               (activeSession.getExpiresAt() == null || activeSession.getExpiresAt().isAfter(LocalDateTime.now()));
    }
}