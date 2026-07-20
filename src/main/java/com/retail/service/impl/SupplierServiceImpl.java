package com.retail.service.impl;

import com.retail.dto.SupplierRequest;
import com.retail.dto.SupplierResponse;
import com.retail.entity.Supplier;
import com.retail.entity.SupplierStatus;
import com.retail.entity.PurchaseOrderStatus;
import com.retail.exception.ValidationException;
import com.retail.entity.AuditLog;
import com.retail.entity.Employee;
import com.retail.repository.AuditLogRepository;
import com.retail.repository.EmployeeRepository;
import com.retail.repository.SupplierRepository;
import com.retail.repository.PurchaseOrderRepository;
import com.retail.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private com.retail.repository.ProductRepository productRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest currentRequest;

    private void logAudit(String actionType, String entityName, Long entityId, String oldValue, String newValue, String reason) {
        Employee employee = null;
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                employee = employeeRepository.findByUsername(auth.getName()).orElse(null);
            }
        } catch (Exception ignored) {}

        String ip = null;
        String userAgent = null;
        if (currentRequest != null) {
            ip = currentRequest.getRemoteAddr();
            userAgent = currentRequest.getHeader("User-Agent");
        }

        AuditLog log = AuditLog.builder()
                .employee(employee)
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .ipAddress(ip)
                .deviceId(userAgent)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> list(String search, SupplierStatus status, Pageable pageable) {
        String keyword = (search == null || search.trim().isEmpty()) ? null : search.trim();
        return supplierRepository.searchSuppliers(keyword, status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getDetail(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));
        return mapToResponse(supplier);
    }

    @Override
    public SupplierResponse create(SupplierRequest request) {
        validateSupplier(null, request);

        Supplier supplier = Supplier.builder()
                .supplierName(request.getSupplierName().trim())
                .contactPhone(request.getContactPhone() != null && !request.getContactPhone().trim().isEmpty() ? request.getContactPhone().trim() : null)
                .contactEmail(request.getContactEmail() != null && !request.getContactEmail().trim().isEmpty() ? request.getContactEmail().trim() : null)
                .address(request.getAddress() != null && !request.getAddress().trim().isEmpty() ? request.getAddress().trim() : null)
                .status(SupplierStatus.Active)
                .build();

        try {
            supplier = supplierRepository.save(supplier);
            supplierRepository.flush(); // Flush immediately inside transaction bounds to check for DB unique constraints
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Email hoặc số điện thoại nhà cung cấp đã tồn tại trong hệ thống");
        }

        return mapToResponse(supplier);
    }

    @Override
    public SupplierResponse update(Integer id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));

        validateSupplier(id, request);

        supplier.setSupplierName(request.getSupplierName().trim());
        supplier.setContactPhone(request.getContactPhone() != null && !request.getContactPhone().trim().isEmpty() ? request.getContactPhone().trim() : null);
        supplier.setContactEmail(request.getContactEmail() != null && !request.getContactEmail().trim().isEmpty() ? request.getContactEmail().trim() : null);
        supplier.setAddress(request.getAddress() != null && !request.getAddress().trim().isEmpty() ? request.getAddress().trim() : null);

        try {
            supplier = supplierRepository.save(supplier);
            supplierRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Email hoặc số điện thoại nhà cung cấp đã tồn tại trong hệ thống");
        }

        return mapToResponse(supplier);
    }

    @Override
    public void delete(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));

        // Check if supplier has any pending purchase orders
        checkPendingOrders(id);

        String oldStatus = supplier.getStatus().name();
        supplier.setStatus(SupplierStatus.Inactive);
        supplierRepository.save(supplier);
        supplierRepository.flush();

        logAudit("UPDATE_SUPPLIER_STATUS", "Supplier", id.longValue(), oldStatus, SupplierStatus.Inactive.name(), "Ngừng hoạt động nhà cung cấp (Soft Delete)");
    }

    @Override
    public void restore(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));
        String oldStatus = supplier.getStatus().name();
        supplier.setStatus(SupplierStatus.Active);
        supplierRepository.save(supplier);
        supplierRepository.flush();

        logAudit("UPDATE_SUPPLIER_STATUS", "Supplier", id.longValue(), oldStatus, SupplierStatus.Active.name(), "Khôi phục hoạt động nhà cung cấp");
    }

    private void checkPendingOrders(Integer supplierId) {
        List<PurchaseOrderStatus> pendingStatuses = Arrays.asList(
                PurchaseOrderStatus.Draft,
                PurchaseOrderStatus.Submitted,
                PurchaseOrderStatus.Partially_Received,
                PurchaseOrderStatus.Received_Partial
        );
        boolean hasPending = purchaseOrderRepository.existsBySupplierSupplierIdAndStatusIn(supplierId, pendingStatuses);
        if (hasPending) {
            throw new ValidationException("Không thể ngừng hoạt động nhà cung cấp này vì đang có đơn mua hàng (Purchase Order) chưa hoàn tất.");
        }

        boolean hasActiveProducts = productRepository.existsByDefaultSupplierSupplierIdAndStatus(supplierId, com.retail.entity.ProductStatus.Active);
        if (hasActiveProducts) {
            throw new ValidationException("Không thể ngừng hoạt động nhà cung cấp này vì đang là Nhà cung cấp mặc định của các sản phẩm đang kinh doanh.");
        }
    }

    private void validateSupplier(Integer id, SupplierRequest request) {
        String name = request.getSupplierName() != null ? request.getSupplierName().trim() : null;
        if (name != null && !name.isEmpty()) {
            boolean nameExists = (id == null)
                    ? supplierRepository.existsBySupplierName(name)
                    : supplierRepository.existsBySupplierNameAndSupplierIdNot(name, id);
            if (nameExists) {
                throw new ValidationException("Tên nhà cung cấp đã tồn tại trong hệ thống");
            }
        }

        String email = request.getContactEmail() != null ? request.getContactEmail().trim() : null;
        String phone = request.getContactPhone() != null ? request.getContactPhone().trim() : null;

        if (email != null && !email.isEmpty()) {
            boolean emailExists = (id == null) 
                    ? supplierRepository.existsByContactEmail(email)
                    : supplierRepository.existsByContactEmailAndSupplierIdNot(email, id);
            if (emailExists) {
                throw new ValidationException("Email liên hệ đã tồn tại trong hệ thống");
            }
        }

        if (phone != null && !phone.isEmpty()) {
            boolean phoneExists = (id == null)
                    ? supplierRepository.existsByContactPhone(phone)
                    : supplierRepository.existsByContactPhoneAndSupplierIdNot(phone, id);
            if (phoneExists) {
                throw new ValidationException("Số điện thoại liên hệ đã tồn tại trong hệ thống");
            }
        }
    }

    private SupplierResponse mapToResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getSupplierName())
                .contactPhone(supplier.getContactPhone())
                .contactEmail(supplier.getContactEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .createdBy(supplier.getCreatedBy())
                .updatedBy(supplier.getUpdatedBy())
                .build();
    }
}
