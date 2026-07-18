package com.retail.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * Placeholder stub for Today's Revenue calculation.
 * This can be wired with InvoiceRepository later once the Invoice module is implemented.
 */
@Service
public class DashboardStatsProviderImpl implements DashboardStatsProvider {
    @Override
    public BigDecimal getTodayRevenue() {
        return BigDecimal.ZERO;
    }
}
