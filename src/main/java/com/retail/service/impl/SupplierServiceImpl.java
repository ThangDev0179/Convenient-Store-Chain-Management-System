package com.retail.service.impl;

import com.retail.dto.SupplierRequest;
import com.retail.dto.SupplierResponse;
import com.retail.entity.Supplier;
import com.retail.entity.SupplierStatus;
import com.retail.exception.ValidationException;
import com.retail.repository.SupplierRepository;
import com.retail.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    public Page<SupplierResponse> list(String search, SupplierStatus status, Pageable pageable) {
        String keyword = (search == null || search.trim().isEmpty()) ? null : search.trim();
        return supplierRepository.searchSuppliers(keyword, status, pageable)
                .map(this::mapToResponse);
    }

    @Override
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

        supplier = supplierRepository.save(supplier);
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

        supplier = supplierRepository.save(supplier);
        return mapToResponse(supplier);
    }

    @Override
    public void delete(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));
        supplier.setStatus(SupplierStatus.Inactive);
        supplierRepository.save(supplier);
    }

    @Override
    public void restore(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp với ID: " + id));
        supplier.setStatus(SupplierStatus.Active);
        supplierRepository.save(supplier);
    }

    private void validateSupplier(Integer id, SupplierRequest request) {
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
                .build();
    }
}
