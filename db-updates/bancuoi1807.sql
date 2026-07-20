/* ============================================================================
   HỆ THỐNG QUẢN LÝ CHUỖI CỬA HÀNG TIỆN LỢI (CENTRALLY MANAGED RETAIL SYSTEM)
   SQL SERVER DATABASE SCRIPT — Thiết kế theo SRS "tailieunghiepvu_VER2.docx"
   Chuẩn hóa: 3NF | Target: Microsoft SQL Server 2019+
   Stack tiêu thụ: Spring Boot + JPA/Hibernate
   ============================================================================
   Quy ước:
   - PK: <Table>Id BIGINT/INT IDENTITY
   - Mọi bảng chứng từ có cột RowStatus phục vụ Soft Delete (không dùng DELETE vật lý)
   - Mọi cột trạng thái dùng NVARCHAR + CHECK (SQL Server không có ENUM native)
   - Mọi FK đặt tên FK_<Child>_<Parent>[_<Role>]
   ============================================================================ */

CREATE DATABASE ConvenienceRetailDB;
GO
USE ConvenienceRetailDB;
GO

/* ============================================================================
   NHÓM 1: BẢO MẬT & TỔ CHỨC (SECURITY & ORGANIZATION)
   ============================================================================ */

-- 1. Role -------------------------------------------------------------------
CREATE TABLE dbo.Role (
    RoleId          INT IDENTITY(1,1)  NOT NULL,
    RoleCode        NVARCHAR(20)       NOT NULL,   -- STAFF | MANAGER | ADMIN
    RoleName        NVARCHAR(100)      NOT NULL,
    CONSTRAINT PK_Role PRIMARY KEY (RoleId),
    CONSTRAINT UQ_Role_RoleCode UNIQUE (RoleCode),
    CONSTRAINT CK_Role_RoleCode CHECK (RoleCode IN (N'STAFF', N'MANAGER', N'ADMIN'))
);
GO

-- 2. Branch (chưa gắn FK ManagerId ở bước này để tránh circular reference) --
CREATE TABLE dbo.Branch (
    BranchId        INT IDENTITY(1,1)  NOT NULL,
    BranchCode      NVARCHAR(10)       NOT NULL,   -- vd: CH01
    BranchName      NVARCHAR(200)      NOT NULL,
    Address         NVARCHAR(300)      NULL,
    ManagerId       BIGINT             NULL,       -- FK -> Employee (thêm sau), 1-1 optional
    Status          NVARCHAR(20)       NOT NULL DEFAULT (N'Active'), -- Active | Archived
    CreatedAt       DATETIME2(0)       NOT NULL DEFAULT (SYSUTCDATETIME()),
    ArchivedAt       DATETIME2(0)      NULL,
    CONSTRAINT PK_Branch PRIMARY KEY (BranchId),
    CONSTRAINT UQ_Branch_BranchCode UNIQUE (BranchCode),
    CONSTRAINT UQ_Branch_ManagerId UNIQUE (ManagerId),
    CONSTRAINT CK_Branch_Status CHECK (Status IN (N'Active', N'Archived'))
);
GO

-- 3. Employee -----------------------------------------------------------------
CREATE TABLE dbo.Employee (
    EmployeeId          BIGINT IDENTITY(1,1) NOT NULL,
    EmployeeCode        NVARCHAR(20)        NOT NULL,  -- NV-YYYY-XXXX
    Username            NVARCHAR(50)        NOT NULL,
    PasswordHash        NVARCHAR(200)       NOT NULL,  -- BCrypt hash
    FullName            NVARCHAR(150)       NOT NULL,
    Email               NVARCHAR(150)       NULL,
    Phone               NVARCHAR(20)        NULL,
    RoleId              INT                 NOT NULL,
    BranchId            INT                 NOT NULL,  -- chi nhánh cố định được chỉ định
    Status              NVARCHAR(20)        NOT NULL DEFAULT (N'Active'), -- Active | Inactive
    ForceChangePassword BIT                 NOT NULL DEFAULT (1),
    CreatedAt           DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    UpdatedAt           DATETIME2(0)        NULL,
    CONSTRAINT PK_Employee PRIMARY KEY (EmployeeId),
    CONSTRAINT UQ_Employee_EmployeeCode UNIQUE (EmployeeCode),
    CONSTRAINT UQ_Employee_Username UNIQUE (Username),
    CONSTRAINT UQ_Employee_Email UNIQUE (Email),
    CONSTRAINT FK_Employee_Role FOREIGN KEY (RoleId) REFERENCES dbo.Role(RoleId),
    CONSTRAINT FK_Employee_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT CK_Employee_Status CHECK (Status IN (N'Active', N'Inactive')),
    CONSTRAINT CK_Employee_EmployeeCode CHECK (EmployeeCode LIKE N'NV-[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]')
);
GO

-- Gắn FK Branch.ManagerId -> Employee (giải quyết circular reference)
ALTER TABLE dbo.Branch
    ADD CONSTRAINT FK_Branch_Manager FOREIGN KEY (ManagerId) REFERENCES dbo.Employee(EmployeeId);
GO

CREATE INDEX IX_Employee_BranchId ON dbo.Employee(BranchId);
GO

-- 4. ActiveSession (Quản trị phiên làm việc — 1 tài khoản chỉ 1 phiên) -------
CREATE TABLE dbo.ActiveSession (
    EmployeeId      BIGINT              NOT NULL,
    SessionToken    NVARCHAR(500)       NOT NULL,
    DeviceId        NVARCHAR(200)       NULL,       -- từ User-Agent
    IpAddress       NVARCHAR(50)        NULL,
    LoginAt         DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    ExpiresAt       DATETIME2(0)        NULL,
    CONSTRAINT PK_ActiveSession PRIMARY KEY (EmployeeId),
    CONSTRAINT FK_ActiveSession_Employee FOREIGN KEY (EmployeeId) REFERENCES dbo.Employee(EmployeeId)
);
GO

