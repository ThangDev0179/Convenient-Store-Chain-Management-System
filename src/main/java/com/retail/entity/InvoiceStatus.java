package com.retail.entity;

/**
 * Maps to Invoice.Status CHECK constraint in SQL Server.
 * Values MUST match the DB strings exactly.
 */
public enum InvoiceStatus {
    Draft,
    Held,
    Paid,
    Canceled;

    /**
     * State machine: validate if a transition from 'current' to 'next' is allowed.
     * Allowed transitions:
     *   Draft  → Held, Paid, Canceled
     *   Held   → Draft, Paid, Canceled
     *   Paid   → (none — use Refund instead)
     *   Canceled → (none — terminal state)
     */
    public boolean canTransitionTo(InvoiceStatus next) {
        return switch (this) {
            case Draft    -> next == Held || next == Paid || next == Canceled;
            case Held     -> next == Draft || next == Paid || next == Canceled;
            case Paid     -> false;
            case Canceled -> false;
        };
    }
}
