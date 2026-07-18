package com.retail.entity;
import com.retail.dto.ChangePasswordRequest;
import com.retail.dto.ForgotPasswordRequest;
import com.retail.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ModelAndView handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("error", ex.getMessage());
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getParameter("username"));
        mav.addObject("loginRequest", loginRequest);
        return mav;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ModelAndView handleAccountInactive(AccountInactiveException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("error", ex.getMessage());
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getParameter("username"));
        mav.addObject("loginRequest", loginRequest);
        return mav;
    }

    @ExceptionHandler(ChangePasswordException.class)
    public ModelAndView handleChangePassword(ChangePasswordException ex) {
        ModelAndView mav = new ModelAndView("change-password");
        mav.addObject("error", ex.getMessage());
        mav.addObject("changePasswordRequest", new ChangePasswordRequest());
        return mav;
    }

    @ExceptionHandler(ForgotPasswordException.class)
    public ModelAndView handleForgotPassword(ForgotPasswordException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("forgot-password");
        mav.addObject("error", ex.getMessage());
        
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(request.getParameter("email"));
        mav.addObject("forgotPasswordRequest", forgotPasswordRequest);
        return mav;
    }
}