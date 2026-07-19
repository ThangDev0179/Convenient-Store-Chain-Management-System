# Hướng dẫn cập nhật Database Schema

Thư mục này chứa các script SQL cập nhật database schema của dự án nhằm đồng bộ với các sửa đổi và sửa lỗi nghiệp vụ trong code backend Java.

## Script khởi tạo Database ban đầu

### [bancuoi1807.sql](file:///H:/ky5/HSF302/ConvenientStoreChainManagementSystem/Convenient-Store-Chain-Management-System/db-updates/bancuoi1807.sql)
*   **Mô tả:** Script SQL khởi tạo toàn bộ cấu trúc cơ sở dữ liệu (`ConvenienceRetailDB`) theo thiết kế SRS (tailieunghiepvu_VER2.docx). Chứa các bảng về bảo mật, nhân viên, chi nhánh, nhà cung cấp, sản phẩm, và đơn đặt hàng.
*   **Cách chạy:** Mở SSMS (SQL Server Management Studio) và thực thi toàn bộ script này để tạo mới database và cấu trúc bảng.

## Danh sách các bản cập nhật bổ sung

### 1. Thêm trạng thái `Completed` cho đơn đặt hàng (PO)
*   **File script:** [update_po_status_completed.sql](file:///H:/ky5/HSF302/ConvenientStoreChainManagementSystem/Convenient-Store-Chain-Management-System/db-updates/update_po_status_completed.sql)
*   **Mô tả:** Thay đổi ràng buộc `CK_PO_Status` trên bảng `dbo.PurchaseOrder` để cho phép trạng thái `Completed` khi nhận đủ 100% hàng hóa từ nhà cung cấp.
*   **Cách chạy:** Mở SSMS (SQL Server Management Studio), kết nối tới DB `ConvenienceRetailDB` và chạy nội dung file SQL này.

---
*Lưu ý dành cho các AI Agent/Developer của thành viên khác: Vui lòng chạy các script trong thư mục này trước khi chạy ứng dụng Spring Boot.*
