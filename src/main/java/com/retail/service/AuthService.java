package com.retail.service;
import com.retail.dto.ChangePasswordRequest;
import com.retail.dto.ForgotPasswordRequest;
import com.retail.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void logout(HttpServletRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    boolean shouldForceChangePassword(String username);
    void processForgotPassword(ForgotPasswordRequest request);
}