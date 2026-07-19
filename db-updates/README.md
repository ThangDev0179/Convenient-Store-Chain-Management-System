# Hướng dẫn cập nhật Database Schema

Thư mục này chứa các script SQL cập nhật database schema của dự án nhằm đồng bộ với các sửa đổi và sửa lỗi nghiệp vụ trong code backend Java.

## Danh sách các bản cập nhật

### 1. Thêm trạng thái `Completed` cho đơn đặt hàng (PO)
*   **File script:** [update_po_status_completed.sql](file:///H:/ky5/HSF302/ConvenientStoreChainManagementSystem/Convenient-Store-Chain-Management-System/db-updates/update_po_status_completed.sql)
*   **Mô tả:** Thay đổi ràng buộc `CK_PO_Status` trên bảng `dbo.PurchaseOrder` để cho phép trạng thái `Completed` khi nhận đủ 100% hàng hóa từ nhà cung cấp.
*   **Cách chạy:** Mở SSMS (SQL Server Management Studio), kết nối tới DB `ConvenienceRetailDB` và chạy nội dung file SQL này.

---
*Lưu ý dành cho các AI Agent/Developer của thành viên khác: Vui lòng chạy các script trong thư mục này trước khi chạy ứng dụng Spring Boot.*
