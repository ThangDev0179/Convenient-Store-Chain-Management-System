package com.retail.common.exception;

/**
 * Thrown when an attempt is made to transition an entity to an invalid state.
 * Examples: Paid → Draft, Canceled → Paid, etc.
 */
public class InvalidStateTransitionException extends BusinessRuleViolationException {

    private final String entityType;
    private final String fromState;
    private final String toState;

    public InvalidStateTransitionException(String entityType, String fromState, String toState) {
        super("INVALID_STATE_TRANSITION",
              String.format("Cannot transition %s from '%s' to '%s'", entityType, fromState, toState));
        this.entityType = entityType;
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getEntityType() { return entityType; }
    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
}
