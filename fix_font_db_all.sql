
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
USE ConvenienceRetailDB;
GO

-- 1. Fix Supplier table
UPDATE dbo.Supplier SET SupplierName = N'Công ty TNHH Nước Giải Khát Coca-Cola Việt Nam', Address = N'Xa lộ Hà Nội, Phường Linh Trung, TP. Thủ Đức, TP.HCM' WHERE SupplierId = 1;
UPDATE dbo.Supplier SET SupplierName = N'Công ty Cổ phần Acecook Việt Nam', Address = N'Lô II-3, Đường số 11, KCN Tân Bình, Quận Tân Phú, TP.HCM' WHERE SupplierId = 2;
UPDATE dbo.Supplier SET SupplierName = N'Công ty TNHH Suntory PepsiCo Việt Nam', Address = N'Tầng 5, Tòa nhà Sheraton, Quận 1, TP.HCM' WHERE SupplierId = 3;

-- 2. Fix Branch table
UPDATE dbo.Branch SET BranchName = N'Cửa hàng tiện lợi Quận 1', Address = N'123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM' WHERE BranchId = 1;
UPDATE dbo.Branch SET BranchName = N'Chi nhánh Trung tâm', Address = N'456 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM' WHERE BranchId = 10;
UPDATE dbo.Branch SET BranchName = N'Chi nhánh Quận 2', Address = N'789 Thảo Điền, Phường Thảo Điền, TP. Thủ Đức, TP.HCM' WHERE BranchId = 11;

-- 3. Fix Role table
UPDATE dbo.Role SET RoleName = N'Administrator' WHERE RoleCode = 'ADMIN';
UPDATE dbo.Role SET RoleName = N'Quản lý cửa hàng' WHERE RoleCode = 'MANAGER';
UPDATE dbo.Role SET RoleName = N'Nhân viên' WHERE RoleCode = 'STAFF';

-- 4. Fix Employee table
UPDATE dbo.Employee SET FullName = N'Nguyễn Văn Admin' WHERE EmployeeCode = 'NV-2026-0001';
UPDATE dbo.Employee SET FullName = N'Trần Thị Manager' WHERE EmployeeCode = 'NV-2026-0002';
UPDATE dbo.Employee SET FullName = N'Lê Văn Nhân Viên' WHERE EmployeeCode = 'NV-2026-0003';
UPDATE dbo.Employee SET FullName = N'Nguyễn Văn Quản Lý 2' WHERE Username = 'manager2';

-- 5. Fix ProductCategory table
UPDATE dbo.ProductCategory SET CategoryName = N'Nước có ga' WHERE CategoryId = 1;
UPDATE dbo.ProductCategory SET CategoryName = N'Đồ khô & Mì gói' WHERE CategoryId = 2;
UPDATE dbo.ProductCategory SET CategoryName = N'Đồ uống' WHERE CategoryId = 3;
UPDATE dbo.ProductCategory SET CategoryName = N'Đồ ăn vặt & Bánh kẹo' WHERE CategoryId = 4;

-- 6. Fix Product table
UPDATE dbo.Product SET ProductName = N'Coca-Cola Lon 320ml' WHERE ProductId = 1;
UPDATE dbo.Product SET ProductName = N'Mì Hảo Hảo Tôm Chua Cay 75g' WHERE ProductId = 2;
UPDATE dbo.Product SET ProductName = N'Coca Cola 330ml' WHERE ProductId = 3;
UPDATE dbo.Product SET ProductName = N'Pepsi 330ml' WHERE ProductId = 4;
UPDATE dbo.Product SET ProductName = N'Snack Khoai Tây Lay''s 54g' WHERE ProductId = 5;
UPDATE dbo.Product SET ProductName = N'Coca-Cola Zero Lon 320ml' WHERE ProductId = 6;
UPDATE dbo.Product SET ProductName = N'Pepsi Không Calo Lon 320ml' WHERE ProductId = 7;
UPDATE dbo.Product SET ProductName = N'7Up Lon 320ml' WHERE ProductId = 8;
UPDATE dbo.Product SET ProductName = N'Sprite Lon 320ml' WHERE ProductId = 9;
UPDATE dbo.Product SET ProductName = N'Mì Ly Modern Lẩu Thái 65g' WHERE ProductId = 10;
UPDATE dbo.Product SET ProductName = N'Mì Kokomi 90 Tôm Chua Cay' WHERE ProductId = 11;
UPDATE dbo.Product SET ProductName = N'Phở Đệ Nhất Gà 65g' WHERE ProductId = 12;
UPDATE dbo.Product SET ProductName = N'Nước Suối Aquafina 500ml' WHERE ProductId = 13;
UPDATE dbo.Product SET ProductName = N'Trà Xanh Không Độ 500ml' WHERE ProductId = 14;
UPDATE dbo.Product SET ProductName = N'Sữa Tươi Vinamilk Ít Đường 180ml' WHERE ProductId = 15;
UPDATE dbo.Product SET ProductName = N'Trà Sữa Kirin Latte 345ml' WHERE ProductId = 16;
UPDATE dbo.Product SET ProductName = N'Snack Oishi Bò Bít Tết 40g' WHERE ProductId = 17;
UPDATE dbo.Product SET ProductName = N'Bánh Choco-Pie Orion 2 Cái' WHERE ProductId = 18;
UPDATE dbo.Product SET ProductName = N'Kẹo Dẻo Haribo Goldbears 80g' WHERE ProductId = 19;
UPDATE dbo.Product SET ProductName = N'Bánh Quy Oreo Socola 133g' WHERE ProductId = 20;

-- 7. Fix ProductUOM table
UPDATE dbo.ProductUOM SET UomName = N'Lon' WHERE UomId = 1;
UPDATE dbo.ProductUOM SET UomName = N'Gói' WHERE UomId = 2;
UPDATE dbo.ProductUOM SET UomName = N'Chai' WHERE UomId = 3;
