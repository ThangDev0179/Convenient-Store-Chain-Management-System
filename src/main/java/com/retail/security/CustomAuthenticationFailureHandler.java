package com.retail.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = "Đăng nhập thất bại. Vui lòng thử lại.";

        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            errorMessage = "Tên đăng nhập hoặc mật khẩu không chính xác.";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.";
        } else if (exception instanceof LockedException) {
            errorMessage = "Tài khoản của bạn đã bị khóa.";
        }

        request.getSession().setAttribute("loginError", errorMessage);
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}