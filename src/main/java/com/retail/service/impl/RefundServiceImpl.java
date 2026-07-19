package com.retail.service.impl;

import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.common.exception.InvalidStateTransitionException;
import com.retail.common.exception.ResourceNotFoundException;
import com.retail.common.stub.*;
import com.retail.entity.Branch;
import com.retail.entity.Employee;
import com.retail.entity.RoleCode;
import com.retail.entity.Invoice;
import com.retail.entity.InvoiceDetail;
import com.retail.repository.InvoiceRepository;
import com.retail.dto.*;
import com.retail.dto.RefundResponse;
import com.retail.entity.*;
import com.retail.mapper.RefundMapper;
import com.retail.repository.RefundDetailRepository;
import com.retail.repository.RefundRepository;
// RefundSpecification removed — replaced by @Query JPQL in RefundRepository
import com.retail.service.RefundService;
import com.retail.validator.RefundValidator;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
// Specification API removed (HSF302: không có bài demo trong slide Chapter04)
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Refund business logic.
 *
 * INVENTORY CONVENTION (Section 3.2.3, Damaged item):
 *   Damaged → QtyOnHand += Quantity (physically back in store),
 *             QtyAvailable unchanged (cannot resell damaged goods).
 *   Module Disposal (thành viên 5) will process write-off later.
 *   ITH record written with TransactionType='Refund_Restock', QuantityChange=0,
 *   Reason='Damaged - not restocked (pending Disposal module)'.
 *
 * Integration notes (post-merge):
 *   - Employee, Branch → use com.retail.entity + com.retail.repository (real entities)
 *   - Product, BranchInventory, ITH → still use Stubs (other team members)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RefundServiceImpl implements RefundService {

    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("200000");

    private final RefundRepository refundRepository;
    private final RefundDetailRepository refundDetailRepository;
    private final RefundMapper refundMapper;
    private final RefundValidator refundValidator;
    private final InvoiceRepository invoiceRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    // ── Real repositories ─────────────────────────────────────────────────────
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;

    // ── Stub repositories (replace at full team merge) ────────────────────────
    private final com.retail.repository.ProductRepository productRepo;
    private final com.retail.repository.BranchInventoryRepository inventoryRepo;
    private final com.retail.repository.InventoryTransactionHistoryRepository ithRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // 3.2.1 — Create Refund
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse createRefund(CreateRefundRequest request) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(request.originalInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", request.originalInvoiceId()));

        refundValidator.validate(request, invoice);

        Employee requestedBy = resolveCurrentEmployee();
        Branch branch = branchRepository.findById(invoice.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", invoice.getBranchId()));

        Map<Long, InvoiceDetail> soldMap = invoice.getDetails().stream()
                .collect(Collectors.toMap(InvoiceDetail::getProductId, d -> d));

        List<RefundDetail> details = request.items().stream().map(item -> {
            InvoiceDetail soldDetail = soldMap.get(item.productId());
            return RefundDetail.builder()
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .conditionType(item.conditionType())
                    .unitRefundAmount(soldDetail.getUnitPrice())
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = details.stream()
                .map(d -> d.getUnitRefundAmount().multiply(d.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Rule #8: >= 200,000 VND → Pending_Approval; else Draft
        RefundStatus initialStatus = total.compareTo(APPROVAL_THRESHOLD) >= 0
                ? RefundStatus.Pending_Approval
                : RefundStatus.Draft;

        String refundCode = generateRefundCode(branch, LocalDate.now());

        Refund refund = Refund.builder()
                .refundCode(refundCode)
                .originalInvoiceId(invoice.getInvoiceId())
                .branchId(invoice.getBranchId())  // Rule #6: copy from invoice, not client
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .reason(request.reason())
                .totalRefundAmount(total)
                .status(initialStatus)
                .requestedBy(requestedBy.getEmployeeId())
                .build();

        details.forEach(refund::addDetail);
        Refund saved = refundRepository.save(refund);

        log.info("Created Refund {} (status={}) for invoice {}",
                refundCode, initialStatus, invoice.getInvoiceCode());

        return buildFullResponse(saved, invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.2.2 — Approve (MANAGER/ADMIN)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse approveRefund(Long refundId) {
        Refund refund = loadRefundWithDetails(refundId);
        completeRefund(refund, false);
        Invoice invoice = invoiceRepository.findById(refund.getOriginalInvoiceId()).orElse(null);
        return buildFullResponse(refundRepository.save(refund), invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.2.2 — Reject (MANAGER/ADMIN)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse rejectRefund(Long refundId, String rejectionReason) {
        Refund refund = loadRefundWithDetails(refundId);
        if (!refund.getStatus().canTransitionTo(RefundStatus.Rejected)) {
            throw new InvalidStateTransitionException("Refund", refund.getStatus().name(), "Rejected");
        }
        refund.setStatus(RefundStatus.Rejected);
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            refund.setReason(refund.getReason() + " [REJECTED: " + rejectionReason + "]");
        }
        refund.setApprovedBy(resolveCurrentEmployee().getEmployeeId());
        Invoice invoice = invoiceRepository.findById(refund.getOriginalInvoiceId()).orElse(null);
        return buildFullResponse(refundRepository.save(refund), invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.2.2 — Manager PIN Override at POS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundResponse overrideApprove(Long refundId, RefundOverrideApproveRequest request) {
        Employee manager = employeeRepository.findByUsername(request.managerUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Manager: " + request.managerUsername()));

        // Verify MANAGER or ADMIN role
        RoleCode roleCode = manager.getRole().getRoleCode();
        if (roleCode != RoleCode.MANAGER && roleCode != RoleCode.ADMIN) {
            throw new AccessDeniedException("User '" + request.managerUsername() + "' is not a Manager or Admin");
        }

        // Authenticate PIN against BCrypt password (no separate PIN table)
        if (!passwordEncoder.matches(request.managerPin(), manager.getPasswordHash())) {
            throw new BusinessRuleViolationException("INVALID_PIN",
                    "Manager PIN authentication failed");
        }

        Refund refund = loadRefundWithDetails(refundId);
        refund.setPinOverrideUsed(true);
        refund.setApprovedBy(manager.getEmployeeId());
        completeRefund(refund, true);

        Invoice invoice = invoiceRepository.findById(refund.getOriginalInvoiceId()).orElse(null);
        log.info("Manager {} PIN-overrode approval for Refund {}", request.managerUsername(), refund.getRefundCode());
        return buildFullResponse(refundRepository.save(refund), invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.2.4 — List & Detail
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<RefundResponse> listRefunds(RefundSearchRequest request) {
        boolean isStaff = !hasRole("MANAGER") && !hasRole("ADMIN");
        Integer branchIdFilter = isStaff ? resolveCurrentEmployee().getBranch().getBranchId() : request.branchId();

        // HSF302 Mục 3: dùng @Query JPQL (findByFilters) thay Specification API
        LocalDateTime fromDateTime = request.fromDate() != null ? request.fromDate().atStartOfDay() : null;
        LocalDateTime toDateTime = request.toDate() != null ? request.toDate().plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(request.page(), request.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Refund> page = refundRepository.findByFilters(
                request.status(), fromDateTime, toDateTime, branchIdFilter, pageable);

        // Batch load invoices
        Set<Long> invoiceIds = page.map(Refund::getOriginalInvoiceId).toSet();
        Map<Long, Invoice> invoiceMap = invoiceRepository.findAllById(invoiceIds).stream()
                .collect(Collectors.toMap(Invoice::getInvoiceId, i -> i));

        // Batch load employee names
        Set<Long> empIds = new HashSet<>();
        page.forEach(r -> {
            empIds.add(r.getRequestedBy());
            if (r.getApprovedBy() != null) empIds.add(r.getApprovedBy());
        });
        Map<Long, String> empNames = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName));

        return page.map(r -> refundMapper.toSummaryResponse(
                r,
                invoiceMap.get(r.getOriginalInvoiceId()) != null
                        ? invoiceMap.get(r.getOriginalInvoiceId()).getInvoiceCode() : null,
                empNames.get(r.getRequestedBy()),
                r.getApprovedBy() != null ? empNames.get(r.getApprovedBy()) : null));
    }

    @Override
    public RefundResponse getRefundDetail(Long refundId) {
        Refund refund = loadRefundWithDetails(refundId);
        Invoice invoice = invoiceRepository.findById(refund.getOriginalInvoiceId()).orElse(null);
        return buildFullResponse(refund, invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void completeRefund(Refund refund, boolean pinOverride) {
        if (!refund.getStatus().canTransitionTo(RefundStatus.Completed)) {
            throw new InvalidStateTransitionException("Refund", refund.getStatus().name(), "Completed");
        }
        refund.setStatus(RefundStatus.Completed);
        LocalDateTime now = LocalDateTime.now();
        refund.setApprovedAt(now);
        if (!pinOverride) {
            refund.setApprovedBy(resolveCurrentEmployee().getEmployeeId());
        }

        for (RefundDetail detail : refund.getDetails()) {
            BranchInventoryId invKey = new BranchInventoryId(refund.getBranchId(), detail.getProductId());
            BranchInventory inventory = entityManager.find(
                    BranchInventory.class, invKey, LockModeType.PESSIMISTIC_WRITE);

            if (inventory == null) {
                log.warn("No inventory record for branchId={} productId={} — skipping",
                         refund.getBranchId(), detail.getProductId());
                continue;
            }

            if (detail.getConditionType() == ConditionType.Resalable) {
                inventory.setQtyAvailable(inventory.getQtyAvailable().add(detail.getQuantity()));
                inventory.setQtyOnHand(inventory.getQtyOnHand().add(detail.getQuantity()));
                inventory.setUpdatedAt(now);
                writeIthRecord(refund, detail, detail.getQuantity(), now,
                        InventoryTransactionType.Refund_Restock, "Refund approved - Resalable");
            } else {
                // Damaged: cộng QtyOnHand, module Disposal xử lý xuất hủy
                inventory.setQtyOnHand(inventory.getQtyOnHand().add(detail.getQuantity()));
                inventory.setUpdatedAt(now);
                writeIthRecord(refund, detail, BigDecimal.ZERO, now,
                        InventoryTransactionType.Refund_Restock, "Damaged - not restocked (pending Disposal module)");
            }
        }
    }

    private void writeIthRecord(Refund refund, RefundDetail detail,
                                 BigDecimal qtyChange, LocalDateTime now,
                                 InventoryTransactionType txType, String reason) {
        InventoryTransactionHistory ith = InventoryTransactionHistory.builder()
                .branch(branchRepository.findById(refund.getBranchId()).orElse(null))
                .product(productRepo.findById(detail.getProductId()).orElse(null))
                .transactionType(txType)
                .referenceTable("Refund")
                .referenceId(refund.getRefundId())
                .quantityChange(qtyChange)
                .reason(reason)
                .createdBy(employeeRepository.findById(refund.getApprovedBy() != null ? refund.getApprovedBy() : refund.getRequestedBy()).orElse(null))
                .createdAt(now)
                .build();
        ithRepo.save(ith);
    }

    private String generateRefundCode(Branch branch, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        long count = refundRepository.countByBranchIdAndCreatedAtBetween(branch.getBranchId(), start, end);
        String seq = String.format("%04d", count + 1);
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("REF-%s-%s-%s", branch.getBranchCode(), dateStr, seq);
    }

    private Refund loadRefundWithDetails(Long refundId) {
        return refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
    }

    private RefundResponse buildFullResponse(Refund refund, Invoice invoice) {
        Set<Long> productIds = refund.getDetails().stream()
                .map(RefundDetail::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productRepo.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        Set<Long> empIds = new HashSet<>();
        empIds.add(refund.getRequestedBy());
        if (refund.getApprovedBy() != null) empIds.add(refund.getApprovedBy());
        Map<Long, String> empNames = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName));

        return refundMapper.toResponse(
                refund,
                invoice != null ? invoice.getInvoiceCode() : null,
                productMap,
                empNames.get(refund.getRequestedBy()),
                refund.getApprovedBy() != null ? empNames.get(refund.getApprovedBy()) : null);
    }

    private Employee resolveCurrentEmployee() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee (current user): " + username));
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}



