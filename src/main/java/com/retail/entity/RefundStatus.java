package com.retail.entity;

/**
 * Maps to Refund.Status CHECK constraint in SQL Server.
 * Values MUST match the DB strings exactly.
 */
public enum RefundStatus {
    Draft,
    Pending_Approval,
    Completed,
    Rejected;

    /**
     * State machine transitions:
     *   Draft            → Pending_Approval, Completed, Rejected
     *   Pending_Approval → Completed, Rejected
     *   Completed        → (terminal)
     *   Rejected         → (terminal)
     */
    public boolean canTransitionTo(RefundStatus next) {
        return switch (this) {
            case Draft            -> next == Pending_Approval || next == Completed || next == Rejected;
            case Pending_Approval -> next == Completed || next == Rejected;
            case Completed        -> false;
            case Rejected         -> false;
        };
    }
}
