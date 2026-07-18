package com.retail.security;

import com.retail.entity.Employee;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Employee employee = userDetails.getEmployee();

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
