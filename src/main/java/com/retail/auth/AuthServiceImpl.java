package com.retail.auth;

import com.retail.auth.dto.ChangePasswordRequest;
import com.retail.auth.dto.ForgotPasswordRequest;
import com.retail.auth.dto.LoginRequest;
import com.retail.auth.exception.AccountInactiveException;
import com.retail.auth.exception.ChangePasswordException;
import com.retail.auth.exception.ForgotPasswordException;
import com.retail.auth.exception.InvalidCredentialsException;
import com.retail.entity.Employee;
import com.retail.entity.EmployeeStatus;
import com.retail.repository.EmployeeRepository;
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

    @Override
    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.getUsername();
        String password = request.getPassword();

        // Manual validation
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

        // Establish Spring Security Context
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

        // Manually save context to session
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
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // Manual validation
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
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

        if (!passwordEncoder.matches(oldPassword, employee.getPasswordHash())) {
            throw new ChangePasswordException("Mật khẩu cũ không chính xác");
        }

        // Update password hash and clear force-change flag
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setForceChangePassword(false);
        employee.setForceChangePassword(false); // ensure cleared
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

        // Manual validation
        if (email == null || email.trim().isEmpty()) {
            throw new ForgotPasswordException("Email không được để trống");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ForgotPasswordException("Email không đúng định dạng");
        }

        if (!employeeRepository.existsByEmail(email)) {
            throw new ForgotPasswordException("Email không tồn tại trong hệ thống");
        }

        // Simulate link dispatching
        System.out.println("DEBUG: Password reset link generated and simulated for email: " + email);
    }
}