-- 5. ShiftType (Ca làm: Sáng/Chiều/Tối) ---------------------------------------
CREATE TABLE dbo.ShiftType (
    ShiftTypeId     INT IDENTITY(1,1)  NOT NULL,
    ShiftName       NVARCHAR(50)       NOT NULL,   -- Sáng | Chiều | Tối
    StartTime       TIME               NOT NULL,
    EndTime         TIME               NOT NULL,
    CONSTRAINT PK_ShiftType PRIMARY KEY (ShiftTypeId),
    CONSTRAINT UQ_ShiftType_ShiftName UNIQUE (ShiftName)
);
GO

-- 6. EmployeeShiftSchedule (Lịch phân ca / Roster) ---------------------------
CREATE TABLE dbo.EmployeeShiftSchedule (
    ScheduleId      BIGINT IDENTITY(1,1) NOT NULL,
    EmployeeId      BIGINT              NOT NULL,
    BranchId        INT                 NOT NULL,
    ShiftTypeId     INT                 NOT NULL,
    DayOfWeek       TINYINT             NOT NULL,  -- 1=Mon ... 7=Sun
    EffectiveFrom   DATE                NOT NULL,
    EffectiveTo     DATE                NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_EmployeeShiftSchedule PRIMARY KEY (ScheduleId),
    CONSTRAINT FK_EmpShiftSch_Employee FOREIGN KEY (EmployeeId) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_EmpShiftSch_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_EmpShiftSch_ShiftType FOREIGN KEY (ShiftTypeId) REFERENCES dbo.ShiftType(ShiftTypeId),
    CONSTRAINT CK_EmpShiftSch_DayOfWeek CHECK (DayOfWeek BETWEEN 1 AND 7)
);
GO
CREATE INDEX IX_EmpShiftSch_Employee ON dbo.EmployeeShiftSchedule(EmployeeId);
GO

-- 7. WorkShift (Ca làm việc thực tế — Chấm công Check-in/Check-out) ---------
CREATE TABLE dbo.WorkShift (
    WorkShiftId     BIGINT IDENTITY(1,1) NOT NULL,
    EmployeeId      BIGINT              NOT NULL,
    BranchId        INT                 NOT NULL,
    CheckInTime     DATETIME2(0)        NOT NULL,
    CheckOutTime    DATETIME2(0)        NULL,
    Status          NVARCHAR(30)        NOT NULL DEFAULT (N'Open'), -- Open | Closed | Warning_Mismatch
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_WorkShift PRIMARY KEY (WorkShiftId),
    CONSTRAINT FK_WorkShift_Employee FOREIGN KEY (EmployeeId) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_WorkShift_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT CK_WorkShift_Status CHECK (Status IN (N'Open', N'Closed', N'Warning_Mismatch')),
    CONSTRAINT CK_WorkShift_TimeOrder CHECK (CheckOutTime IS NULL OR CheckOutTime > CheckInTime)
);
GO
-- Ràng buộc nghiệp vụ "không cho check-in 2 lần khi phiên chưa đóng":
-- chỉ được tồn tại tối đa 1 WorkShift ở trạng thái Open cho mỗi nhân viên (Filtered Unique Index)
CREATE UNIQUE INDEX UX_WorkShift_OneOpenPerEmployee
    ON dbo.WorkShift(EmployeeId)
    WHERE Status = N'Open';
GO

-- 8. ShiftMetrics (Kết ca — Bàn giao tiền mặt, quan hệ 1-1 với WorkShift) ----
CREATE TABLE dbo.ShiftMetrics (
    WorkShiftId     BIGINT              NOT NULL,
    CashExpected    DECIMAL(18,2)       NOT NULL DEFAULT (0),
    CashCounted     DECIMAL(18,2)       NOT NULL DEFAULT (0),
    CashVariance    AS (CashCounted - CashExpected) PERSISTED,
    BankCardAmount  DECIMAL(18,2)       NOT NULL DEFAULT (0),
    QrAmount        DECIMAL(18,2)       NOT NULL DEFAULT (0),
    ClosedAt        DATETIME2(0)        NULL,
    CONSTRAINT PK_ShiftMetrics PRIMARY KEY (WorkShiftId),
    CONSTRAINT FK_ShiftMetrics_WorkShift FOREIGN KEY (WorkShiftId) REFERENCES dbo.WorkShift(WorkShiftId)
);
GO

/* ============================================================================
   NHÓM 2: DANH MỤC GỐC (MASTER DATA)
   ============================================================================ */

-- 9. Supplier ----------------------------------------------------------------
CREATE TABLE dbo.Supplier (
    SupplierId      INT IDENTITY(1,1)  NOT NULL,
    SupplierName    NVARCHAR(200)      NOT NULL,
    ContactPhone    NVARCHAR(20)       NULL,
    ContactEmail    NVARCHAR(150)      NULL,
    Address         NVARCHAR(300)      NULL,
    Status          NVARCHAR(20)       NOT NULL DEFAULT (N'Active'), -- Active | Inactive (Soft Delete)
    CreatedAt       DATETIME2(0)       NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_Supplier PRIMARY KEY (SupplierId),
    CONSTRAINT CK_Supplier_Status CHECK (Status IN (N'Active', N'Inactive'))
);
GO

-- 10. ProductCategory (Loại/Ngành hàng — quy định tiền tố SKU) ---------------
CREATE TABLE dbo.ProductCategory (
    CategoryId      INT IDENTITY(1,1)  NOT NULL,
    CategoryName    NVARCHAR(150)      NOT NULL,
    SkuPrefix       NVARCHAR(5)        NOT NULL,   -- vd: DK, NC
    CONSTRAINT PK_ProductCategory PRIMARY KEY (CategoryId),
    CONSTRAINT UQ_ProductCategory_SkuPrefix UNIQUE (SkuPrefix)
);
GO

