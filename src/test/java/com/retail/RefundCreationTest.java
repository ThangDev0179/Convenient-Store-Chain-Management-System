package com.retail;

import com.retail.dto.CreateRefundRequest;
import com.retail.dto.RefundItemRequest;
import com.retail.dto.RefundSearchRequest;
import com.retail.entity.ConditionType;
import com.retail.entity.Employee;
import com.retail.repository.EmployeeRepository;
import com.retail.security.CustomUserDetails;
import com.retail.service.RefundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
public class RefundCreationTest {

    @Autowired
    private RefundService refundService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @Transactional
    public void testCreateAndListRefund() {
        System.out.println("=================================================");
        System.out.println("STARTING CREATE AND LIST REFUND INTEGRATION TEST...");
        System.out.println("=================================================");

        // Setup security context
        Employee employee = employeeRepository.findByUsername("NV-2026-0003")
                .orElseThrow(() -> new RuntimeException("NV-2026-0003 not found"));
        CustomUserDetails userDetails = new CustomUserDetails(employee);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 1. Create a refund for Invoice 15
        CreateRefundRequest createRequest = new CreateRefundRequest(
                15L,
                "Nguyen Van A",
                "0912345678",
                "San pham loi",
                List.of(new RefundItemRequest(82L, BigDecimal.ONE, ConditionType.Resalable))
        );

        var created = refundService.createRefund(createRequest);
        System.out.println("1. Refund created successfully! Code: " + created.refundCode());

        // 2. Query the list of refunds
        RefundSearchRequest searchRequest = new RefundSearchRequest(null, null, null, null, 0, 10);
        var page = refundService.listRefunds(searchRequest);

        System.out.println("2. Listing refunds returned " + page.getContent().size() + " items.");
        for (var r : page.getContent()) {
            System.out.println("   - Found Refund Code: " + r.refundCode() + " for invoice: " + r.originalInvoiceCode());
        }

        // Verify that the page contains the created refund
        assertFalse(page.getContent().isEmpty(), "Refund list should not be empty after creation!");
        assertEquals(created.refundCode(), page.getContent().get(0).refundCode());
        System.out.println("INTEGRATION TEST PASSED SUCCESSFULLY!");
        System.out.println("=================================================");
    }
}
