package com.retail.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
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
        return "redirect:/login";
    }
}
