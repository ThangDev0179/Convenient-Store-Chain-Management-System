package com.retail.entity;

/**
 * Maps to RefundDetail.ConditionType CHECK constraint in SQL Server.
 * Values MUST match the DB strings exactly: Damaged | Resalable.
 *
 * Inventory impact (3.2.3):
 *   Resalable → cộng QtyAvailable + QtyOnHand
 *   Damaged   → chỉ cộng QtyOnHand (hàng hỏng không bán lại được);
 *               module Disposal (thành viên 5) sẽ xử lý xuất hủy sau.
 */
public enum ConditionType {
    Damaged,
    Resalable
}
