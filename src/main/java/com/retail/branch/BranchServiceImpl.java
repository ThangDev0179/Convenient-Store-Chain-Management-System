package com.retail.branch;

import com.retail.branch.dto.BranchResponse;
import com.retail.branch.dto.CreateBranchRequest;
import com.retail.branch.dto.UpdateBranchRequest;
import com.retail.exception.BranchHasActiveDataException;
import com.retail.exception.DuplicateBranchCodeException;
import com.retail.exception.ValidationException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Override
    public BranchResponse create(CreateBranchRequest request) {
        if (request.getBranchCode() == null || request.getBranchCode().trim().isEmpty()) {
            throw new ValidationException("Mã chi nhánh không được để trống");
        }
        if (request.getBranchCode().trim().length() > 10) {
            throw new ValidationException("Mã chi nhánh không được dài quá 10 ký tự");
        }
        if (request.getBranchName() == null || request.getBranchName().trim().isEmpty()) {
            throw new ValidationException("Tên chi nhánh không được để trống");
        }
        if (request.getBranchName().trim().length() > 200) {
            throw new ValidationException("Tên chi nhánh không được dài quá 200 ký tự");
        }
        if (request.getAddress() != null && request.getAddress().trim().length() > 300) {
            throw new ValidationException("Địa chỉ không được dài quá 300 ký tự");
        }

        String cleanedCode = request.getBranchCode().trim().toUpperCase();
        if (branchRepository.existsByBranchCode(cleanedCode)) {
            throw new DuplicateBranchCodeException("Mã chi nhánh đã tồn tại trong hệ thống");
        }

        Branch branch = Branch.builder()
                .branchCode(cleanedCode)
                .branchName(request.getBranchName().trim())
                .address(request.getAddress() != null ? request.getAddress().trim() : "")
                .status(BranchStatus.Active)
                .build();

        Branch savedBranch = branchRepository.save(branch);
        return mapToResponse(savedBranch);
    }

    @Override
    public BranchResponse update(Integer branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        if (branch.getStatus() == BranchStatus.Archived) {
            throw new ValidationException("Không thể chỉnh sửa chi nhánh đã được lưu trữ");
        }

        if (request.getBranchName() == null || request.getBranchName().trim().isEmpty()) {
            throw new ValidationException("Tên chi nhánh không được để trống");
        }
        if (request.getBranchName().trim().length() > 200) {
            throw new ValidationException("Tên chi nhánh không được dài quá 200 ký tự");
        }
        if (request.getAddress() != null && request.getAddress().trim().length() > 300) {
            throw new ValidationException("Địa chỉ không được dài quá 300 ký tự");
        }

        branch.setBranchName(request.getBranchName().trim());
        branch.setAddress(request.getAddress() != null ? request.getAddress().trim() : "");

        Branch updatedBranch = branchRepository.save(branch);
        return mapToResponse(updatedBranch);
    }

    @Override
    public void archive(Integer branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        if (branch.getStatus() == BranchStatus.Archived) {
            return;
        }

        long activeEmployees = branchRepository.countActiveEmployees(branchId);
        if (activeEmployees > 0) {
            throw new BranchHasActiveDataException("Không thể lưu trữ vì vẫn có nhân viên đang hoạt động tại chi nhánh này");
        }

        long openWorkShifts = branchRepository.countOpenWorkShifts(branchId);
        if (openWorkShifts > 0) {
            throw new BranchHasActiveDataException("Không thể lưu trữ vì vẫn có ca làm việc chưa đóng tại chi nhánh này");
        }

        long openInvoices = branchRepository.countOpenInvoices(branchId);
        if (openInvoices > 0) {
            throw new BranchHasActiveDataException("Không thể lưu trữ vì vẫn có hóa đơn chưa hoàn thành tại chi nhánh này");
        }

        branch.setStatus(BranchStatus.Archived);
        branch.setArchivedAt(LocalDateTime.now());
        branchRepository.save(branch);
    }

    @Override
    public void close(Integer branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        if (branch.getStatus() == BranchStatus.Archived) {
            throw new ValidationException("Không thể đóng cửa một chi nhánh đã được lưu trữ");
        }

        // Close maps to Archive because database status CHECK constraint only allows N'Active' and N'Archived'
        branch.setStatus(BranchStatus.Archived);
        branch.setArchivedAt(LocalDateTime.now());
        branchRepository.save(branch);
    }

    @Override
    public void reopen(Integer branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));

        branch.setStatus(BranchStatus.Active);
        branch.setArchivedAt(null);
        branchRepository.save(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponse> list(String search, BranchStatus status, Pageable pageable) {
        Specification<Branch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate codePredicate = cb.like(cb.lower(root.get("branchCode")), searchPattern);
                Predicate namePredicate = cb.like(cb.lower(root.get("branchName")), searchPattern);
                predicates.add(cb.or(codePredicate, namePredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return branchRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getDetail(Integer branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ValidationException("Chi nhánh không tồn tại"));
        return mapToResponse(branch);
    }

    private BranchResponse mapToResponse(Branch branch) {
        return BranchResponse.builder()
                .branchId(branch.getBranchId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .address(branch.getAddress())
                .status(branch.getStatus())
                .createdAt(branch.getCreatedAt())
                .archivedAt(branch.getArchivedAt())
                .managerId(branch.getManager() != null ? branch.getManager().getEmployeeId() : null)
                .managerName(branch.getManager() != null ? branch.getManager().getFullName() : null)
                .build();
    }
}
