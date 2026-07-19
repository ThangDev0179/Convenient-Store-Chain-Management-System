package com.retail.service.impl;
import com.retail.entity.AccountInactiveException;
import com.retail.service.AuthService;
import com.retail.entity.ChangePasswordException;
import com.retail.dto.ChangePasswordRequest;
import com.retail.entity.Employee;
import com.retail.repository.EmployeeRepository;
import com.retail.entity.EmployeeStatus;
import com.retail.entity.ForgotPasswordException;
import com.retail.dto.ForgotPasswordRequest;
import com.retail.entity.InvalidCredentialsException;
import com.retail.dto.LoginRequest;
import com.retail.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Autowired
    private EmailService emailService;

    // Simple in-memory token store for password reset tokens
    private static final java.util.Map<String, TokenInfo> resetTokenStore = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenInfo {
        private final String email;
        private final java.time.LocalDateTime expiry;

        public TokenInfo(String email, java.time.LocalDateTime expiry) {
            this.email = email;
            this.expiry = expiry;
        }
    }

    @Override
    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.trim().isEmpty()) {
            throw new InvalidCredentialsException("Tên đăng nhập không được để trống");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidCredentialsException("Mật khẩu không được để trống");
        }

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(password, employee.getPasswordHash())) {
            throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (employee.getStatus() == EmployeeStatus.Inactive) {
            throw new AccountInactiveException("Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động");
        }

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + employee.getRole().getRoleCode().name())
        );

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        employee.getUsername(),
                        employee.getPasswordHash(),
                        authorities
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    @Override
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (currentPassword == null || currentPassword.trim().isEmpty() ||
            newPassword == null || newPassword.trim().isEmpty() ||
            confirmPassword == null || confirmPassword.trim().isEmpty()) {
            throw new ChangePasswordException("Các trường thông tin không được để trống");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new ChangePasswordException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        if (newPassword.length() < 6) {
            throw new ChangePasswordException("Mật khẩu mới phải từ 6 ký tự trở lên");
        }

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ChangePasswordException("Tài khoản không tồn tại"));

        if (!passwordEncoder.matches(currentPassword, employee.getPasswordHash())) {
            throw new ChangePasswordException("Mật khẩu cũ không chính xác");
        }

        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setForceChangePassword(false);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldForceChangePassword(String username) {
        if (username == null) {
            return false;
        }
        return employeeRepository.findByUsername(username)
                .map(employee -> Boolean.TRUE.equals(employee.getForceChangePassword()))
                .orElse(false);
    }

    @Override
    public void processForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();

        if (email == null || email.trim().isEmpty()) {
            throw new ForgotPasswordException("Email không được để trống");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ForgotPasswordException("Email không đúng định dạng");
        }

        java.util.Optional<Employee> employeeOpt = employeeRepository.findByEmail(email.trim());

        if (employeeOpt.isPresent()) {
            // Internally: generate a password reset token, save it, and send a real email via EmailService
            String token = java.util.UUID.randomUUID().toString();
            resetTokenStore.put(token, new TokenInfo(email.trim(), java.time.LocalDateTime.now().plusMinutes(15)));
            
            emailService.sendResetToken(email.trim(), token);

            // Timing-attack mitigation: perform a dummy BCrypt-cost operation on the success path
            passwordEncoder.matches("dummyPassword", "$2a$10$ZXx6yB8B5u16f39Gz13v..6G8X2rX1r3p2G5u16f39Gz13v..6G8X");
        } else {
            // Timing-attack mitigation: perform a dummy BCrypt-cost operation on the non-existent path
            passwordEncoder.matches("dummyPassword", "$2a$10$ZXx6yB8B5u16f39Gz13v..6G8X2rX1r3p2G5u16f39Gz13v..6G8X");
        }
    }
}