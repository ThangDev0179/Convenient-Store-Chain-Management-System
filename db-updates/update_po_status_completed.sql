-- ============================================================================
-- SCRIPT CẬP NHẬT DATABASE SCHEMA (DATABASE MIGRATION)
-- Mục tiêu: Bổ sung trạng thái 'Completed' cho vòng đời đơn đặt hàng PurchaseOrder.
-- Chạy script này trên SQL Server (ConvenienceRetailDB) trước khi khởi động ứng dụng Java.
-- ============================================================================

-- 1. Xóa ràng buộc CHECK cũ trên bảng dbo.PurchaseOrder
ALTER TABLE dbo.PurchaseOrder DROP CONSTRAINT CK_PO_Status;
GO

-- 2. Tạo lại ràng buộc CHECK mới bổ sung thêm trạng thái N'Completed'
ALTER TABLE dbo.PurchaseOrder 
    ADD CONSTRAINT CK_PO_Status CHECK (Status IN (N'Draft', N'Submitted', N'Partially_Received', N'Received_Partial', N'Completed', N'Canceled'));
GO

PRINT 'Cập nhật ràng buộc CK_PO_Status thành công! Đã bổ sung trạng thái Completed.';
GO
