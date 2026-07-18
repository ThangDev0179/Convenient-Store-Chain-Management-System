package com.retail.auth;

import com.retail.auth.dto.ChangePasswordRequest;
import com.retail.auth.dto.ForgotPasswordRequest;
import com.retail.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void logout(HttpServletRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    boolean shouldForceChangePassword(String username);
    void processForgotPassword(ForgotPasswordRequest request);
}
