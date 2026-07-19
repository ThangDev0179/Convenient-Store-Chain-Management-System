package com.retail.auth.exception;

import com.retail.auth.dto.ChangePasswordRequest;
import com.retail.auth.dto.ForgotPasswordRequest;
import com.retail.auth.dto.LoginRequest;
import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified GlobalExceptionHandler — handles:
 *  - Auth-specific exceptions (login, change password, forgot password)
 *  - POS/Refund module exceptions (ResourceNotFoundException, BusinessRuleViolationException, etc.)
 *  - Returns JSON for AJAX requests (Accept: application/json)
 *  - Returns Thymeleaf view model for MVC requests
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // ─── Auth Exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(InvalidCredentialsException.class)
    public ModelAndView handleInvalidCredentials(InvalidCredentialsException ex,
                                                  HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("error", ex.getMessage());
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getParameter("username"));
        mav.addObject("loginRequest", loginRequest);
        return mav;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ModelAndView handleAccountInactive(AccountInactiveException ex,
                                               HttpServletRequest request) {
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
    public ModelAndView handleForgotPassword(ForgotPasswordException ex,
                                              HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("forgot-password");
        mav.addObject("error", ex.getMessage());
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(request.getParameter("email"));
        mav.addObject("forgotPasswordRequest", forgotPasswordRequest);
        return mav;
    }

    // ─── POS / Refund — 404 Not Found ────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex,
                                  HttpServletRequest request,
                                  Model model) {
        if (isJsonRequest(request)) {
            return buildJsonError(HttpStatus.NOT_FOUND, null, ex.getMessage());
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    // ─── POS / Refund — 422 Business Rule Violation ───────────────────────────

    @ExceptionHandler(BusinessRuleViolationException.class)
    public Object handleBusinessRule(BusinessRuleViolationException ex,
                                      HttpServletRequest request,
                                      Model model) {
        if (isJsonRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 422);
            body.put("error", "Unprocessable Entity");
            body.put("ruleCode", ex.getRuleCode());
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
        }
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/business";
    }

    // ─── 400 Bean Validation ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex,
                                    HttpServletRequest request,
                                    Model model) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        if (isJsonRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 400);
            body.put("error", "Validation Failed");
            body.put("errors", errors);
            return ResponseEntity.badRequest().body(body);
        }
        model.addAttribute("validationErrors", errors);
        return "error/validation";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex,
                                             HttpServletRequest request,
                                             Model model) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());
        if (isJsonRequest(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 400);
            body.put("error", "Validation Failed");
            body.put("errors", errors);
            return ResponseEntity.badRequest().body(body);
        }
        model.addAttribute("validationErrors", errors);
        return "error/validation";
    }

    // ─── 403 Access Denied ────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                      HttpServletRequest request,
                                      Model model) {
        if (isJsonRequest(request)) {
            return buildJsonError(HttpStatus.FORBIDDEN, null, "Bạn không có quyền thực hiện thao tác này.");
        }
        model.addAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này.");
        return "error/403";
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String ct = request.getHeader("Content-Type");
        return (accept != null && accept.contains("application/json"))
               || (ct != null && ct.contains("application/json"));
    }

    private ResponseEntity<Map<String, Object>> buildJsonError(HttpStatus status,
                                                                String ruleCode,
                                                                String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        if (ruleCode != null) body.put("ruleCode", ruleCode);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
