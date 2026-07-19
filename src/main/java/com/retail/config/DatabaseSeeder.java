package com.retail.config;

import com.retail.entity.*;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles if they don't exist
        seedRole(RoleCode.ADMIN, "Administrator");
        seedRole(RoleCode.MANAGER, "Quản lý cửa hàng");
        seedRole(RoleCode.STAFF, "Nhân viên");

        // 2. Seed Branch if it doesn't exist
        Branch branch;
        Optional<Branch> existingBranch = branchRepository.findByBranchCode("CH01");
        if (existingBranch.isEmpty()) {
            branch = Branch.builder()
                    .branchCode("CH01")
                    .branchName("Chi nhánh chính")
                    .address("123 Đường chính, Hà Nội")
                    .status(BranchStatus.Active)
                    .build();
            try {
                branch = branchRepository.save(branch);
            } catch (Exception e) {
                // If it fails due to UNIQUE constraint on ManagerId = NULL, just pick the first available branch
                branch = branchRepository.findAll().stream().findFirst().orElseThrow();
            }
        } else {
            branch = existingBranch.get();
        }

        // 3. Seed Users
        seedEmployee("admin", "admin123", "Người quản trị", "admin@retail.com", "0999999999", RoleCode.ADMIN, branch, "NV-2026-0001");
        seedEmployee("manager", "manager123", "Quản lý nhánh", "manager@retail.com", "0988888888", RoleCode.MANAGER, branch, "NV-2026-0002");
        seedEmployee("staff", "staff123", "Nhân viên bán lẻ", "staff@retail.com", "0977777777", RoleCode.STAFF, branch, "NV-2026-0003");
    }

    private void seedRole(RoleCode roleCode, String roleName) {
        if (roleRepository.findByRoleCode(roleCode).isEmpty()) {
            Role role = Role.builder()
                    .roleCode(roleCode)
                    .roleName(roleName)
                    .build();
            roleRepository.save(role);
        }
    }

    private void seedEmployee(String username, String password, String fullName, String email, String phone, 
                              RoleCode roleCode, Branch branch, String code) {
        if (!employeeRepository.existsByUsername(username)) {
            Role role = roleRepository.findByRoleCode(roleCode)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

            Employee employee = Employee.builder()
                    .employeeCode(code)
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .email(email)
                    .phone(phone)
                    .role(role)
                    .branch(branch)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();
            employeeRepository.save(employee);
            System.out.println("Seeded employee: " + username + " with password: " + password);
        }
    }
}
