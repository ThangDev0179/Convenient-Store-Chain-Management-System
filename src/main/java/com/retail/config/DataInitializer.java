package com.retail.config;

import com.retail.entity.*;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking and initializing dummy data for testing...");

        // 1. Khởi tạo Roles nếu chưa có
        if (roleRepository.count() == 0) {
            log.info("Creating default roles...");
            roleRepository.save(Role.builder().roleCode(RoleCode.ADMIN).roleName("Quản trị viên").build());
            roleRepository.save(Role.builder().roleCode(RoleCode.MANAGER).roleName("Quản lý cửa hàng").build());
            roleRepository.save(Role.builder().roleCode(RoleCode.STAFF).roleName("Nhân viên bán hàng").build());
        }

        // 2. Khởi tạo Branch nếu chưa có
        if (branchRepository.count() == 0) {
            log.info("Creating default branch...");
            Branch branch = Branch.builder()
                    .branchCode("BR001")
                    .branchName("Chi nhánh Trung tâm")
                    .address("123 Đường Test, Quận 1")
                    .status(BranchStatus.Active)
                    .build();
            branchRepository.save(branch);
        }

        // 3. Khởi tạo tài khoản Employee nếu chưa có
        if (employeeRepository.count() == 0) {
            log.info("Creating default test accounts...");

            Branch branch = branchRepository.findAll().get(0);
            Role roleAdmin = roleRepository.findByRoleCode(RoleCode.ADMIN).orElseThrow();
            Role roleManager = roleRepository.findByRoleCode(RoleCode.MANAGER).orElseThrow();
            Role roleStaff = roleRepository.findByRoleCode(RoleCode.STAFF).orElseThrow();

            String defaultPassword = passwordEncoder.encode("123456");

            // Admin account
            Employee admin = Employee.builder()
                    .employeeCode("EMP-ADMIN")
                    .username("admin")
                    .passwordHash(defaultPassword)
                    .fullName("Test Admin")
                    .email("admin@test.com")
                    .role(roleAdmin)
                    .branch(branch)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            // Manager account
            Employee manager = Employee.builder()
                    .employeeCode("EMP-MANAGER")
                    .username("manager")
                    .passwordHash(defaultPassword)
                    .fullName("Test Manager")
                    .email("manager@test.com")
                    .role(roleManager)
                    .branch(branch)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            // Staff account
            Employee staff = Employee.builder()
                    .employeeCode("EMP-STAFF")
                    .username("staff")
                    .passwordHash(defaultPassword)
                    .fullName("Test Staff")
                    .email("staff@test.com")
                    .role(roleStaff)
                    .branch(branch)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            employeeRepository.save(admin);
            employeeRepository.save(manager);
            employeeRepository.save(staff);

            log.info("3 Test accounts created successfully:");
            log.info("Username: admin / Password: 123456 (Role: ADMIN)");
            log.info("Username: manager / Password: 123456 (Role: MANAGER)");
            log.info("Username: staff / Password: 123456 (Role: STAFF)");
        }
    }
}
