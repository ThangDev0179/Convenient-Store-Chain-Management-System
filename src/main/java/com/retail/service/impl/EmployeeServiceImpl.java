package com.retail.service.impl;
import com.retail.entity.Branch;
import com.retail.exception.BranchAlreadyHasManagerException;
import com.retail.repository.BranchRepository;
import com.retail.dto.ChangeRoleRequest;
import com.retail.dto.CreateEmployeeRequest;
import com.retail.service.EmailService;
import com.retail.entity.Employee;
import com.retail.repository.EmployeeRepository;
import com.retail.dto.EmployeeResponse;
import com.retail.service.EmployeeService;
import com.retail.entity.EmployeeStatus;
import com.retail.entity.Role;
import com.retail.repository.RoleRepository;
import com.retail.dto.TransferBranchRequest;
import com.retail.dto.UpdateEmployeeRequest;
import com.retail.exception.ValidationException;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private boolean checkIsBranchManager(Employee employee) {
        return employee.getBranch() != null &&
               employee.getBranch().getManager() != null &&
               employee.getBranch().getManager().getEmployeeId().equals(employee.getEmployeeId());
    }

    @Override
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new ValidationException("Họ và tên không được để trống");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Email không đúng định dạng");
        }
        if (request.getRoleId() == null) {
            throw new ValidationException("Chức vụ không được để trống");
        }
        if (request.getBranchId() == null) {
            throw new ValidationException("Chi nhánh làm việc không được để trống");
        }

        if (employeeRepository.existsByEmail(request.getEmail().trim())) {
            throw new ValidationException("Email đã tồn tại trong hệ thống");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Chức vụ không tồn tại"));

        boolean isManager = Boolean.TRUE.equals(request.getIsBranchManager());
        if (isManager && employeeRepository.existsActiveBranchManager(branch.getBranchId(), EmployeeStatus.Active)) {
            throw new BranchAlreadyHasManagerException("Chi nhánh này đã có Quản lý chi nhánh đang hoạt động.");
        }

        int currentYear = LocalDate.now().getYear();
        String prefix = "NV-" + currentYear + "-";
        String maxCode = employeeRepository.findMaxEmployeeCodeWithPrefix(prefix);
        int seq = 1;
        if (maxCode != null && maxCode.startsWith(prefix) && maxCode.length() == 12) {
            try {
                String seqPart = maxCode.substring(prefix.length());
                seq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {}
        }
        String employeeCode = prefix + String.format("%04d", seq);

        String baseUsername = generateUsername(request.getFullName().trim());
        String username = baseUsername;
        int collisionCount = 1;
        while (employeeRepository.existsByUsername(username)) {
            username = baseUsername + collisionCount;
            collisionCount++;
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String hashedPwd = passwordEncoder.encode(tempPassword);

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .username(username)
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : "")
                .role(role)
                .branch(branch)
                .passwordHash(hashedPwd)
                .status(EmployeeStatus.Active)
                .forceChangePassword(true)
                .build();

        Employee saved = employeeRepository.save(employee);

        if (isManager) {
            branch.setManager(saved);
            branchRepository.save(branch);
        }

        emailService.sendCredentials(saved.getEmail(), saved.getUsername(), tempPassword);

        return mapToResponse(saved);
      }

    @Override
    public EmployeeResponse update(Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new ValidationException("Họ và tên không được để trống");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email không được để trống");
        }
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Email không đúng định dạng");
        }

        String cleanedEmail = request.getEmail().trim();
        if (employeeRepository.existsByEmailAndEmployeeIdNot(cleanedEmail, employeeId)) {
            throw new ValidationException("Email đã tồn tại ở tài khoản khác");
        }

        employee.setFullName(request.getFullName().trim());
        employee.setEmail(cleanedEmail);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");

        Employee updated = employeeRepository.save(employee);
        return mapToResponse(updated);
    }

    @Override
    public void lock(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        if (employee.getStatus() == EmployeeStatus.Inactive) {
            return;
        }

        if (checkIsBranchManager(employee)) {
            long activeManagers = employeeRepository.countActiveBranchManagers(employee.getBranch().getBranchId(), EmployeeStatus.Active);
            if (activeManagers <= 1) {
                throw new ValidationException("Không thể khóa vì đây là quản lý chi nhánh duy nhất đang hoạt động.");
            }
        }

        employee.setStatus(EmployeeStatus.Inactive);
        employeeRepository.save(employee);
    }

    @Override
    public void unlock(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        if (employee.getStatus() == EmployeeStatus.Active) {
            return;
      }

        if (checkIsBranchManager(employee)) {
            if (employeeRepository.existsActiveBranchManager(employee.getBranch().getBranchId(), EmployeeStatus.Active)) {
                throw new ValidationException("Chi nhánh đã có quản lý đang hoạt động. Vui lòng tắt cờ quản lý của nhân viên này hoặc quản lý hiện tại trước.");
            }
        }

        employee.setStatus(EmployeeStatus.Active);
        employeeRepository.save(employee);
    }

    @Override
    public void resetPassword(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        employee.setPasswordHash(passwordEncoder.encode(tempPassword));
        employee.setForceChangePassword(true);

        employeeRepository.save(employee);

        emailService.sendCredentials(employee.getEmail(), employee.getUsername(), tempPassword);
    }

    @Override
    public void changeRole(Long employeeId, ChangeRoleRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        if (request.getRoleId() == null) {
            throw new ValidationException("Chức vụ không được để trống");
        }

        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ValidationException("Chức vụ không tồn tại"));

        employee.setRole(newRole);
        employeeRepository.save(employee);
    }

    @Override
    public void transferBranch(Long employeeId, TransferBranchRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));

        if (request.getNewBranchId() == null) {
            throw new ValidationException("Chi nhánh mới không được để trống");
        }

        if (checkIsBranchManager(employee)) {
            throw new ValidationException("Nhân viên này đang giữ chức vụ Quản lý chi nhánh. Vui lòng bỏ chọn chức vụ Quản lý trước khi chuyển chi nhánh.");
        }

        Branch newBranch = branchRepository.findById(request.getNewBranchId())
                .orElseThrow(() -> new ValidationException("Chi nhánh mới không tồn tại"));

        employee.setBranch(newBranch);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String search, Integer branchId, Integer roleId, EmployeeStatus status, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("branchId"), branchId));
            }
            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("roleId"), roleId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("employeeCode")), searchPattern);
                Predicate namePred = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate emailPred = cb.like(cb.lower(root.get("email")), searchPattern);
                predicates.add(cb.or(codePred, namePred, emailPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return employeeRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getDetail(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ValidationException("Nhân viên không tồn tại"));
        return mapToResponse(employee);
    }

    private String generateUsername(String fullName) {
        String temp = Normalizer.normalize(fullName, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String unsigned = pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9 ]", "");

        String[] parts = unsigned.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "employee";
        }

        StringBuilder username = new StringBuilder(parts[parts.length - 1]);
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                username.append(parts[i].charAt(0));
            }
        }
        return username.toString();
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .employeeId(employee.getEmployeeId())
                .employeeCode(employee.getEmployeeCode())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .roleId(employee.getRole().getRoleId())
                .roleName(employee.getRole().getRoleName())
                .roleCode(employee.getRole().getRoleCode().name())
                .branchId(employee.getBranch() != null ? employee.getBranch().getBranchId() : null)
                .branchName(employee.getBranch() != null ? employee.getBranch().getBranchName() : "Không có")
                .isBranchManager(checkIsBranchManager(employee))
                .status(employee.getStatus())
                .forceChangePassword(employee.getForceChangePassword())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}