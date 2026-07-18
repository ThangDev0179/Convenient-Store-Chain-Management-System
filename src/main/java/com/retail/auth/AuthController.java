package com.retail.auth;

import com.retail.auth.dto.ChangePasswordRequest;
import com.retail.auth.dto.ForgotPasswordRequest;
import com.retail.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return getRedirectUrl(auth);
        }

        // Retrieve failure message set by CustomAuthenticationFailureHandler
        String sessionError = (String) request.getSession().getAttribute("loginError");
        if (sessionError != null) {
            model.addAttribute("error", sessionError);
            request.getSession().removeAttribute("loginError");
        }

        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(@ModelAttribute ChangePasswordRequest changePasswordRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        String username = auth.getName();
        authService.changePassword(username, changePasswordRequest);
        return "redirect:/?passwordChanged";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@ModelAttribute ForgotPasswordRequest forgotPasswordRequest, Model model) {
        authService.processForgotPassword(forgotPasswordRequest);
        model.addAttribute("success", "Yêu cầu đặt lại mật khẩu thành công. Vui lòng kiểm tra email của bạn để lấy lại mật khẩu!");
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    private String getRedirectUrl(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String roleName = authority.getAuthority();
            if ("ROLE_ADMIN".equals(roleName)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_MANAGER".equals(roleName)) {
                return "redirect:/manager/dashboard";
            } else if ("ROLE_STAFF".equals(roleName)) {
                return "redirect:/staff/dashboard";
            }
        }
        return "redirect:/";
    }
}
