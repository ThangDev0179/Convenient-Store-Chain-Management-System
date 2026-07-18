package com.retail.security;
import com.retail.service.ActiveSessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SessionValidationFilter extends OncePerRequestFilter {

    @Autowired
    private ActiveSessionService activeSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Long employeeId = userDetails.getEmployee().getEmployeeId();
            String sessionToken = request.getSession(true).getId();

            if (!activeSessionService.isValid(employeeId, sessionToken)) {
                // Invalidate context and session
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession().invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.equals("/login") || 
               path.equals("/forgot-password") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/");
    }
}