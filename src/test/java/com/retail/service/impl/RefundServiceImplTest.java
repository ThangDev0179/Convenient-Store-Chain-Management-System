package com.retail.service.impl;

import com.retail.common.exception.BusinessRuleViolationException;
import com.retail.common.exception.InvalidStateTransitionException;
import com.retail.dto.*;
import com.retail.entity.*;
import com.retail.mapper.RefundMapper;
import com.retail.repository.*;
import com.retail.validator.RefundValidator;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private RefundDetailRepository refundDetailRepository;
    @Mock
    private RefundMapper refundMapper;
    @Mock
    private RefundValidator refundValidator;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductRepository productRepo;
    @Mock
    private BranchInventoryRepository inventoryRepo;
    @Mock
    private InventoryTransactionHistoryRepository ithRepo;

    @InjectMocks
    private RefundServiceImpl refundService;

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
    void testCreateRefund_UnderThreshold_AutoApprove() {
        // [TC-REF-01] Auto-approve dưới ngưỡng (200k)
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("1"));
        detail.setUnitPrice(new BigDecimal("150000")); // < 200,000
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));
        when(branchRepository.findById(100)).thenReturn(Optional.of(branch));
        
        BranchInventory inventory = new BranchInventory();
        inventory.setQtyAvailable(new BigDecimal("10"));
        inventory.setQtyOnHand(new BigDecimal("10"));
        // Using explicit cast to avoid ambiguity with Object, Map
        when(entityManager.find(eq(BranchInventory.class), any(BranchInventoryId.class), (LockModeType) any()))
                .thenReturn(inventory);
                
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
            Refund r = i.getArgument(0);
            r.setRefundId(1L);
            return r;
        });

        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(1L, new BigDecimal("1"), ConditionType.Resalable)
        );
        CreateRefundRequest request = new CreateRefundRequest(10L, "John", "012", "Test", items);

        refundService.createRefund(request);

        // Since it's < 200k, it should auto-approve and restock
        assertEquals(new BigDecimal("11"), inventory.getQtyAvailable());
        assertEquals(new BigDecimal("11"), inventory.getQtyOnHand());
    }

    @Test
    void testCreateRefund_OverThreshold_PendingApproval() {
        // [TC-REF-02] Pending_Approval trên ngưỡng
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("1"));
        detail.setUnitPrice(new BigDecimal("250000")); // > 200,000
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));
        when(branchRepository.findById(100)).thenReturn(Optional.of(branch));
        
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
            Refund r = i.getArgument(0);
            assertEquals(RefundStatus.Pending_Approval, r.getStatus());
            return r;
        });

        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(1L, new BigDecimal("1"), ConditionType.Resalable)
        );
        CreateRefundRequest request = new CreateRefundRequest(10L, "John", "012", "Test", items);

        refundService.createRefund(request);
        
        // Ensure no inventory changes during Pending state
        verify(entityManager, never()).find(eq(BranchInventory.class), any(), (LockModeType) any());
    }

    @Test
    void testCreateRefund_DuplicateProducts_ThrowsException() {
        // [TC-REF-03] Bắt lỗi sản phẩm trùng lặp
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("5"));
        detail.setUnitPrice(new BigDecimal("10000"));
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));
        when(branchRepository.findById(100)).thenReturn(Optional.of(branch));

        // Duplicate item 1L in request
        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(1L, new BigDecimal("1"), ConditionType.Resalable),
                new RefundItemRequest(1L, new BigDecimal("2"), ConditionType.Resalable)
        );
        CreateRefundRequest request = new CreateRefundRequest(10L, "John", "012", "Test", items);

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> refundService.createRefund(request)
        );

        assertEquals("DUPLICATE_PRODUCT", exception.getRuleCode());
    }

    @Test
    void testCreateRefund_DamagedItem() {
        // [TC-REF-04] Xử lý hàng hư hỏng (Damaged)
        mockSecurityContext();
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(10L);
        invoice.setBranchId(100);
        InvoiceDetail detail = new InvoiceDetail();
        detail.setProductId(1L);
        detail.setQuantity(new BigDecimal("1"));
        detail.setUnitPrice(new BigDecimal("150000")); // < 200,000 -> auto approve
        invoice.addDetail(detail);

        when(invoiceRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(invoice));
        when(branchRepository.findById(100)).thenReturn(Optional.of(branch));
        
        BranchInventory inventory = new BranchInventory();
        inventory.setQtyAvailable(new BigDecimal("10"));
        inventory.setQtyOnHand(new BigDecimal("10"));
        when(entityManager.find(eq(BranchInventory.class), any(BranchInventoryId.class), (LockModeType) any()))
                .thenReturn(inventory);
                
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> {
            Refund r = i.getArgument(0);
            r.setRefundId(1L);
            return r;
        });

        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(1L, new BigDecimal("1"), ConditionType.Damaged) // DAMAGED
        );
        CreateRefundRequest request = new CreateRefundRequest(10L, "John", "012", "Test", items);

        refundService.createRefund(request);

        // Damaged: QtyAvailable should remain 10, QtyOnHand increases to 11
        assertEquals(new BigDecimal("10"), inventory.getQtyAvailable());
        assertEquals(new BigDecimal("11"), inventory.getQtyOnHand());
    }

    @Test
    void testOverrideApprove_InvalidPin() {
        // [TC-REF-05] Override approve (Sai PIN)
        Employee manager = new Employee();
        manager.setEmployeeId(2L);
        manager.setUsername("manager01");
        Role mRole = new Role();
        mRole.setRoleCode(RoleCode.MANAGER);
        manager.setRole(mRole);
        manager.setPasswordHash("hashed_pin_1234");
        
        when(employeeRepository.findByUsername("manager01")).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("0000", "hashed_pin_1234")).thenReturn(false);
        
        RefundOverrideApproveRequest request = new RefundOverrideApproveRequest("manager01", "0000");

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> refundService.overrideApprove(1L, request)
        );

        assertEquals("INVALID_PIN", exception.getRuleCode());
    }
}
