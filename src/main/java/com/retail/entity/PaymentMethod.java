package com.retail.entity;

/**
 * Maps to Invoice.PaymentMethod CHECK constraint in SQL Server.
 * Values MUST match the DB strings exactly.
 * PaymentMethod is NULL until Invoice reaches Paid status.
 */
public enum PaymentMethod {
    Cash,
    QR,
    Bank,
    Card
}
