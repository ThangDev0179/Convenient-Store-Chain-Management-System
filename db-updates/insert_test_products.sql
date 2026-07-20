-- Script to seed test products for POS cashier testing
-- Target DB: ConvenienceRetailDB
-- Checks for duplicates before inserting.

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET QUOTED_IDENTIFIER ON;
GO

USE ConvenienceRetailDB;
GO

BEGIN TRANSACTION;

DECLARE @NewProductId BIGINT;

-- ==========================================
-- CATEGORY 1: NƯỚC CÓ GA (NC) - CategoryId 1
-- ==========================================

-- 1. Coca-Cola Zero Lon 320ml (NC-0002)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'NC-0002' OR Barcode = N'8935049500022')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'NC-0002', N'Coca-Cola Zero Lon 320ml', 1, 10500.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500022', N'Nước giải khát Coca-Cola không đường, không calo.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Lon', 1.0000, 1, N'8935049500022', 10500.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 500.000, 500.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 500.000, 500.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 500.000, 500.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Coca-Cola Zero Lon 320ml';
END

-- 2. Pepsi Không Calo Lon 320ml (NC-0003)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'NC-0003' OR Barcode = N'8935049500039')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'NC-0003', N'Pepsi Không Calo Lon 320ml', 1, 10500.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500039', N'Nước ngọt Pepsi vị chanh không calo.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Lon', 1.0000, 1, N'8935049500039', 10500.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 400.000, 400.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 400.000, 400.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 400.000, 400.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Pepsi Không Calo Lon 320ml';
END

-- 3. 7Up Lon 320ml (NC-0004)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'NC-0004' OR Barcode = N'8935049500046')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'NC-0004', N'7Up Lon 320ml', 1, 10000.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500046', N'Nước giải khát hương chanh tự nhiên.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Lon', 1.0000, 1, N'8935049500046', 10000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: 7Up Lon 320ml';
END

-- 4. Sprite Lon 320ml (NC-0005)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'NC-0005' OR Barcode = N'8935049500053')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'NC-0005', N'Sprite Lon 320ml', 1, 10000.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500053', N'Nước giải khát Sprite hương chanh.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Lon', 1.0000, 1, N'8935049500053', 10000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 350.000, 350.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 350.000, 350.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 350.000, 350.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Sprite Lon 320ml';
END


-- ==========================================
-- CATEGORY 2: ĐỒ KHÔ (DK) - CategoryId 2
-- ==========================================

-- 5. Mì Ly Modern Lẩu Thái 65g (DK-0002)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DK-0002' OR Barcode = N'8935049500121')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DK-0002', N'Mì Ly Modern Lẩu Thái 65g', 2, 8000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500121', N'Mì ly ăn liền hương vị lẩu thái chua cay.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Ly', 1.0000, 1, N'8935049500121', 8000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 150.000, 150.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 150.000, 150.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 150.000, 150.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Mì Ly Modern Lẩu Thái';
END

-- 6. Mì Kokomi 90 Tôm Chua Cay (DK-0003)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DK-0003' OR Barcode = N'8935049500138')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DK-0003', N'Mì Kokomi 90 Tôm Chua Cay', 2, 3500.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500138', N'Mì gói ăn liền Kokomi đại 90g dai ngon.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Gói', 1.0000, 1, N'8935049500138', 3500.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Mì Kokomi 90 Tôm Chua Cay';
END

-- 7. Phở Đệ Nhất Gà (DK-0004)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DK-0004' OR Barcode = N'8935049500145')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DK-0004', N'Phở Đệ Nhất Gà', 2, 7000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500145', N'Phở Đệ Nhất vị gà thơm ngon thanh ngọt.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Gói', 1.0000, 1, N'8935049500145', 7000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Phở Đệ Nhất Gà';
END


-- ==========================================
-- CATEGORY 3: ĐỒ UỐNG (DRK) - CategoryId 3
-- ==========================================

-- 8. Nước Suối Aquafina 500ml (DRK-003)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DRK-003' OR Barcode = N'8935049500213')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DRK-003', N'Nước Suối Aquafina 500ml', 3, 5000.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500213', N'Nước tinh khiết Aquafina 500ml.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Chai', 1.0000, 1, N'8935049500213', 5000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 800.000, 800.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 800.000, 800.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 800.000, 800.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Nước Suối Aquafina 500ml';
END

