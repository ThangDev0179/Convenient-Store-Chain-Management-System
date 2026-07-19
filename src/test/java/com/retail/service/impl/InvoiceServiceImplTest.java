package com.retail.service.impl;

import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.common.exception.InsufficientStockException;
import com.retail.common.exception.InvalidStateTransitionException;
import com.retail.common.exception.ResourceNotFoundException;
import com.retail.dto.*;
import com.retail.entity.*;
import com.retail.mapper.InvoiceMapper;
import com.retail.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceDetailRepository invoiceDetailRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ProductRepository productRepo;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private BranchInventoryRepository inventoryRepo;
    @Mock
    private InventoryTransactionHistoryRepository ithRepo;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Employee cashier;
    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setBranchId(100);

        Role role = new Role();
        role.setRoleCode(RoleCode.STAFF);

        cashier = new Employee();
        cashier.setEmployeeId(1L);
        cashier.setUsername("staff01");
        cashier.setBranch(branch);
        cashier.setRole(role);
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("staff01");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(employeeRepository.findByUsername("staff01")).thenReturn(Optional.of(cashier));
    }

    @Test
    void testCreateInvoice_Success() {
        // [TC-INV-01] Khởi tạo hóa đơn mới thành công
        mockSecurityContext();

        when(branchRepository.findById(100)).thenReturn(Optional.of(branch));
        when(invoiceRepository.countByBranchIdAndCreatedAtBetween(any(), any(), any())).thenReturn(5L);

        Invoice savedInvoice = new Invoice();
        savedInvoice.setInvoiceId(10L);
        savedInvoice.setInvoiceCode("INV-100-20230101-0006");
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        InvoiceResponse mockResponse = new InvoiceResponse(10L, "INV-100-20230101-0006", 100, null, null, null, null, null, null, null, null, null);
        lenient().when(invoiceMapper.toSummaryResponse(any(), any())).thenReturn(mockResponse);

        InvoiceResponse response = invoiceService.createInvoice();

        assertNotNull(response);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testAddItem_NegativeQuantity_ThrowsException() {
        // [TC-INV-02] Bắt lỗi Hack số lượng âm
        AddInvoiceItemRequest request = new AddInvoiceItemRequest(1L, new BigDecimal("-5"));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> invoiceService.addItem(10L, request)
        );

        assertEquals("INVALID_QUANTITY", exception.getRuleCode());
    }

    @Test
    void testAddItem_MergeQuantity_Success() {
        // [TC-INV-03] Gộp số lượng sản phẩm trùng
        mockSecurityContext(); // BUG FIX from previous run
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setStatus(InvoiceStatus.Draft);
        invoice.setBranchId(100);
        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));

        Product product = new Product();
        product.setProductId(1L);
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        InvoiceDetail existingDetail = new InvoiceDetail();
        existingDetail.setProductId(1L);
        existingDetail.setQuantity(new BigDecimal("2"));
        existingDetail.setUnitPrice(new BigDecimal("10000")); // Need price for recalculateTotalAmount
        when(invoiceDetailRepository.findByInvoice_InvoiceIdAndProductId(10L, 1L))
                .thenReturn(Optional.of(existingDetail));

        AddInvoiceItemRequest request = new AddInvoiceItemRequest(1L, new BigDecimal("3"));

        invoiceService.addItem(10L, request);

        assertEquals(new BigDecimal("5"), existingDetail.getQuantity());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void testPayInvoice_Success() {
        // [TC-INV-04] Thanh toán thành công (trừ tồn kho)
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        invoice.setStatus(InvoiceStatus.Draft);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("2"));
        detail.setUnitPrice(new BigDecimal("10000")); // Added price to fix NullPointer
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));

        BranchInventory inventory = new BranchInventory();
        inventory.setQtyAvailable(new BigDecimal("10"));
        inventory.setQtyOnHand(new BigDecimal("10"));
        
        when(entityManager.find(eq(BranchInventory.class), any(BranchInventoryId.class), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(inventory);

        PayInvoiceRequest request = new PayInvoiceRequest(PaymentMethod.Cash);
        invoiceService.payInvoice(10L, request);

        assertEquals(InvoiceStatus.Paid, invoice.getStatus());
        assertEquals(new BigDecimal("8"), inventory.getQtyAvailable());
        assertEquals(new BigDecimal("8"), inventory.getQtyOnHand());
        verify(ithRepo, times(1)).save(any(InventoryTransactionHistory.class));
    }

    @Test
    void testPayInvoice_InsufficientStock() {
        // [TC-INV-05] Thanh toán thất bại (thiếu tồn kho)
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        invoice.setStatus(InvoiceStatus.Draft);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("10"));
        detail.setUnitPrice(new BigDecimal("10000")); // Added price to fix NullPointer
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));

        BranchInventory inventory = new BranchInventory();
        inventory.setQtyAvailable(new BigDecimal("5")); // Only 5 in stock, need 10
        
        when(entityManager.find(eq(BranchInventory.class), any(BranchInventoryId.class), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(inventory);

        PayInvoiceRequest request = new PayInvoiceRequest(PaymentMethod.Cash);
        
        assertThrows(InsufficientStockException.class, () -> {
            invoiceService.payInvoice(10L, request);
        });
    }

    @Test
    void testStateTransitions() {
        // [TC-INV-06] Kiểm tra luồng trạng thái
        mockSecurityContext(); // BUG FIX from previous run
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        invoice.setStatus(InvoiceStatus.Paid); // Paid cannot be Canceled
        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleViolationException.class, () -> {
            invoiceService.cancelInvoice(10L);
        });
    }
}
