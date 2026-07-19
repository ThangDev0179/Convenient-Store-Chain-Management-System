package com.retail.service.impl;

import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.common.exception.InsufficientStockException;
import com.retail.common.exception.InvalidStateTransitionException;
import com.retail.common.exception.ResourceNotFoundException;
import com.retail.common.stub.*;
import com.retail.entity.Branch;
import com.retail.entity.Employee;
import com.retail.entity.RoleCode;
import com.retail.dto.*;
import com.retail.dto.InvoiceResponse;
import com.retail.dto.ProductSearchResponse;
import com.retail.entity.Product;
import com.retail.entity.ProductStatus;
import com.retail.entity.BranchInventoryId;
import com.retail.entity.BranchInventory;
import com.retail.entity.InventoryTransactionHistory;
import com.retail.entity.InventoryTransactionType;
import com.retail.entity.Invoice;
import com.retail.entity.InvoiceDetail;
import com.retail.entity.InvoiceStatus;
import com.retail.mapper.InvoiceMapper;
import com.retail.repository.InvoiceDetailRepository;
import com.retail.repository.InvoiceRepository;
// InvoiceSpecification removed — replaced by @Query JPQL in InvoiceRepository
import com.retail.service.InvoiceService;
import com.retail.repository.BranchRepository;
import com.retail.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
// Specification API removed (HSF302: không có bài demo trong slide Chapter04)
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for POS / Invoice module.
 *
 * Integration notes (post-merge):
 *   - Employee, Branch → use com.retail.entity + com.retail.repository (real entities)
 *   - Product, Promotion, BranchInventory, BranchProductPrice → still use Stubs
 *     (these belong to other team members; replace at full merge)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;

    // ── Real repositories (already in project) ────────────────────────────────
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;

    // ── Stub repositories (cross-module — replaced at full team merge) ─────────
    private final com.retail.repository.ProductRepository productRepo;
    private final com.retail.repository.BranchInventoryRepository inventoryRepo;
    private final com.retail.repository.BranchProductPriceStubRepository priceRepo;
    private final com.retail.repository.PromotionDetailStubRepository promotionDetailRepo;
    private final com.retail.repository.InventoryTransactionHistoryRepository ithRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.1 — Create Invoice
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse createInvoice() {
        Employee cashier = resolveCurrentEmployee();
        Branch branch = branchRepository.findById(cashier.getBranch().getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", cashier.getBranch().getBranchId()));

        String invoiceCode = generateInvoiceCode(branch, LocalDate.now());

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .branchId(branch.getBranchId())
                .cashierId(cashier.getEmployeeId())
                .status(InvoiceStatus.Draft)
                .totalAmount(BigDecimal.ZERO)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created Invoice {} for cashier {}", invoiceCode, cashier.getEmployeeId());

        return invoiceMapper.toSummaryResponse(saved, cashier.getFullName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.1 — Product Search
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<ProductSearchResponse> searchProducts(String keyword, String sku,
                                                       Integer branchId, int page, int size) {
        List<Product> allProducts = productRepo.findAll();

        List<Product> filtered = allProducts.stream()
                .filter(p -> p.getStatus() == ProductStatus.Active)
                .filter(p -> {
                    if (sku != null && !sku.isBlank()) {
                        return sku.trim().equalsIgnoreCase(p.getSku());
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        return p.getProductName().toLowerCase().contains(keyword.trim().toLowerCase())
                               || p.getSku().toLowerCase().contains(keyword.trim().toLowerCase());
                    }
                    return true;
                })
                .toList();

        List<ProductSearchResponse> responses = filtered.stream()
                .map(p -> buildProductSearchResponse(p, branchId))
                .collect(Collectors.toList());

        int start = Math.min(page * size, responses.size());
        int end = Math.min(start + size, responses.size());
        return new PageImpl<>(responses.subList(start, end),
                PageRequest.of(page, size), responses.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.1 — Add Item (Rule #10: merge if duplicate)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse addItem(Long invoiceId, AddInvoiceItemRequest request) {
        Invoice invoice = loadEditableInvoice(invoiceId);
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId()));

        // Rule #10: If product already in invoice → merge quantities, not new row
        Optional<InvoiceDetail> existingDetail =
                invoiceDetailRepository.findByInvoice_InvoiceIdAndProductId(invoiceId, request.productId());

        if (existingDetail.isPresent()) {
            InvoiceDetail detail = existingDetail.get();
            detail.setQuantity(detail.getQuantity().add(request.quantity()));
            invoice.recalculateTotalAmount();
            log.debug("Merged quantity for product {} in invoice {}", request.productId(), invoiceId);
        } else {
            BigDecimal basePrice = resolveEffectivePrice(product, invoice.getBranchId());
            PromotionDetailStub bestPromo = findBestActivePromotion(request.productId());
            BigDecimal finalPrice = applyPromotion(basePrice, bestPromo);

            InvoiceDetail detail = InvoiceDetail.builder()
                    .productId(request.productId())
                    .quantity(request.quantity())
                    .unitPrice(finalPrice)
                    .promotionId(bestPromo != null ? bestPromo.getPromotionId() : null)
                    .build();
            invoice.addDetail(detail);
        }

        invoiceRepository.save(invoice);
        return buildFullResponse(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.1 — Update Item
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse updateItem(Long invoiceId, Long detailId, UpdateInvoiceItemRequest request) {
        Invoice invoice = loadEditableInvoice(invoiceId);
        InvoiceDetail detail = invoice.getDetails().stream()
                .filter(d -> d.getInvoiceDetailId().equals(detailId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("InvoiceDetail", detailId));

        detail.setQuantity(request.quantity());
        invoice.recalculateTotalAmount();
        invoiceRepository.save(invoice);
        return buildFullResponse(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.1 — Remove Item
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse removeItem(Long invoiceId, Long detailId) {
        Invoice invoice = loadEditableInvoice(invoiceId);
        InvoiceDetail detail = invoice.getDetails().stream()
                .filter(d -> d.getInvoiceDetailId().equals(detailId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("InvoiceDetail", detailId));

        invoice.removeDetail(detail);
        invoiceRepository.save(invoice);
        return buildFullResponse(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.3 — Pay Invoice (ATOMIC, @Transactional)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse payInvoice(Long invoiceId, PayInvoiceRequest request) {
        Invoice invoice = loadEditableInvoice(invoiceId);

        if (request.paymentMethod() == null) {
            throw new BusinessRuleViolationException("RULE_2", "Payment method is required to complete payment");
        }
        if (invoice.getDetails().isEmpty()) {
            throw new BusinessRuleViolationException("EMPTY_CART", "Cannot pay an empty invoice");
        }

        // Rule #1: Stock check with PESSIMISTIC_WRITE lock
        for (InvoiceDetail detail : invoice.getDetails()) {
            BranchInventoryId invKey = new BranchInventoryId(invoice.getBranchId(), detail.getProductId());
            BranchInventory inventory = entityManager.find(
                    BranchInventory.class, invKey, LockModeType.PESSIMISTIC_WRITE);

            if (inventory == null || inventory.getQtyAvailable().compareTo(detail.getQuantity()) < 0) {
                Product product = productRepo.findById(detail.getProductId()).orElse(null);
                String productName = product != null ? product.getProductName() : "ProductId=" + detail.getProductId();
                BigDecimal available = inventory != null ? inventory.getQtyAvailable() : BigDecimal.ZERO;
                throw new InsufficientStockException(detail.getProductId(), productName,
                        detail.getQuantity(), available);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Employee cashier = resolveCurrentEmployee();

        transitionStatus(invoice, InvoiceStatus.Paid);
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setPaidAt(now);

        for (InvoiceDetail detail : invoice.getDetails()) {
            BranchInventoryId invKey = new BranchInventoryId(invoice.getBranchId(), detail.getProductId());
            BranchInventory inventory = entityManager.find(BranchInventory.class, invKey,
                    LockModeType.PESSIMISTIC_WRITE);

            inventory.setQtyAvailable(inventory.getQtyAvailable().subtract(detail.getQuantity()));
            inventory.setQtyOnHand(inventory.getQtyOnHand().subtract(detail.getQuantity()));
            inventory.setUpdatedAt(now);

            InventoryTransactionHistory ith = InventoryTransactionHistory.builder()
                    .branch(branchRepository.findById(invoice.getBranchId()).orElse(null))
                    .product(productRepo.findById(detail.getProductId()).orElse(null))
                    .transactionType(InventoryTransactionType.Sale)
                    .referenceTable("Invoice")
                    .referenceId(invoice.getInvoiceId())
                    .quantityChange(detail.getQuantity().negate())
                    .reason("POS Sale")
                    .createdBy(employeeRepository.findById(cashier.getEmployeeId()).orElse(null))
                    .createdAt(now)
                    .build();
            ithRepo.save(ith);
        }

        invoiceRepository.save(invoice);
        log.info("Invoice {} paid via {}", invoice.getInvoiceCode(), request.paymentMethod());
        return buildFullResponse(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.4 — State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse holdInvoice(Long invoiceId) {
        Invoice invoice = loadEditableInvoice(invoiceId);
        transitionStatus(invoice, InvoiceStatus.Held);
        Employee cashier = resolveCurrentEmployee();
        return invoiceMapper.toSummaryResponse(invoiceRepository.save(invoice), cashier.getFullName());
    }

    @Override
    @Transactional
    public InvoiceResponse resumeInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        assertBranchOwnership(invoice);
        transitionStatus(invoice, InvoiceStatus.Draft);
        Employee cashier = resolveCurrentEmployee();
        return invoiceMapper.toSummaryResponse(invoiceRepository.save(invoice), cashier.getFullName());
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId) {
        Invoice invoice = loadEditableInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.Paid) {
            throw new InvalidStateTransitionException("Invoice", "Paid", "Canceled");
        }
        transitionStatus(invoice, InvoiceStatus.Canceled);
        invoice.setCanceledAt(LocalDateTime.now());
        Employee cashier = resolveCurrentEmployee();
        return invoiceMapper.toSummaryResponse(invoiceRepository.save(invoice), cashier.getFullName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.1.5 — List & Detail
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<InvoiceResponse> listInvoices(InvoiceSearchRequest request) {
        Employee currentEmployee = resolveCurrentEmployee();
        boolean isStaff = !hasRole("MANAGER") && !hasRole("ADMIN");
        Integer branchIdFilter = isStaff ? currentEmployee.getBranch().getBranchId() : null;

        // HSF302 Mục 3: dùng @Query JPQL (findByFilters) thay Specification API
        // Tham số null → bỏ qua filter (pattern đã học Chapter04)
        LocalDateTime fromDateTime = request.fromDate() != null ? request.fromDate().atStartOfDay() : null;
        LocalDateTime toDateTime = request.toDate() != null ? request.toDate().plusDays(1).atStartOfDay() : null;

        Sort sort = parseSort(request.sort());
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);
        Page<Invoice> invoicePage = invoiceRepository.findByFilters(
                request.status(), fromDateTime, toDateTime,
                request.cashierId(), branchIdFilter, pageable);

        // Batch load cashier names
        Set<Long> cashierIds = invoicePage.map(Invoice::getCashierId).toSet();
        Map<Long, String> cashierNames = employeeRepository.findAllById(cashierIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName));

        return invoicePage.map(inv ->
                invoiceMapper.toSummaryResponse(inv, cashierNames.get(inv.getCashierId())));
    }

    @Override
    public InvoiceResponse getInvoiceDetail(Long invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        assertBranchOwnership(invoice);
        return buildFullResponse(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String generateInvoiceCode(Branch branch, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        long count = invoiceRepository.countByBranchIdAndCreatedAtBetween(
                branch.getBranchId(), startOfDay, endOfDay);
        String seq = String.format("%06d", count + 1);
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("INV-%s-%s-%s", branch.getBranchCode(), dateStr, seq);
    }

    private BigDecimal resolveEffectivePrice(Product product, Integer branchId) {
        return priceRepo.findById(new BranchProductPriceId(branchId, product.getProductId()))
                .map(BranchProductPriceStub::getEffectivePrice)
                .orElse(product.getStandardPrice());
    }

    /**
     * Find best active promotion for a product.
     * Strategy: choose promotion with most recent StartDateTime among all active ones.
     * Rationale: most recently started = latest pricing intent.
     */
    private PromotionDetailStub findBestActivePromotion(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionDetailRepo.findAll().stream()
                .filter(pd -> pd.getProductId().equals(productId))
                .filter(pd -> pd.getPromotion() != null
                              && "Active".equalsIgnoreCase(pd.getPromotion().getStatus())
                              && !now.isBefore(pd.getPromotion().getStartDateTime())
                              && !now.isAfter(pd.getPromotion().getEndDateTime()))
                .max(Comparator.comparing(pd -> pd.getPromotion().getStartDateTime()))
                .orElse(null);
    }

    private BigDecimal applyPromotion(BigDecimal basePrice, PromotionDetailStub promo) {
        if (promo == null) return basePrice;
        BigDecimal discounted;
        if ("Percentage".equalsIgnoreCase(promo.getDiscountType())) {
            discounted = basePrice.multiply(BigDecimal.ONE.subtract(
                    promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
        } else {
            discounted = basePrice.subtract(promo.getDiscountValue());
        }
        return discounted.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private InvoiceResponse buildFullResponse(Invoice invoice) {
        Set<Long> productIds = invoice.getDetails().stream()
                .map(InvoiceDetail::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productRepo.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        String cashierName = employeeRepository.findById(invoice.getCashierId())
                .map(Employee::getFullName).orElse("N/A");
        return invoiceMapper.toResponse(invoice, productMap, cashierName);
    }

    private ProductSearchResponse buildProductSearchResponse(Product product, Integer branchId) {
        BigDecimal effectivePrice = resolveEffectivePrice(product, branchId);
        BranchInventory inv = inventoryRepo
                .findById(new BranchInventoryId(branchId, product.getProductId()))
                .orElse(null);
        BigDecimal qtyAvailable = inv != null ? inv.getQtyAvailable() : BigDecimal.ZERO;
        PromotionDetailStub promo = findBestActivePromotion(product.getProductId());
        return new ProductSearchResponse(
                product.getProductId(), product.getSku(), product.getProductName(),
                effectivePrice, qtyAvailable,
                promo != null ? promo.getPromotionId() : null,
                promo != null ? promo.getDiscountType() : null,
                promo != null ? promo.getDiscountValue() : null);
    }

    private Invoice loadEditableInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        assertBranchOwnership(invoice);
        if (invoice.getStatus() != InvoiceStatus.Draft && invoice.getStatus() != InvoiceStatus.Held) {
            throw new BusinessRuleViolationException("INVOICE_NOT_EDITABLE",
                    "Invoice " + invoice.getInvoiceCode() + " cannot be modified in status: " + invoice.getStatus());
        }
        return invoice;
    }

    private void transitionStatus(Invoice invoice, InvoiceStatus next) {
        if (!invoice.getStatus().canTransitionTo(next)) {
            throw new InvalidStateTransitionException("Invoice",
                    invoice.getStatus().name(), next.name());
        }
        invoice.setStatus(next);
    }

    private void assertBranchOwnership(Invoice invoice) {
        if (hasRole("ADMIN") || hasRole("MANAGER")) return;
        Employee emp = resolveCurrentEmployee();
        if (!invoice.getBranchId().equals(emp.getBranch().getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only access invoices from your own branch");
        }
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

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}



