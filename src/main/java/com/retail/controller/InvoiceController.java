package com.retail.controller;

import com.retail.dto.*;
import com.retail.dto.InvoiceResponse;
import com.retail.dto.ProductSearchResponse;
import com.retail.entity.InvoiceStatus;
import com.retail.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

/**
 * Controller for POS (Invoice) module.
 *
 * Security:
 *   - All POS operations require authentication (configured in SecurityConfig).
 *   - List/detail: STAFF can only see own branch (enforced in Service, not just annotation).
 *   - STAFF can create/edit invoices; branch ownership checked in Service.
 *
 * URL structure:
 *   GET  /pos                           â†’ redirect to POS screen or invoice list
 *   GET  /pos/invoices                  â†’ list invoices (Thymeleaf)
 *   GET  /pos/invoices/new              â†’ POS screen (create + cart)
 *   GET  /pos/invoices/{id}             â†’ invoice detail
 *   POST /pos/invoices                  â†’ create new invoice
 *   POST /pos/invoices/{id}/items       â†’ add item to cart
 *   PUT  /pos/invoices/{id}/items/{did} â†’ update item quantity
 *   DELETE /pos/invoices/{id}/items/{did} â†’ remove item
 *   PUT  /pos/invoices/{id}/pay         â†’ pay invoice
 *   PUT  /pos/invoices/{id}/hold        â†’ hold invoice
 *   PUT  /pos/invoices/{id}/resume      â†’ resume held invoice
 *   PUT  /pos/invoices/{id}/cancel      â†’ cancel invoice
 *   GET  /pos/products/search           â†’ product search (AJAX JSON)
 */
@Controller
@RequestMapping("/pos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public String posHome() {
        return "redirect:/pos/invoices/new";
    }

    // ─── List page ───────────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String listInvoices(@ModelAttribute InvoiceSearchRequest request, Model model) {
        Page<InvoiceResponse> page = invoiceService.listInvoices(request);
        model.addAttribute("invoicePage", page);
        model.addAttribute("searchRequest", request);
        model.addAttribute("statuses", InvoiceStatus.values());
        return "pos/invoice-list";
    }

    // ─── POS screen ───────────────────────────────────────────────────────────────

    @GetMapping("/invoices/new")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String posScreen(@RequestParam(required = false) Long invoiceId, Model model) {
        model.addAttribute("paymentMethods",
                Arrays.asList(com.retail.entity.PaymentMethod.values()));
        model.addAttribute("preloadedInvoiceId", invoiceId);
        return "pos/index";
    }

    // ─── Detail ──────────────────────────────────────────────────────────────────

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public String invoiceDetail(@PathVariable Long id, Model model) {
        InvoiceResponse invoice = invoiceService.getInvoiceDetail(id);
        model.addAttribute("invoice", invoice);
        return "pos/invoice-detail";
    }

    @GetMapping("/invoices/{id}/json")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> getInvoiceJson(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceDetail(id));
    }

    @GetMapping("/invoices/by-invoice-code")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> lookupInvoice(@RequestParam String invoiceCode) {
        // Need to add getByCode in InvoiceService? Actually wait, InvoiceService doesn't have getByCode yet.
        // Let's check InvoiceServiceImpl to see if it has a way to get by code. 
        // InvoiceRepository has findByInvoiceCode. I need to add getByCode to InvoiceService.
        // Let's do that next.
        return ResponseEntity.ok(invoiceService.getByCode(invoiceCode));
    }

    // â”€â”€â”€ Create invoice (POST, returns JSON for AJAX) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> createInvoice() {
        return ResponseEntity.ok(invoiceService.createInvoice());
    }

    // â”€â”€â”€ Add item (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/invoices/{id}/items")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> addItem(@PathVariable Long id,
                                                    @Valid @RequestBody AddInvoiceItemRequest request) {
        return ResponseEntity.ok(invoiceService.addItem(id, request));
    }

    // â”€â”€â”€ Update item (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/invoices/{id}/items/{detailId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> updateItem(@PathVariable Long id,
                                                       @PathVariable Long detailId,
                                                       @Valid @RequestBody UpdateInvoiceItemRequest request) {
        return ResponseEntity.ok(invoiceService.updateItem(id, detailId, request));
    }

    // â”€â”€â”€ Remove item (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @DeleteMapping("/invoices/{id}/items/{detailId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> removeItem(@PathVariable Long id,
                                                       @PathVariable Long detailId) {
        return ResponseEntity.ok(invoiceService.removeItem(id, detailId));
    }

    // â”€â”€â”€ Pay (AJAX JSON) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable Long id,
                                                       @Valid @RequestBody PayInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, request));
    }

    // â”€â”€â”€ Hold â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/invoices/{id}/hold")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> holdInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.holdInvoice(id));
    }

    // â”€â”€â”€ Resume â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/invoices/{id}/resume")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> resumeInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.resumeInvoice(id));
    }

    // â”€â”€â”€ Cancel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id));
    }

    // â”€â”€â”€ Product search (AJAX JSON for POS barcode scan / autocomplete) â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/products/search")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    @ResponseBody
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceService.searchProducts(keyword, sku, branchId, page, size));
    }
}