-- 11. Product (Danh mục sản phẩm gốc) ----------------------------------------
CREATE TABLE dbo.Product (
    ProductId           BIGINT IDENTITY(1,1) NOT NULL,
    Sku                 NVARCHAR(20)        NOT NULL,  -- [PREFIX]-[0000..9999] auto-increment zero-padded
    ProductName         NVARCHAR(200)       NOT NULL,
    CategoryId          INT                 NOT NULL,
    StandardPrice       DECIMAL(18,2)       NOT NULL,
    DefaultSupplierId   INT                 NULL,
    Status              NVARCHAR(20)        NOT NULL DEFAULT (N'Active'), -- Active | Inactive
    CreatedAt           DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_Product PRIMARY KEY (ProductId),
    CONSTRAINT UQ_Product_Sku UNIQUE (Sku),
    CONSTRAINT FK_Product_Category FOREIGN KEY (CategoryId) REFERENCES dbo.ProductCategory(CategoryId),
    CONSTRAINT FK_Product_Supplier FOREIGN KEY (DefaultSupplierId) REFERENCES dbo.Supplier(SupplierId),
    CONSTRAINT CK_Product_Status CHECK (Status IN (N'Active', N'Inactive')),
    CONSTRAINT CK_Product_StandardPrice CHECK (StandardPrice >= 0)
);
GO
CREATE INDEX IX_Product_CategoryId ON dbo.Product(CategoryId);
GO

-- 12. ProductUOM (Đơn vị tính & Tỷ lệ quy đổi) -------------------------------
CREATE TABLE dbo.ProductUOM (
    UomId               BIGINT IDENTITY(1,1) NOT NULL,
    ProductId           BIGINT              NOT NULL,
    UomName             NVARCHAR(50)        NOT NULL,   -- Thùng, Lốc, Chai...
    ConversionRate      DECIMAL(18,4)       NOT NULL,    -- quy đổi ra đơn vị bán cơ bản (Chai)
    IsBaseUnit          BIT                 NOT NULL DEFAULT (0),
    CONSTRAINT PK_ProductUOM PRIMARY KEY (UomId),
    CONSTRAINT FK_ProductUOM_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT UQ_ProductUOM_ProductName UNIQUE (ProductId, UomName),
    CONSTRAINT CK_ProductUOM_ConversionRate CHECK (ConversionRate > 0)
);
GO

/* ============================================================================
   NHÓM 3: TỒN KHO & GIÁ THEO CHI NHÁNH
   ============================================================================ */

-- 13. BranchInventory (Tồn kho theo chi nhánh) -------------------------------
CREATE TABLE dbo.BranchInventory (
    BranchId        INT                 NOT NULL,
    ProductId       BIGINT              NOT NULL,
    QtyOnHand       DECIMAL(18,3)       NOT NULL DEFAULT (0),  -- kho vật lý
    QtyAvailable    DECIMAL(18,3)       NOT NULL DEFAULT (0),  -- kho khả dụng bán tại POS
    QtyInTransit    DECIMAL(18,3)       NOT NULL DEFAULT (0),  -- kho ảo đang điều chuyển đến
    UpdatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_BranchInventory PRIMARY KEY (BranchId, ProductId),
    CONSTRAINT FK_BranchInventory_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_BranchInventory_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT CK_BranchInventory_QtyOnHand CHECK (QtyOnHand >= 0),
    CONSTRAINT CK_BranchInventory_QtyAvailable CHECK (QtyAvailable >= 0),
    CONSTRAINT CK_BranchInventory_QtyInTransit CHECK (QtyInTransit >= 0)
);
GO

