package com.retail.service.impl;

import com.retail.entity.*;
import com.retail.exception.ValidationException;
import com.retail.repository.*;
import com.retail.service.InvoiceService;
import com.retail.service.BranchPriceService;
import com.retail.service.PromotionService;
import com.retail.service.InventoryTransactionService;
import com.retail.service.AuditLogService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository detailRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchPriceService branchPriceService;
    private final PromotionService promotionService;
    private final InventoryTransactionService transactionService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Invoice createDraftInvoice(Integer branchId, Long cashierId) {
        Branch branch = entityManager.find(Branch.class, branchId);
        if (branch == null) {
            throw new ValidationException("Chi nhánh không tồn tại");
        }

        // Generate InvoiceCode: INV-[BranchCode]-YYYYMMDD-[6 số]
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "INV-" + branch.getBranchCode() + "-" + dateStr + "-";

        String maxCode = invoiceRepository.findMaxInvoiceCodeByBranchAndDate(branch.getBranchCode(), dateStr);
        int nextSeq = 1;
        if (maxCode != null && maxCode.startsWith(prefix) && maxCode.length() == prefix.length() + 6) {
            try {
                String seqStr = maxCode.substring(prefix.length());
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {}
        }
        String invoiceCode = prefix + String.format("%06d", nextSeq);

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .branch(branch)
                .cashier(entityManager.getReference(Employee.class, cashierId))
                .status("Draft")
                .totalAmount(BigDecimal.ZERO)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.logAction(cashierId, "CreateInvoice", "Invoice",
                saved.getInvoiceId(), null, "Draft", "Tạo hóa đơn bán lẻ nháp", null, null);
        return saved;
    }

    @Override
    @Transactional
    public Invoice addDetail(Long invoiceId, Long productId, BigDecimal quantity) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Hóa đơn không tồn tại"));

        if (!"Draft".equals(invoice.getStatus()) && !"Held".equals(invoice.getStatus())) {
            throw new ValidationException("Chỉ được chỉnh sửa hóa đơn ở trạng thái Draft hoặc Held");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Số lượng sản phẩm phải lớn hơn 0");
        }

        Product product = entityManager.find(Product.class, productId);
        if (product == null) {
            throw new ValidationException("Sản phẩm không tồn tại");
        }

        // Tính giá bán sau khuyến mãi
        BigDecimal effectivePrice = branchPriceService.getEffectivePrice(invoice.getBranch().getBranchId(), productId);
        PromotionDetail bestPromo = promotionService.getBestActivePromotion(productId, effectivePrice);
        BigDecimal unitPrice = effectivePrice;
        Promotion promotion = null;

        if (bestPromo != null) {
            promotion = bestPromo.getPromotion();
            BigDecimal discount = BigDecimal.ZERO;
            if ("Percentage".equals(bestPromo.getDiscountType())) {
                discount = effectivePrice.multiply(bestPromo.getDiscountValue()).divide(new BigDecimal("100"));
            } else if ("FixedAmount".equals(bestPromo.getDiscountType())) {
                discount = bestPromo.getDiscountValue();
            }
            unitPrice = effectivePrice.subtract(discount);
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                unitPrice = BigDecimal.ZERO;
            }
        }

        InvoiceDetail detail = InvoiceDetail.builder()
                .invoice(invoice)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .promotion(promotion)
                .build();

        invoice.addDetail(detail);

        // Cập nhật tổng tiền hóa đơn nháp
        BigDecimal currentTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal lineTotal = quantity.multiply(unitPrice);
        invoice.setTotalAmount(currentTotal.add(lineTotal));

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public Invoice checkout(Long invoiceId, String paymentMethod) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Hóa đơn không tồn tại"));

        if (!"Draft".equals(invoice.getStatus()) && !"Held".equals(invoice.getStatus())) {
            throw new ValidationException("Chỉ hóa đơn Draft hoặc Held mới được thanh toán");
        }

        if (paymentMethod == null || (!"Cash".equals(paymentMethod) && !"QR".equals(paymentMethod) && !"Bank".equals(paymentMethod) && !"Card".equals(paymentMethod))) {
            throw new ValidationException("Phương thức thanh toán không hợp lệ (Cash | QR | Bank | Card)");
        }

        if (invoice.getDetails().isEmpty()) {
            throw new ValidationException("Hóa đơn không có sản phẩm nào");
        }

        Integer branchId = invoice.getBranch().getBranchId();

        // Kiểm tra tồn kho khả dụng và trừ kho cho từng sản phẩm
        for (InvoiceDetail detail : invoice.getDetails()) {
            Long productId = detail.getProduct().getProductId();
            BigDecimal qty = detail.getQuantity();

            BranchInventory inv = branchInventoryRepository.findByBranchBranchIdAndProductProductId(branchId, productId)
                    .orElseThrow(() -> new ValidationException("Sản phẩm ID " + productId + " không tồn tại trong kho của chi nhánh này."));

            if (inv.getQtyAvailable().compareTo(qty) < 0) {
                throw new ValidationException("Sản phẩm '" + detail.getProduct().getProductName() + "' không đủ tồn kho khả dụng. Tồn khả dụng hiện tại: " + inv.getQtyAvailable() + ", Cần bán: " + qty);
            }

            // Trừ kho vật lý và khả dụng bán hàng
            transactionService.recordTransaction(
                    branchId, productId,
                    qty.negate(), qty.negate(), BigDecimal.ZERO,
                    InventoryTransactionType.Sale,
                    "Invoice", invoiceId,
                    "Bán lẻ POS hóa đơn: " + invoice.getInvoiceCode(),
                    invoice.getCashier().getEmployeeId()
            );
        }

        invoice.setStatus("Paid");
        invoice.setPaymentMethod(paymentMethod);
        invoice.setPaidAt(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.logAction(invoice.getCashier().getEmployeeId(), "CheckoutInvoice", "Invoice",
                saved.getInvoiceId(), "Draft", "Paid", "Thanh toán hóa đơn thành công", null, null);
        return saved;
    }

    @Override
    @Transactional
    public Invoice cancel(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Hóa đơn không tồn tại"));

        if (!"Draft".equals(invoice.getStatus()) && !"Held".equals(invoice.getStatus())) {
            throw new ValidationException("Chỉ hóa đơn Draft hoặc Held mới được hủy");
        }

        String oldStatus = invoice.getStatus();
        invoice.setStatus("Canceled");
        invoice.setCanceledAt(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.logAction(invoice.getCashier().getEmployeeId(), "CancelInvoice", "Invoice",
                saved.getInvoiceId(), oldStatus, "Canceled", "Hủy bỏ hóa đơn nháp", null, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getDetail(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ValidationException("Hóa đơn không tồn tại"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByBranch(Integer branchId) {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getBranch().getBranchId().equals(branchId))
                .toList();
    }
}
