package com.retail.config;

import com.retail.entity.*;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.RoleRepository;
import com.retail.repository.ProductCategoryRepository;
import com.retail.repository.ProductRepository;
import com.retail.repository.BranchInventoryRepository;
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
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking and initializing dummy data for testing...");

        // 0. Auto-migrate SQL Server VARCHAR columns to NVARCHAR for full Vietnamese Unicode support
        try {
            log.info("Migrating SQL Server VARCHAR columns to NVARCHAR...");
            jdbcTemplate.execute("ALTER TABLE dbo.Promotion ALTER COLUMN PromotionName NVARCHAR(200) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Product ALTER COLUMN ProductName NVARCHAR(150) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Product ALTER COLUMN Description NVARCHAR(MAX) NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Supplier ALTER COLUMN SupplierName NVARCHAR(200) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Supplier ALTER COLUMN Address NVARCHAR(300) NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.ProductCategory ALTER COLUMN CategoryName NVARCHAR(150) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.ProductUOM ALTER COLUMN UomName NVARCHAR(50) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Employee ALTER COLUMN FullName NVARCHAR(100) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Branch ALTER COLUMN BranchName NVARCHAR(100) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE dbo.Branch ALTER COLUMN Address NVARCHAR(255) NULL");
            log.info("SQL Server columns successfully migrated to NVARCHAR for full Vietnamese Unicode support!");
        } catch (Exception e) {
            log.warn("SQL Server column migration notice: {}", e.getMessage());
        }

        // 1. Khởi tạo Roles nếu chưa có
        if (roleRepository.count() == 0) {
            log.info("Creating default roles...");
            roleRepository.save(Role.builder().roleCode(RoleCode.ADMIN).roleName("Quản trị viên").build());
            roleRepository.save(Role.builder().roleCode(RoleCode.MANAGER).roleName("Quản lý kho").build());
            roleRepository.save(Role.builder().roleCode(RoleCode.STAFF).roleName("Nhân viên bán hàng").build());
        }

        // 2. Khởi tạo Branch 1 nếu chưa có
        java.util.List<Branch> allBranches = branchRepository.findAll();
        boolean hasBR001 = allBranches.stream().anyMatch(b -> "BR001".equals(b.getBranchCode()));

        if (!hasBR001) {
            log.info("Creating default branch 1...");
            Branch branch1 = Branch.builder()
                    .branchCode("BR001")
                    .branchName("Chi nhánh Trung tâm")
                    .address("123 Đường Test, Quận 1")
                    .status(BranchStatus.Active)
                    .build();
            branchRepository.save(branch1);
        }

        // 3. Khởi tạo tài khoản Employee (cho Branch 1) nếu chưa có
        if (employeeRepository.count() == 0) {
            log.info("Creating default test accounts...");

            Branch branch1 = branchRepository.findAll().stream().filter(b -> "BR001".equals(b.getBranchCode())).findFirst().orElseThrow();
            Role roleAdmin = roleRepository.findByRoleCode(RoleCode.ADMIN).orElseThrow();
            Role roleManager = roleRepository.findByRoleCode(RoleCode.MANAGER).orElseThrow();
            Role roleStaff = roleRepository.findByRoleCode(RoleCode.STAFF).orElseThrow();

            String defaultPassword = passwordEncoder.encode("123456");

            // Admin account
            Employee admin = Employee.builder()
                    .employeeCode("NV-2026-9999")
                    .username("admin")
                    .passwordHash(defaultPassword)
                    .fullName("Test Admin")
                    .email("admin@test.com")
                    .phone("0123456789")
                    .role(roleAdmin)
                    .branch(branch1)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            // Manager account
            Employee manager = Employee.builder()
                    .employeeCode("NV-2026-9998")
                    .username("manager")
                    .passwordHash(defaultPassword)
                    .fullName("Test Manager")
                    .email("manager@test.com")
                    .phone("0123456789")
                    .role(roleManager)
                    .branch(branch1)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            // Staff account
            Employee staff = Employee.builder()
                    .employeeCode("NV-2026-9997")
                    .username("staff")
                    .passwordHash(defaultPassword)
                    .fullName("Test Staff")
                    .email("staff@test.com")
                    .phone("0123456789")
                    .role(roleStaff)
                    .branch(branch1)
                    .status(EmployeeStatus.Active)
                    .forceChangePassword(false)
                    .build();

            employeeRepository.save(admin);
            employeeRepository.save(manager);
            employeeRepository.save(staff);

            log.info("3 Test accounts created successfully:");
        }

        // 4. Đảm bảo Branch 1 có quản lý để không bị lỗi UNIQUE NULL khi tạo Branch 2
        Branch b1 = branchRepository.findAll().stream().filter(b -> "BR001".equals(b.getBranchCode())).findFirst().orElse(null);
        if (b1 != null && b1.getManager() == null) {
            employeeRepository.findByUsername("manager").ifPresent(mgr -> {
                b1.setManager(mgr);
                branchRepository.save(b1);
            });
        }

        // 5. Khởi tạo Branch 2
        boolean hasBR002 = branchRepository.findAll().stream().anyMatch(b -> "BR002".equals(b.getBranchCode()));
        if (!hasBR002) {
            log.info("Creating default branch 2...");
            Branch branch2 = Branch.builder()
                    .branchCode("BR002")
                    .branchName("Chi nhánh Quận 2")
                    .address("456 Đường Test, Quận 2")
                    .status(BranchStatus.Active)
                    .build();
            branchRepository.save(branch2); // Sẽ không lỗi vì b1 đã hết NULL
        }

        // 6. Đảm bảo Branch 2 có quản lý
        Branch b2 = branchRepository.findAll().stream().filter(b -> "BR002".equals(b.getBranchCode())).findFirst().orElse(null);
        if (b2 != null && b2.getManager() == null) {
            Employee mgr2 = employeeRepository.findByUsername("manager2").orElse(null);
            if (mgr2 == null) {
                Role roleManager = roleRepository.findByRoleCode(RoleCode.MANAGER).orElseThrow();
                mgr2 = Employee.builder()
                        .employeeCode("NV-2026-9996")
                        .username("manager2")
                        .passwordHash(passwordEncoder.encode("123456"))
                        .fullName("Test Manager 2")
                        .email("manager2@test.com")
                        .phone("0123456789")
                        .role(roleManager)
                        .branch(b2)
                        .status(EmployeeStatus.Active)
                        .forceChangePassword(false)
                        .build();
                mgr2 = employeeRepository.save(mgr2);
            }
            b2.setManager(mgr2);
            branchRepository.save(b2);
        }

        // 4. Khởi tạo Sản phẩm và Tồn kho mẫu
        if (productRepository.count() == 0) {
            log.info("Creating default products and inventory...");
            ProductCategory category = ProductCategory.builder()
                    .categoryName("Nước giải khát")
                    .skuPrefix("DRK")
                    .build();
            categoryRepository.save(category);

            Product product = Product.builder()
                    .sku("DRK-0001")
                    .productName("Coca Cola 330ml")
                    .category(category)
                    .standardPrice(new java.math.BigDecimal("10000.00"))
                    .status(ProductStatus.Active)
                    .build();
            productRepository.save(product);

            // Cấp tồn kho cho 2 chi nhánh
            java.util.List<Branch> branches = branchRepository.findAll();
            if (branches.size() >= 2 && inventoryRepository.count() == 0) {
                Branch invB1 = branches.get(0);
                Branch invB2 = branches.get(1);

                BranchInventory inv1 = BranchInventory.builder()
                        .branch(invB1)
                        .product(product)
                        .qtyOnHand(new java.math.BigDecimal("1000.000"))
                        .qtyInTransit(java.math.BigDecimal.ZERO)
                        .build();
                inventoryRepository.save(inv1);

                BranchInventory inv2 = BranchInventory.builder()
                        .branch(invB2)
                        .product(product)
                        .qtyOnHand(java.math.BigDecimal.ZERO)
                        .qtyInTransit(java.math.BigDecimal.ZERO)
                        .build();
                inventoryRepository.save(inv2);
            }
        } else if (inventoryRepository.count() == 0) {
            java.util.List<Branch> branches = branchRepository.findAll();
            Product product = productRepository.findAll().get(0);
            if (branches.size() >= 2) {
                Branch invB1 = branches.get(0);
                Branch invB2 = branches.get(1);

                BranchInventory inv1 = BranchInventory.builder()
                        .branch(invB1)
                        .product(product)
                        .qtyOnHand(new java.math.BigDecimal("1000.000"))
                        .qtyInTransit(java.math.BigDecimal.ZERO)
                        .build();
                inventoryRepository.save(inv1);

                BranchInventory inv2 = BranchInventory.builder()
                        .branch(invB2)
                        .product(product)
                        .qtyOnHand(java.math.BigDecimal.ZERO)
                        .qtyInTransit(java.math.BigDecimal.ZERO)
                        .build();
                inventoryRepository.save(inv2);
            }
        }
    }
}
