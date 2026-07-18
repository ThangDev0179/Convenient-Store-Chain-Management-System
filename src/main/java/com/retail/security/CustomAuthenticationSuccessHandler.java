package com.retail.security;

import com.retail.employee.Employee;
import com.retail.auth.ActiveSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private ActiveSessionService activeSessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Employee employee = userDetails.getEmployee();

        // Capture session info and log/upsert active session
        String sessionToken = request.getSession(true).getId();
        String ipAddress = request.getRemoteAddr();
        String deviceId = request.getHeader("User-Agent");
        // Active session set to expire in 30 minutes
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        activeSessionService.createOrReplace(employee, sessionToken, deviceId, ipAddress, expiresAt);

        if (Boolean.TRUE.equals(employee.getForceChangePassword())) {
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String targetUrl = "/";
        for (GrantedAuthority authority : authorities) {
            String roleName = authority.getAuthority();
            if ("ROLE_ADMIN".equals(roleName)) {
                targetUrl = "/admin/dashboard";
                break;
            } else if ("ROLE_MANAGER".equals(roleName)) {
                targetUrl = "/manager/dashboard";
                break;
            } else if ("ROLE_STAFF".equals(roleName)) {
                targetUrl = "/staff/dashboard";
                break;
            }
        }

        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
