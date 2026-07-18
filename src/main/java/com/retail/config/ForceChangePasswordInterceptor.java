package com.retail.config;

import com.retail.employee.Employee;
import com.retail.employee.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ForceChangePasswordInterceptor implements HandlerInterceptor {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            Employee employee = employeeRepository.findByUsername(username).orElse(null);
            if (employee != null && Boolean.TRUE.equals(employee.getForceChangePassword())) {
                String uri = request.getRequestURI();
                // Allow request to fall through only if going to change-password or logging out
                if (!uri.equals("/change-password") && 
                    !uri.equals("/logout") && 
                    !uri.startsWith("/css/") && 
                    !uri.startsWith("/js/")) {
                    response.sendRedirect("/change-password");
                    return false;
                }
            }
        }
        return true;
    }
}
