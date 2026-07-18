package com.retail.service;

import com.retail.entity.ActiveSession;
import com.retail.entity.Employee;
import com.retail.repository.ActiveSessionRepository;
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

        // Remove existing active session for this employee (enforcing single active session)
        activeSessionRepository.deleteById(employee.getEmployeeId());
        activeSessionRepository.flush(); // Commit deletion immediately

        // Create new active session record
        ActiveSession activeSession = ActiveSession.builder()
                .employeeId(employee.getEmployeeId())
                .employee(employee)
                .sessionToken(sessionToken)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .loginAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        activeSessionRepository.save(activeSession);
    }

    @Override
    public void invalidate(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        activeSessionRepository.deleteById(employeeId);
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

        // Compare session tokens and check if not expired
        return activeSession.getSessionToken().equals(sessionToken) &&
               activeSession.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