-- 14. BranchPriceRequest (Đề xuất giá riêng chi nhánh) -----------------------
CREATE TABLE dbo.BranchPriceRequest (
    PriceRequestId      BIGINT IDENTITY(1,1) NOT NULL,
    BranchId            INT                 NOT NULL,
    ProductId           BIGINT              NOT NULL,
    ProposedPrice       DECIMAL(18,2)       NOT NULL,
    StandardPriceSnapshot DECIMAL(18,2)     NOT NULL,  -- giá gốc tại thời điểm đề xuất, phục vụ audit %20
    Status              NVARCHAR(20)        NOT NULL DEFAULT (N'Pending'), -- Pending | Approved | Rejected
    RequestedBy         BIGINT              NOT NULL,
    ApprovedBy          BIGINT              NULL,
    RequestedAt         DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    ApprovedAt          DATETIME2(0)        NULL,
    CONSTRAINT PK_BranchPriceRequest PRIMARY KEY (PriceRequestId),
    CONSTRAINT FK_BPR_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_BPR_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_BPR_RequestedBy FOREIGN KEY (RequestedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_BPR_ApprovedBy FOREIGN KEY (ApprovedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_BPR_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
    CONSTRAINT CK_BPR_ProposedPrice CHECK (ProposedPrice >= 0)
    -- Lưu ý: ràng buộc "biến động không vượt quá 20%" cần so sánh động với Product.StandardPrice
    -- tại thời điểm duyệt => không thể biểu diễn bằng CHECK tĩnh, sẽ implement bằng Trigger/Service Layer (xem PHẦN 6).
);
GO

-- 15. BranchProductPrice (Giá hiệu lực hiện tại theo chi nhánh — bảng phái sinh) --
CREATE TABLE dbo.BranchProductPrice (
    BranchId            INT                 NOT NULL,
    ProductId           BIGINT              NOT NULL,
    EffectivePrice       DECIMAL(18,2)       NOT NULL,
    SourcePriceRequestId BIGINT              NULL,  -- NULL nếu đang dùng giá tiêu chu�ẩn (không có giá riêng)
    EffectiveFrom        DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_BranchProductPrice PRIMARY KEY (BranchId, ProductId),
    CONSTRAINT FK_BPP_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_BPP_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_BPP_PriceRequest FOREIGN KEY (SourcePriceRequestId) REFERENCES dbo.BranchPriceRequest(PriceRequestId),
    CONSTRAINT CK_BPP_EffectivePrice CHECK (EffectivePrice >= 0)
);
GO

/* ============================================================================
   NHÓM 4: KHUYẾN MÃI
   ============================================================================ */

-- 16. Promotion ----------------------------------------------------------------
CREATE TABLE dbo.Promotion (
    PromotionId     BIGINT IDENTITY(1,1) NOT NULL,
    PromotionName   NVARCHAR(200)       NOT NULL,
    StartDateTime   DATETIME2(0)        NOT NULL,
    EndDateTime     DATETIME2(0)        NOT NULL,
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft | Active | Ended | Canceled
    CreatedBy       BIGINT              NOT NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_Promotion PRIMARY KEY (PromotionId),
    CONSTRAINT FK_Promotion_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_Promotion_Status CHECK (Status IN (N'Draft', N'Active', N'Ended', N'Canceled')),
    CONSTRAINT CK_Promotion_DateRange CHECK (EndDateTime > StartDateTime)
);
GO

-- 17. PromotionDetail (Khuyến mãi áp dụng theo SKU) --------------------------
CREATE TABLE dbo.PromotionDetail (
    PromotionDetailId BIGINT IDENTITY(1,1) NOT NULL,
    PromotionId     BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    DiscountType    NVARCHAR(20)        NOT NULL,  -- Percentage | FixedAmount
    DiscountValue   DECIMAL(18,2)       NOT NULL,
    CONSTRAINT PK_PromotionDetail PRIMARY KEY (PromotionDetailId),
    CONSTRAINT FK_PromotionDetail_Promotion FOREIGN KEY (PromotionId) REFERENCES dbo.Promotion(PromotionId),
    CONSTRAINT FK_PromotionDetail_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT UQ_PromotionDetail_Promo_Product UNIQUE (PromotionId, ProductId),
    CONSTRAINT CK_PromotionDetail_DiscountType CHECK (DiscountType IN (N'Percentage', N'FixedAmount')),
    CONSTRAINT CK_PromotionDetail_DiscountValue CHECK (DiscountValue >= 0)
);
GO

/* ============================================================================
   NHÓM 5: BÁN HÀNG POS (INVOICE / REFUND)
   ============================================================================ */

-- 18. Invoice (Hóa đơn bán hàng) ---------------------------------------------
CREATE TABLE dbo.Invoice (
    InvoiceId       BIGINT IDENTITY(1,1) NOT NULL,
    InvoiceCode     NVARCHAR(40)        NOT NULL,  -- INV-[Branch]-YYYYMMDD-[6 số]
    BranchId        INT                 NOT NULL,
    CashierId       BIGINT              NOT NULL,
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft | Held | Paid | Canceled
    PaymentMethod   NVARCHAR(20)        NULL,      -- Cash | QR | Bank | Card (bắt buộc khi Paid)
    TotalAmount     DECIMAL(18,2)       NOT NULL DEFAULT (0),
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    PaidAt          DATETIME2(0)        NULL,
    CanceledAt      DATETIME2(0)        NULL,
    CONSTRAINT PK_Invoice PRIMARY KEY (InvoiceId),
    CONSTRAINT UQ_Invoice_InvoiceCode UNIQUE (InvoiceCode),
    CONSTRAINT FK_Invoice_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_Invoice_Cashier FOREIGN KEY (CashierId) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_Invoice_Status CHECK (Status IN (N'Draft', N'Held', N'Paid', N'Canceled')),
    CONSTRAINT CK_Invoice_PaymentMethod CHECK (PaymentMethod IS NULL OR PaymentMethod IN (N'Cash', N'QR', N'Bank', N'Card')),
    CONSTRAINT CK_Invoice_TotalAmount CHECK (TotalAmount >= 0)
);
GO
CREATE INDEX IX_Invoice_BranchId_CreatedAt ON dbo.Invoice(BranchId, CreatedAt);
GO

-- 19. InvoiceDetail -----------------------------------------------------------
CREATE TABLE dbo.InvoiceDetail (
    InvoiceDetailId BIGINT IDENTITY(1,1) NOT NULL,
    InvoiceId       BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    Quantity        DECIMAL(18,3)       NOT NULL,
    UnitPrice       DECIMAL(18,2)       NOT NULL,   -- Final_POS_Price tại thời điểm bán
    PromotionId     BIGINT              NULL,       -- khuyến mãi (nếu có) đã áp dụng
    LineTotal       AS (Quantity * UnitPrice) PERSISTED,
    CONSTRAINT PK_InvoiceDetail PRIMARY KEY (InvoiceDetailId),
    CONSTRAINT FK_InvoiceDetail_Invoice FOREIGN KEY (InvoiceId) REFERENCES dbo.Invoice(InvoiceId),
    CONSTRAINT FK_InvoiceDetail_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_InvoiceDetail_Promotion FOREIGN KEY (PromotionId) REFERENCES dbo.Promotion(PromotionId),
    CONSTRAINT CK_InvoiceDetail_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_InvoiceDetail_UnitPrice CHECK (UnitPrice >= 0)
);
GO
CREATE INDEX IX_InvoiceDetail_InvoiceId ON dbo.InvoiceDetail(InvoiceId);
GO

-- 20. Refund (Đổi trả hàng) --------------------------------------------------
CREATE TABLE dbo.Refund (
    RefundId            BIGINT IDENTITY(1,1) NOT NULL,
    RefundCode          NVARCHAR(40)        NOT NULL,  -- REF-[Branch]-YYYYMMDD-[4 số]
    OriginalInvoiceId   BIGINT              NOT NULL,
    BranchId            INT                 NOT NULL,  -- phải trùng chi nhánh của hóa đơn gốc
    CustomerName        NVARCHAR(150)       NOT NULL,
    CustomerPhone       NVARCHAR(20)        NOT NULL,
    Reason              NVARCHAR(500)       NOT NULL,
    TotalRefundAmount   DECIMAL(18,2)       NOT NULL,
    Status              NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft|Pending_Approval|Completed|Rejected
    RequestedBy          BIGINT              NOT NULL,
    ApprovedBy           BIGINT              NULL,      -- Manager phê duyệt (>=200k) hoặc hậu kiểm
    PinOverrideUsed      BIT                 NOT NULL DEFAULT (0), -- Manager override PIN tại POS
    CreatedAt            DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    ApprovedAt           DATETIME2(0)        NULL,
    CONSTRAINT PK_Refund PRIMARY KEY (RefundId),
    CONSTRAINT UQ_Refund_RefundCode UNIQUE (RefundCode),
    CONSTRAINT FK_Refund_Invoice FOREIGN KEY (OriginalInvoiceId) REFERENCES dbo.Invoice(InvoiceId),
    CONSTRAINT FK_Refund_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_Refund_RequestedBy FOREIGN KEY (RequestedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_Refund_ApprovedBy FOREIGN KEY (ApprovedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_Refund_Status CHECK (Status IN (N'Draft', N'Pending_Approval', N'Completed', N'Rejected')),
    CONSTRAINT CK_Refund_TotalRefundAmount CHECK (TotalRefundAmount >= 0)
    -- Lưu ý: ràng buộc "trong 7 ngày kể từ ngày in" và "tổng SL hoàn trả tích lũy <= SL mua ban đầu"
    -- là ràng buộc liên-bảng/liên-dòng động => implement tại Service Layer (Spring Boot) trước khi INSERT.
);
GO
CREATE INDEX IX_Refund_OriginalInvoiceId ON dbo.Refund(OriginalInvoiceId);
GO

-- 21. RefundDetail -------------------------------------------------------------
CREATE TABLE dbo.RefundDetail (
    RefundDetailId  BIGINT IDENTITY(1,1) NOT NULL,
    RefundId        BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    Quantity        DECIMAL(18,3)       NOT NULL,
    ConditionType   NVARCHAR(20)        NOT NULL,  -- Damaged | Resalable
    UnitRefundAmount DECIMAL(18,2)      NOT NULL,
    CONSTRAINT PK_RefundDetail PRIMARY KEY (RefundDetailId),
    CONSTRAINT FK_RefundDetail_Refund FOREIGN KEY (RefundId) REFERENCES dbo.Refund(RefundId),
    CONSTRAINT FK_RefundDetail_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT CK_RefundDetail_ConditionType CHECK (ConditionType IN (N'Damaged', N'Resalable')),
    CONSTRAINT CK_RefundDetail_Quantity CHECK (Quantity > 0)
);
GO

/* ============================================================================
   NHÓM 6: MUA HÀNG & CÔNG NỢ (PROCUREMENT / AP)
   ============================================================================ */

-- 22. PurchaseOrder ------------------------------------------------------------
CREATE TABLE dbo.PurchaseOrder (
    PurchaseOrderId BIGINT IDENTITY(1,1) NOT NULL,
    PoCode          NVARCHAR(40)        NOT NULL,  -- PO-[Branch]-YYYYMMDD-[4 số]
    BranchId        INT                 NOT NULL,
    SupplierId      INT                 NOT NULL,
    Status          NVARCHAR(30)        NOT NULL DEFAULT (N'Draft'),
        -- Draft|Submitted|Partially_Received|Received_Partial|Canceled
    CreatedBy       BIGINT              NOT NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_PurchaseOrder PRIMARY KEY (PurchaseOrderId),
    CONSTRAINT UQ_PurchaseOrder_PoCode UNIQUE (PoCode),
    CONSTRAINT FK_PO_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_PO_Supplier FOREIGN KEY (SupplierId) REFERENCES dbo.Supplier(SupplierId),
    CONSTRAINT FK_PO_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_PO_Status CHECK (Status IN (N'Draft', N'Submitted', N'Partially_Received', N'Received_Partial', N'Canceled'))
);
GO

-- 23. PurchaseOrderDetail -------------------------------------------------------
CREATE TABLE dbo.PurchaseOrderDetail (
    PoDetailId      BIGINT IDENTITY(1,1) NOT NULL,
    PurchaseOrderId BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    UomId           BIGINT              NOT NULL,
    QuantityOrdered DECIMAL(18,3)       NOT NULL,
    UnitCost        DECIMAL(18,2)       NULL,
    CONSTRAINT PK_PurchaseOrderDetail PRIMARY KEY (PoDetailId),
    CONSTRAINT FK_POD_PO FOREIGN KEY (PurchaseOrderId) REFERENCES dbo.PurchaseOrder(PurchaseOrderId),
    CONSTRAINT FK_POD_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_POD_Uom FOREIGN KEY (UomId) REFERENCES dbo.ProductUOM(UomId),
    CONSTRAINT CK_POD_QuantityOrdered CHECK (QuantityOrdered > 0)
);
GO

-- 24. GoodsReceiptNote (GRN) ----------------------------------------------------
CREATE TABLE dbo.GoodsReceiptNote (
    GrnId           BIGINT IDENTITY(1,1) NOT NULL,
    GrnCode         NVARCHAR(40)        NOT NULL,  -- GRN-[Branch]-YYYYMMDD-[4 số]
    PurchaseOrderId BIGINT              NOT NULL,
    BranchId        INT                 NOT NULL,
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft|Submitted|Completed|Canceled
    ReceivedBy      BIGINT              NOT NULL,
    ReceivedAt      DATETIME2(0)        NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_GoodsReceiptNote PRIMARY KEY (GrnId),
    CONSTRAINT UQ_GRN_GrnCode UNIQUE (GrnCode),
    CONSTRAINT FK_GRN_PO FOREIGN KEY (PurchaseOrderId) REFERENCES dbo.PurchaseOrder(PurchaseOrderId),
    CONSTRAINT FK_GRN_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_GRN_ReceivedBy FOREIGN KEY (ReceivedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_GRN_Status CHECK (Status IN (N'Draft', N'Submitted', N'Completed', N'Canceled'))
);
GO
CREATE INDEX IX_GRN_PurchaseOrderId ON dbo.GoodsReceiptNote(PurchaseOrderId);
GO

-- 25. GoodsReceiptNoteDetail -----------------------------------------------------
CREATE TABLE dbo.GoodsReceiptNoteDetail (
    GrnDetailId     BIGINT IDENTITY(1,1) NOT NULL,
    GrnId           BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    UomId           BIGINT              NOT NULL,
    QuantityReceived DECIMAL(18,3)      NOT NULL,
    QuantityConvertedBase DECIMAL(18,3) NOT NULL, -- = QuantityReceived * ConversionRate
    CONSTRAINT PK_GoodsReceiptNoteDetail PRIMARY KEY (GrnDetailId),
    CONSTRAINT FK_GRND_GRN FOREIGN KEY (GrnId) REFERENCES dbo.GoodsReceiptNote(GrnId),
    CONSTRAINT FK_GRND_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_GRND_Uom FOREIGN KEY (UomId) REFERENCES dbo.ProductUOM(UomId),
    CONSTRAINT CK_GRND_QuantityReceived CHECK (QuantityReceived > 0)
);
GO

-- 26. SupplierInvoice (Hóa đơn nhà cung cấp — căn cứ ghi nhận AP) ---------------
CREATE TABLE dbo.SupplierInvoice (
    SupplierInvoiceId BIGINT IDENTITY(1,1) NOT NULL,
    GrnId             BIGINT              NOT NULL,  -- 1-1 với GRN (giả định: 1 GRN ứng với 1 hóa đơn NCC)
    SupplierId        INT                 NOT NULL,
    Amount            DECIMAL(18,2)       NOT NULL,
    AmountPaid        DECIMAL(18,2)       NOT NULL DEFAULT (0),
    Status            NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'),
        -- Draft|Approved|Unpaid|Partially_Paid|Paid
    IssuedAt          DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    ApprovedBy        BIGINT              NULL,
    ApprovedAt        DATETIME2(0)        NULL,
    CONSTRAINT PK_SupplierInvoice PRIMARY KEY (SupplierInvoiceId),
    CONSTRAINT UQ_SupplierInvoice_GrnId UNIQUE (GrnId),
    CONSTRAINT FK_SI_GRN FOREIGN KEY (GrnId) REFERENCES dbo.GoodsReceiptNote(GrnId),
    CONSTRAINT FK_SI_Supplier FOREIGN KEY (SupplierId) REFERENCES dbo.Supplier(SupplierId),
    CONSTRAINT FK_SI_ApprovedBy FOREIGN KEY (ApprovedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_SI_Status CHECK (Status IN (N'Draft', N'Approved', N'Unpaid', N'Partially_Paid', N'Paid')),
    CONSTRAINT CK_SI_Amount CHECK (Amount >= 0),
    CONSTRAINT CK_SI_AmountPaid CHECK (AmountPaid >= 0 AND AmountPaid <= Amount)
);
GO

-- 27. SupplierPayment (Phiếu chi thanh toán công nợ) ----------------------------
CREATE TABLE dbo.SupplierPayment (
    SupplierPaymentId BIGINT IDENTITY(1,1) NOT NULL,
    SupplierInvoiceId BIGINT              NOT NULL,
    AmountPaid        DECIMAL(18,2)       NOT NULL,
    PaymentMethod     NVARCHAR(20)        NOT NULL,  -- Cash | Bank | QR | Card
    PaidBy            BIGINT              NOT NULL,  -- Admin
    PaidAt            DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_SupplierPayment PRIMARY KEY (SupplierPaymentId),
    CONSTRAINT FK_SP_SupplierInvoice FOREIGN KEY (SupplierInvoiceId) REFERENCES dbo.SupplierInvoice(SupplierInvoiceId),
    CONSTRAINT FK_SP_PaidBy FOREIGN KEY (PaidBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_SP_AmountPaid CHECK (AmountPaid > 0)
);
GO
CREATE INDEX IX_SupplierPayment_SupplierInvoiceId ON dbo.SupplierPayment(SupplierInvoiceId);
GO

/* ============================================================================
   NHÓM 7: ĐIỀU CHUYỂN, XUẤT HỦY, KIỂM KÊ KHO
   ============================================================================ */

-- 28. StockTransfer (Điều chuyển kho nội bộ) ------------------------------------
CREATE TABLE dbo.StockTransfer (
    StockTransferId BIGINT IDENTITY(1,1) NOT NULL,
    TransferCode    NVARCHAR(50)        NOT NULL,  -- ST-[CN Gửi]-[CN Nhận]-YYYYMMDD-[4 số]
    FromBranchId    INT                 NOT NULL,
    ToBranchId      INT                 NOT NULL,
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft|In_Transit|Completed|Rejected
    CreatedBy       BIGINT              NOT NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    ReceivedBy      BIGINT              NULL,
    ReceivedAt      DATETIME2(0)        NULL,
    CONSTRAINT PK_StockTransfer PRIMARY KEY (StockTransferId),
    CONSTRAINT UQ_StockTransfer_TransferCode UNIQUE (TransferCode),
    CONSTRAINT FK_ST_FromBranch FOREIGN KEY (FromBranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_ST_ToBranch FOREIGN KEY (ToBranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_ST_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_ST_ReceivedBy FOREIGN KEY (ReceivedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_ST_Status CHECK (Status IN (N'Draft', N'In_Transit', N'Completed', N'Rejected')),
    CONSTRAINT CK_ST_DifferentBranch CHECK (FromBranchId <> ToBranchId)
);
GO

-- 29. StockTransferDetail --------------------------------------------------------
CREATE TABLE dbo.StockTransferDetail (
    TransferDetailId BIGINT IDENTITY(1,1) NOT NULL,
    StockTransferId BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    QuantitySent    DECIMAL(18,3)       NOT NULL,
    QuantityReceived DECIMAL(18,3)      NULL,
    VarianceQty     AS (QuantityReceived - QuantitySent) PERSISTED,
    CONSTRAINT PK_StockTransferDetail PRIMARY KEY (TransferDetailId),
    CONSTRAINT FK_STD_Transfer FOREIGN KEY (StockTransferId) REFERENCES dbo.StockTransfer(StockTransferId),
    CONSTRAINT FK_STD_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT CK_STD_QuantitySent CHECK (QuantitySent > 0)
);
GO

-- 30. StockDisposal (Xuất kho khác — hủy hàng / hao hụt vận chuyển) -------------
CREATE TABLE dbo.StockDisposal (
    StockDisposalId BIGINT IDENTITY(1,1) NOT NULL,
    DisposalCode    NVARCHAR(40)        NOT NULL,
    BranchId        INT                 NOT NULL,
    Reason          NVARCHAR(500)       NOT NULL,   -- bắt buộc nhập tay
    SourceType      NVARCHAR(30)        NOT NULL DEFAULT (N'Manual'), -- Manual | TransferLossAuto
    RelatedTransferId BIGINT            NULL,        -- nếu SourceType = TransferLossAuto
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft|Completed|Canceled
    CreatedBy       BIGINT              NOT NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_StockDisposal PRIMARY KEY (StockDisposalId),
    CONSTRAINT UQ_StockDisposal_DisposalCode UNIQUE (DisposalCode),
    CONSTRAINT FK_SD_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_SD_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_SD_RelatedTransfer FOREIGN KEY (RelatedTransferId) REFERENCES dbo.StockTransfer(StockTransferId),
    CONSTRAINT CK_SD_Status CHECK (Status IN (N'Draft', N'Completed', N'Canceled')),
    CONSTRAINT CK_SD_SourceType CHECK (SourceType IN (N'Manual', N'TransferLossAuto'))
);
GO

-- 31. StockDisposalDetail ---------------------------------------------------------
CREATE TABLE dbo.StockDisposalDetail (
    DisposalDetailId BIGINT IDENTITY(1,1) NOT NULL,
    StockDisposalId BIGINT              NOT NULL,
    ProductId       BIGINT              NOT NULL,
    Quantity        DECIMAL(18,3)       NOT NULL,
    CONSTRAINT PK_StockDisposalDetail PRIMARY KEY (DisposalDetailId),
    CONSTRAINT FK_SDD_Disposal FOREIGN KEY (StockDisposalId) REFERENCES dbo.StockDisposal(StockDisposalId),
    CONSTRAINT FK_SDD_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT CK_SDD_Quantity CHECK (Quantity > 0)
);
GO

-- 32. InventoryCount (Kiểm kê kho định kỳ) -----------------------------------------
CREATE TABLE dbo.InventoryCount (
    InventoryCountId BIGINT IDENTITY(1,1) NOT NULL,
    CountCode       NVARCHAR(40)        NOT NULL,  -- STK-[Branch]-YYYYMMDD-[4 số]
    BranchId        INT                 NOT NULL,
    Status          NVARCHAR(20)        NOT NULL DEFAULT (N'Draft'), -- Draft|Submitted|Approved|Rejected
    CreatedBy       BIGINT              NOT NULL,
    SubmittedAt     DATETIME2(0)        NULL,
    ApprovedBy      BIGINT              NULL,
    ApprovedAt      DATETIME2(0)        NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_InventoryCount PRIMARY KEY (InventoryCountId),
    CONSTRAINT UQ_InventoryCount_CountCode UNIQUE (CountCode),
    CONSTRAINT FK_IC_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_IC_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT FK_IC_ApprovedBy FOREIGN KEY (ApprovedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_IC_Status CHECK (Status IN (N'Draft', N'Submitted', N'Approved', N'Rejected'))
);
GO

-- 33. InventoryCountDetail -----------------------------------------------------------
CREATE TABLE dbo.InventoryCountDetail (
    CountDetailId   BIGINT IDENTITY(1,1) NOT NULL,
    InventoryCountId BIGINT             NOT NULL,
    ProductId       BIGINT              NOT NULL,
    SystemQty       DECIMAL(18,3)       NOT NULL,   -- số lượng phần mềm tại thời điểm kiểm
    ActualQty       DECIMAL(18,3)       NOT NULL,   -- số lượng đếm thực tế
    DeltaQty        AS (ActualQty - SystemQty) PERSISTED,
    CONSTRAINT PK_InventoryCountDetail PRIMARY KEY (CountDetailId),
    CONSTRAINT FK_ICD_Count FOREIGN KEY (InventoryCountId) REFERENCES dbo.InventoryCount(InventoryCountId),
    CONSTRAINT FK_ICD_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT CK_ICD_ActualQty CHECK (ActualQty >= 0)
);
GO

/* ============================================================================
   NHÓM 8: LỊCH SỬ KHO & NHẬT KÝ VẬN HÀNH
   ============================================================================ */

-- 34. InventoryTransactionHistory (Sổ cái giao dịch kho — nguồn sự thật duy nhất) --
CREATE TABLE dbo.InventoryTransactionHistory (
    TransactionId   BIGINT IDENTITY(1,1) NOT NULL,
    BranchId        INT                 NOT NULL,
    ProductId       BIGINT              NOT NULL,
    TransactionType NVARCHAR(30)        NOT NULL,
        -- Sale | Refund_Restock | GRN | Disposal | TransferOut | TransferIn | CountAdjustment
    ReferenceTable  NVARCHAR(50)        NOT NULL,   -- tên bảng chứng từ gốc
    ReferenceId     BIGINT              NOT NULL,   -- id chứng từ gốc
    QuantityChange  DECIMAL(18,3)       NOT NULL,   -- (+) nhập / (-) xuất
    Reason          NVARCHAR(500)       NULL,
    CreatedBy       BIGINT              NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_InventoryTransactionHistory PRIMARY KEY (TransactionId),
    CONSTRAINT FK_ITH_Branch FOREIGN KEY (BranchId) REFERENCES dbo.Branch(BranchId),
    CONSTRAINT FK_ITH_Product FOREIGN KEY (ProductId) REFERENCES dbo.Product(ProductId),
    CONSTRAINT FK_ITH_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES dbo.Employee(EmployeeId),
    CONSTRAINT CK_ITH_TransactionType CHECK (TransactionType IN
        (N'Sale', N'Refund_Restock', N'GRN', N'Disposal', N'TransferOut', N'TransferIn', N'CountAdjustment'))
);
GO
CREATE INDEX IX_ITH_Branch_Product ON dbo.InventoryTransactionHistory(BranchId, ProductId);
GO

-- 35. AuditLog (Nhật ký vận hành mở rộng) ------------------------------------------
CREATE TABLE dbo.AuditLog (
    AuditLogId      BIGINT IDENTITY(1,1) NOT NULL,
    EmployeeId      BIGINT              NULL,       -- tài khoản thực hiện
    ActionType      NVARCHAR(50)        NOT NULL,
        -- ApproveInventoryCount|Disposal|PriceChange|ApprovePromotion|CancelPromotion|UpdateSupplier|ApproveRefund
    EntityName      NVARCHAR(50)        NOT NULL,   -- tên bảng bị tác động
    EntityId        BIGINT              NOT NULL,
    OldValue        NVARCHAR(MAX)       NULL,       -- JSON snapshot trước khi đổi
    NewValue        NVARCHAR(MAX)       NULL,       -- JSON snapshot sau khi đổi
    Reason          NVARCHAR(500)       NULL,       -- bắt buộc với 1 số ActionType (enforce ở App layer)
    IpAddress       NVARCHAR(50)        NULL,
    DeviceId        NVARCHAR(200)       NULL,
    CreatedAt       DATETIME2(0)        NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AuditLog PRIMARY KEY (AuditLogId),
    CONSTRAINT FK_AuditLog_Employee FOREIGN KEY (EmployeeId) REFERENCES dbo.Employee(EmployeeId)
);
GO
CREATE INDEX IX_AuditLog_Entity ON dbo.AuditLog(EntityName, EntityId);
GO

/* ============================================================================
   DỮ LIỆU KHỞI TẠO TỐI THIỂU (SEED DATA THAM KHẢO)
   ============================================================================ */
INSERT INTO dbo.Role (RoleCode, RoleName) VALUES
    (N'STAFF', N'Nhân viên'), (N'MANAGER', N'Quản lý kho'), (N'ADMIN', N'Administrator');
GO
INSERT INTO dbo.ShiftType (ShiftName, StartTime, EndTime) VALUES
    (N'Sáng', '06:00', '14:00'), (N'Chiều', '14:00', '22:00'), (N'Tối', '22:00', '06:00');
GO

/* ============================================================================
   GHI CHÚ TRIỂN KHAI QUAN TRỌNG (đọc cùng PHẦN 6 trong báo cáo):
   1. Các ràng buộc động (so sánh 2 dòng dữ liệu hoặc tổng hợp nhiều dòng) không thể
      biểu diễn bằng CHECK CONSTRAINT tĩnh của SQL Server, phải xử lý bằng Trigger hoặc
      tốt hơn là Service Layer (Spring Boot) trong 1 Transaction để dễ bảo trì & test:
        - BranchPriceRequest: %biến động giá <= 20% so với Product.StandardPrice
        - Refund: hạn 7 ngày kể từ Invoice.CreatedAt; tổng SL hoàn trả tích lũy <= SL đã mua
        - BranchInventory.QtyAvailable: khóa thanh toán POS nếu Quantity giỏ hàng > QtyAvailable
   2. Không có bảng nào bị xóa vật lý (Hard Delete) — toàn bộ vòng đời qua cột Status.
   ============================================================================ */
   -- 1. Xóa ràng buộc CHECK cũ trên bảng PurchaseOrder
ALTER TABLE dbo.PurchaseOrder DROP CONSTRAINT CK_PO_Status;

-- 2. Tạo lại ràng buộc CHECK mới bổ sung thêm trạng thái N'Completed'
ALTER TABLE dbo.PurchaseOrder 
    ADD CONSTRAINT CK_PO_Status CHECK (Status IN (N'Draft', N'Submitted', N'Partially_Received', N'Received_Partial', N'Completed', N'Canceled'));

	-- 1. Bổ sung các cột audit còn thiếu vào bảng Nhà cung cấp (Supplier)
-- Cột CreatedAt đã có sẵn trong thiết kế gốc nên không cần thêm.
ALTER TABLE dbo.Supplier ADD UpdatedAt DATETIME2(0) NULL;
ALTER TABLE dbo.Supplier ADD CreatedBy NVARCHAR(100) NULL;
ALTER TABLE dbo.Supplier ADD UpdatedBy NVARCHAR(100) NULL;

-- 2. Bổ sung toàn bộ 4 cột audit vào bảng Danh mục ngành hàng (ProductCategory)
ALTER TABLE dbo.ProductCategory ADD CreatedAt DATETIME2(0) NULL;
ALTER TABLE dbo.ProductCategory ADD UpdatedAt DATETIME2(0) NULL;
ALTER TABLE dbo.ProductCategory ADD CreatedBy NVARCHAR(100) NULL;
ALTER TABLE dbo.ProductCategory ADD UpdatedBy NVARCHAR(100) NULL;
-- 1. Bổ sung các cột nghiệp vụ mới cho Sản phẩm
ALTER TABLE dbo.Product ADD Barcode NVARCHAR(100) NULL;
ALTER TABLE dbo.Product ADD Description NVARCHAR(MAX) NULL;

-- 2. Bổ sung các cột audit còn thiếu
ALTER TABLE dbo.Product ADD UpdatedAt DATETIME2(0) NULL;
ALTER TABLE dbo.Product ADD CreatedBy NVARCHAR(100) NULL;
ALTER TABLE dbo.Product ADD UpdatedBy NVARCHAR(100) NULL;

-- 3. Tạo ràng buộc duy nhất (Unique Constraint) cho cột Barcode (Mã vạch)
ALTER TABLE dbo.Product ADD CONSTRAINT UQ_Product_Barcode UNIQUE (Barcode);