-- 9. Trà Xanh Không Độ 500ml (DRK-004)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DRK-004' OR Barcode = N'8935049500220')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DRK-004', N'Trà Xanh Không Độ 500ml', 3, 9000.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500220', N'Trà xanh đóng chai Không Độ thanh lọc cơ thể.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Chai', 1.0000, 1, N'8935049500220', 9000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 450.000, 450.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 450.000, 450.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 450.000, 450.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Trà Xanh Không Độ 500ml';
END

-- 10. Sữa Tươi Vinamilk Ít Đường 180ml (DRK-005)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DRK-005' OR Barcode = N'8935049500237')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DRK-005', N'Sữa Tươi Vinamilk Ít Đường 180ml', 3, 7500.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500237', N'Sữa tươi tiệt trùng Vinamilk 100% hộp giấy.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Hộp', 1.0000, 1, N'8935049500237', 7500.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 600.000, 600.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Sữa Tươi Vinamilk Ít Đường';
END

-- 11. Trà Sữa Kirin Latte 345ml (DRK-006)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'DRK-006' OR Barcode = N'8935049500244')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'DRK-006', N'Trà Sữa Kirin Latte 345ml', 3, 12000.00, 1, N'Active', SYSUTCDATETIME(), N'8935049500244', N'Trà sữa Latte đóng chai Kirin Nhật Bản.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Chai', 1.0000, 1, N'8935049500244', 12000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Trà Sữa Kirin Latte';
END


-- ==========================================
-- CATEGORY 4: ĐỒ ĂN VẶT (SNK) - CategoryId 4
-- ==========================================

-- 12. Snack Oishi Bí Đỏ 40g (SNK-002)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'SNK-002' OR Barcode = N'8935049500312')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'SNK-002', N'Snack Oishi Bí Đỏ 40g', 4, 6000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500312', N'Snack vị bò nướng bí đỏ giòn ngon.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Gói', 1.0000, 1, N'8935049500312', 6000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 300.000, 300.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Snack Oishi Bí Đỏ';
END

-- 13. Bánh Choco-Pie Orion 2 Cái (SNK-003)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'SNK-003' OR Barcode = N'8935049500329')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'SNK-003', N'Bánh Choco-Pie Orion 2 Cái', 4, 10000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500329', N'Bánh Choco-pie sô cô la thơm ngon xốp dẻo.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Hộp', 1.0000, 1, N'8935049500329', 10000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 180.000, 180.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 180.000, 180.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 180.000, 180.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Bánh Choco-Pie Orion';
END

-- 14. Kẹo Dẻo Haribo Goldbears 80g (SNK-004)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'SNK-004' OR Barcode = N'8935049500336')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'SNK-004', N'Kẹo Dẻo Haribo Goldbears 80g', 4, 22000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500336', N'Kẹo dẻo hương vị trái cây hình gấu ngộ nghĩnh.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Gói', 1.0000, 1, N'8935049500336', 22000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 250.000, 250.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 250.000, 250.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 250.000, 250.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Kẹo Dẻo Haribo Goldbears';
END

-- 15. Bánh Quy Oreo Socola 133g (SNK-005)
IF NOT EXISTS (SELECT 1 FROM dbo.Product WHERE Sku = N'SNK-005' OR Barcode = N'8935049500343')
BEGIN
    INSERT INTO dbo.Product (Sku, ProductName, CategoryId, StandardPrice, DefaultSupplierId, Status, CreatedAt, Barcode, Description)
    VALUES (N'SNK-005', N'Bánh Quy Oreo Socola 133g', 4, 16000.00, 2, N'Active', SYSUTCDATETIME(), N'8935049500343', N'Bánh quy giòn vị sô cô la kẹp kem truyền thống.');
    SET @NewProductId = SCOPE_IDENTITY();

    INSERT INTO dbo.ProductUOM (ProductId, UomName, ConversionRate, IsBaseUnit, Barcode, StandardPrice, Status)
    VALUES (@NewProductId, N'Thanh', 1.0000, 1, N'8935049500343', 16000.00, N'ACTIVE');

    INSERT INTO dbo.BranchInventory (BranchId, ProductId, QtyOnHand, QtyAvailable, QtyInTransit, UpdatedAt)
    VALUES (1, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (10, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME()),
           (11, @NewProductId, 200.000, 200.000, 0.000, SYSUTCDATETIME());
    PRINT N'Added product: Bánh Quy Oreo Socola';
END

COMMIT TRANSACTION;
PRINT N'Transaction committed successfully!';
GO
