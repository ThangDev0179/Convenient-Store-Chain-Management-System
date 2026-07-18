package com.retail.controller;

import com.retail.entity.Employee;
import com.retail.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication auth) {
        populateEmployeeModel(model, auth);
        return "admin/dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model, Authentication auth) {
        populateEmployeeModel(model, auth);
        return "manager/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String staffDashboard(Model model, Authentication auth) {
        populateEmployeeModel(model, auth);
        return "staff/dashboard";
    }

    private void populateEmployeeModel(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            Employee employee = employeeRepository.findByUsername(username).orElse(null);
            model.addAttribute("employee", employee);
        }
    }
}
