-- =====================================================================
-- CƠ SỞ DỮ LIỆU: QL_Laptop (Microsoft SQL Server)
-- Đề tài: Hệ thống Quản lý Website Bán Laptop
-- Phụ trách thiết kế & xây dựng CSDL: Thành viên 1 - Lư Gia Huy
-- =====================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =========================================
-- 1. TAO DATABASE
-- =========================================
IF DB_ID(N'QL_Laptop') IS NULL
BEGIN
    CREATE DATABASE QL_Laptop;
END
GO

USE QL_Laptop;
GO

-- Xoa cac bang cu theo thu tu khoa ngoai (giup script chay lai duoc tren moi may)
DROP TABLE IF EXISTS OrderStatusHistory;
DROP TABLE IF EXISTS Payments;
DROP TABLE IF EXISTS OrderDetails;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Customers;
DROP TABLE IF EXISTS Laptops;
DROP TABLE IF EXISTS Brands;
DROP TABLE IF EXISTS Categories;
DROP TABLE IF EXISTS Accounts;
DROP TABLE IF EXISTS Roles;
GO

-- =========================================
-- 2. BANG ROLES
-- =========================================
CREATE TABLE Roles (
    RoleID INT IDENTITY(1,1) PRIMARY KEY,
    RoleName NVARCHAR(50) NOT NULL UNIQUE,
    Description NVARCHAR(255)
);
GO

-- =========================================
-- 3. BANG ACCOUNTS
-- =========================================
CREATE TABLE Accounts (
    AccountID INT IDENTITY(1,1) PRIMARY KEY,
    Username VARCHAR(50) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    FullName NVARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    Phone VARCHAR(15),
    RoleID INT NOT NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Accounts_Roles
        FOREIGN KEY (RoleID) REFERENCES Roles(RoleID)
);
GO

-- =========================================
-- 4. BANG CATEGORIES
-- =========================================
CREATE TABLE Categories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName NVARCHAR(100) NOT NULL UNIQUE,
    Description NVARCHAR(500),
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- =========================================
-- 5. BANG BRANDS
-- =========================================
CREATE TABLE Brands (
    BrandID INT IDENTITY(1,1) PRIMARY KEY,
    BrandName NVARCHAR(100) NOT NULL UNIQUE,
    Country NVARCHAR(100),
    Description NVARCHAR(500),
    IsActive BIT NOT NULL DEFAULT 1
);
GO

-- =========================================
-- 6. BANG LAPTOPS
-- =========================================
CREATE TABLE Laptops (
    LaptopID INT IDENTITY(1,1) PRIMARY KEY,
    SKU VARCHAR(30) NOT NULL UNIQUE,
    LaptopName NVARCHAR(200) NOT NULL,

    CategoryID INT NOT NULL,
    BrandID INT NOT NULL,

    CPU NVARCHAR(150),
    RAM_GB INT,
    Storage_GB INT,
    GPU NVARCHAR(150),
    ScreenSize DECIMAL(4,1),
    OperatingSystem NVARCHAR(100),
    Color NVARCHAR(50),

    PurchasePrice DECIMAL(18,2) NOT NULL DEFAULT 0,
    SalePrice DECIMAL(18,2) NOT NULL,
    StockQuantity INT NOT NULL DEFAULT 0,

    ImageURL NVARCHAR(500),
    Description NVARCHAR(MAX),

    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2,

    CONSTRAINT FK_Laptops_Categories
        FOREIGN KEY (CategoryID) REFERENCES Categories(CategoryID),

    CONSTRAINT FK_Laptops_Brands
        FOREIGN KEY (BrandID) REFERENCES Brands(BrandID),

    CONSTRAINT CK_Laptops_PurchasePrice
        CHECK (PurchasePrice >= 0),

    CONSTRAINT CK_Laptops_SalePrice
        CHECK (SalePrice >= 0),

    CONSTRAINT CK_Laptops_Stock
        CHECK (StockQuantity >= 0),

    CONSTRAINT CK_Laptops_RAM
        CHECK (RAM_GB IS NULL OR RAM_GB > 0),

    CONSTRAINT CK_Laptops_Storage
        CHECK (Storage_GB IS NULL OR Storage_GB > 0)
);
GO

-- =========================================
-- 7. BANG CUSTOMERS
-- AccountID cho phep NULL (khach vang lai) va duoc kiem soat
-- duy nhat khi khac NULL bang Filtered Unique Index UX_Customers_AccountID
-- =========================================
CREATE TABLE Customers (
    CustomerID INT IDENTITY(1,1) PRIMARY KEY,
    AccountID INT NULL,
    CustomerName NVARCHAR(100) NOT NULL,
    Email VARCHAR(100),
    Phone VARCHAR(15) NOT NULL,
    Address NVARCHAR(300),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Customers_Accounts
        FOREIGN KEY (AccountID) REFERENCES Accounts(AccountID)
);
GO

-- =========================================
-- 8. BANG ORDERS
-- =========================================
CREATE TABLE Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    OrderCode VARCHAR(30) NOT NULL UNIQUE,

    CustomerID INT,
    CreatedBy INT,

    RecipientName NVARCHAR(100) NOT NULL,
    RecipientPhone VARCHAR(15) NOT NULL,
    DeliveryAddress NVARCHAR(300) NOT NULL,

    OrderDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    OrderStatus NVARCHAR(20) NOT NULL DEFAULT N'PENDING',

    Subtotal DECIMAL(18,2) NOT NULL DEFAULT 0,
    Discount DECIMAL(18,2) NOT NULL DEFAULT 0,
    ShippingFee DECIMAL(18,2) NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(18,2) NOT NULL DEFAULT 0,

    Note NVARCHAR(500),
    UpdatedAt DATETIME2,

    CONSTRAINT FK_Orders_Customers
        FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),

    CONSTRAINT FK_Orders_Accounts
        FOREIGN KEY (CreatedBy) REFERENCES Accounts(AccountID),

    CONSTRAINT CK_Orders_Status
        CHECK (OrderStatus IN (
            N'PENDING', N'CONFIRMED', N'SHIPPING',
            N'COMPLETED', N'CANCELLED', N'RETURNED'
        )),

    CONSTRAINT CK_Orders_Amounts
        CHECK (
            Subtotal >= 0 AND Discount >= 0
            AND ShippingFee >= 0 AND TotalAmount >= 0
        )
);
GO

-- =========================================
-- 9. BANG ORDERDETAILS
-- =========================================
CREATE TABLE OrderDetails (
    OrderDetailID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL,
    LaptopID INT,

    SKUSnapshot VARCHAR(30) NOT NULL,
    LaptopNameSnapshot NVARCHAR(200) NOT NULL,

    UnitPrice DECIMAL(18,2) NOT NULL,
    Quantity INT NOT NULL,
    LineTotal DECIMAL(18,2) NOT NULL,

    CONSTRAINT FK_OrderDetails_Orders
        FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),

    CONSTRAINT FK_OrderDetails_Laptops
        FOREIGN KEY (LaptopID) REFERENCES Laptops(LaptopID),

    CONSTRAINT CK_OrderDetails_UnitPrice
        CHECK (UnitPrice >= 0),

    CONSTRAINT CK_OrderDetails_Quantity
        CHECK (Quantity > 0),

    CONSTRAINT CK_OrderDetails_LineTotal
        CHECK (LineTotal >= 0)
);
GO

-- =========================================
-- 10. BANG PAYMENTS
-- =========================================
CREATE TABLE Payments (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,

    PaymentMethod NVARCHAR(30) NOT NULL,
    PaymentStatus NVARCHAR(20) NOT NULL DEFAULT N'UNPAID',

    Amount DECIMAL(18,2) NOT NULL,
    TransactionCode VARCHAR(100),
    PaidAt DATETIME2,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_Payments_Orders
        FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),

    CONSTRAINT CK_Payments_Method
        CHECK (PaymentMethod IN (
            N'CASH', N'BANK_TRANSFER', N'CARD', N'E_WALLET'
        )),

    CONSTRAINT CK_Payments_Status
        CHECK (PaymentStatus IN (
            N'UNPAID', N'PENDING', N'PAID',
            N'FAILED', N'REFUNDED'
        )),

    CONSTRAINT CK_Payments_Amount
        CHECK (Amount >= 0)
);
GO

-- =========================================
-- 11. BANG ORDERSTATUSHISTORY
-- =========================================
CREATE TABLE OrderStatusHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,

    OrderID INT NOT NULL,
    OldStatus NVARCHAR(20),
    NewStatus NVARCHAR(20) NOT NULL,

    ChangedBy INT,
    ChangeNote NVARCHAR(500),
    ChangedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_History_Orders
        FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),

    CONSTRAINT FK_History_Accounts
        FOREIGN KEY (ChangedBy) REFERENCES Accounts(AccountID),

    CONSTRAINT CK_History_NewStatus
        CHECK (NewStatus IN (
            N'PENDING', N'CONFIRMED', N'SHIPPING',
            N'COMPLETED', N'CANCELLED', N'RETURNED'
        ))
);
GO

-- =========================================
-- 12. TAO INDEX HO TRO TIM KIEM & RANG BUOC DUY NHAT CO DIEU KIEN
-- =========================================
CREATE INDEX IX_Laptops_LaptopName
ON Laptops(LaptopName);

CREATE INDEX IX_Laptops_CategoryID
ON Laptops(CategoryID);

CREATE INDEX IX_Laptops_BrandID
ON Laptops(BrandID);

CREATE INDEX IX_Orders_OrderDate
ON Orders(OrderDate);

CREATE INDEX IX_Orders_OrderStatus
ON Orders(OrderStatus);

CREATE INDEX IX_Customers_CustomerName
ON Customers(CustomerName);

-- Filtered Unique Index: Đảm bảo mỗi AccountID (khi khác NULL) chỉ gắn với 1 Customer duy nhất,
-- nhưng vẫn cho phép nhiều khách hàng vãng lai có AccountID = NULL.
CREATE UNIQUE NONCLUSTERED INDEX UX_Customers_AccountID
ON Customers(AccountID)
WHERE AccountID IS NOT NULL;
GO

-- =========================================
-- PHẦN CHÈN DỮ LIỆU MẪU (SEED DATA)
-- =========================================

-- 1. ROLES (3 vai trò phân quyền chuẩn)
INSERT INTO Roles (RoleName, Description)
VALUES
(N'ADMIN', N'Quản trị hệ thống'),
(N'STAFF', N'Nhân viên cửa hàng'),
(N'CUSTOMER', N'Khách hàng');
GO

-- 2. ACCOUNTS (Mật khẩu mặc định: 123456 đã mã hóa chuẩn BCrypt)
INSERT INTO Accounts
(Username, PasswordHash, FullName, Email, Phone, RoleID)
VALUES
('admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Quản trị viên',  'admin@qlLaptop.com',   '0900000001', 1),
('staff01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nhân viên 01',   'staff01@qlLaptop.com', '0900000002', 2),
('staff02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nhân viên 02',   'staff02@qlLaptop.com', '0900000003', 2),
('khach01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Nguyễn Văn An',  'an01@gmail.com',       '0911000001', 3),
('khach02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', N'Trần Thị Bình',  'binh02@gmail.com',     '0911000002', 3);
GO

-- 3. CATEGORIES - 20 DANH MỤC
INSERT INTO Categories (CategoryName, Description)
VALUES
(N'Laptop văn phòng', N'Laptop phục vụ công việc văn phòng'),
(N'Laptop gaming', N'Laptop chuyên chơi game'),
(N'Laptop đồ họa', N'Laptop phục vụ thiết kế và đồ họa'),
(N'Laptop mỏng nhẹ', N'Laptop có thiết kế mỏng nhẹ'),
(N'Laptop cao cấp', N'Laptop phân khúc cao cấp'),
(N'Laptop học sinh - sinh viên', N'Laptop giá tốt phục vụ học tập'),
(N'Laptop doanh nhân', N'Laptop bảo mật cao, bền bỉ cho doanh nhân'),
(N'Laptop 2 trong 1', N'Laptop xoay gập 360 độ, màn hình cảm ứng'),
(N'Laptop workstation', N'Máy trạm di động chuyên kỹ thuật và dựng phim 3D'),
(N'Laptop AI', N'Laptop thế hệ mới tích hợp NPU xử lý trí tuệ nhân tạo'),
(N'Laptop màn hình OLED', N'Laptop trang bị tấm nền OLED chuẩn màu cao'),
(N'Laptop pin trâu', N'Laptop thời lượng pin trên 10 giờ làm việc'),
(N'Laptop lập trình', N'Laptop RAM lớn, bàn phím tốt cho lập trình viên'),
(N'Laptop siêu nhẹ dưới 1kg', N'Laptop hợp kim magie/carbon siêu di động'),
(N'Laptop phổ thông', N'Laptop tầm giá dưới 15 triệu đồng'),
(N'Laptop màn hình lớn 17 inch', N'Laptop không gian hiển thị rộng rãi'),
(N'Laptop chuyên sáng tạo nội dung', N'Laptop dành cho Creator, Streamer, Editor'),
(N'Laptop tiêu chuẩn quân đội', N'Laptop đạt chuẩn độ bền MIL-STD-810H'),
(N'Laptop cảm ứng đa điểm', N'Laptop hỗ trợ bút cảm ứng viết vẽ trực tiếp'),
(N'Laptop doanh nghiệp SME', N'Laptop đồng bộ dễ quản trị cho doanh nghiệp');
GO

-- 4. BRANDS - 20 THƯƠNG HIỆU
INSERT INTO Brands (BrandName, Country, Description)
VALUES
(N'Dell', N'Mỹ', N'Thương hiệu máy tính Dell'),
(N'HP', N'Mỹ', N'Thương hiệu máy tính HP'),
(N'Lenovo', N'Trung Quốc', N'Thương hiệu máy tính Lenovo'),
(N'ASUS', N'Đài Loan', N'Thương hiệu máy tính ASUS'),
(N'Acer', N'Đài Loan', N'Thương hiệu máy tính Acer'),
(N'Apple', N'Mỹ', N'Thương hiệu máy tính Apple'),
(N'MSI', N'Đài Loan', N'Thương hiệu máy tính MSI'),
(N'Gigabyte', N'Đài Loan', N'Thương hiệu máy tính Gigabyte'),
(N'LG', N'Hàn Quốc', N'Thương hiệu laptop mỏng nhẹ LG Gram'),
(N'Microsoft', N'Mỹ', N'Dòng máy tính Microsoft Surface'),
(N'Samsung', N'Hàn Quốc', N'Dòng máy tính Samsung Galaxy Book'),
(N'Razer', N'Mỹ', N'Thương hiệu laptop gaming cao cấp Razer Blade'),
(N'Alienware', N'Mỹ', N'Thương hiệu laptop gaming đầu bảng của Dell'),
(N'Huawei', N'Trung Quốc', N'Dòng laptop mỏng nhẹ Huawei MateBook'),
(N'Xiaomi', N'Trung Quốc', N'Dòng laptop RedmiBook và Mi Notebook'),
(N'Fujitsu', N'Nhật Bản', N'Thương hiệu laptop bền bỉ Nhật Bản Fujitsu'),
(N'Dynabook', N'Nhật Bản', N'Thương hiệu laptop kế thừa từ Toshiba'),
(N'Vaio', N'Nhật Bản', N'Thương hiệu laptop doanh nhân cao cấp Vaio'),
(N'Framework', N'Mỹ', N'Thương hiệu laptop module dễ nâng cấp sửa chữa'),
(N'Avita', N'Hồng Kông', N'Thương hiệu laptop thời trang trẻ trung Avita');
GO

-- 5. LAPTOPS - 20 SẢN PHẨM
INSERT INTO Laptops
(SKU, LaptopName, CategoryID, BrandID, CPU, RAM_GB,
 Storage_GB, GPU, ScreenSize, OperatingSystem, Color,
 PurchasePrice, SalePrice, StockQuantity, ImageURL, Description)
VALUES
('LT001', N'Dell Inspiron 15', 1, 1, N'Intel Core i5-1235U', 8, 512, N'Intel Iris Xe', 15.6, N'Windows 11', N'Bạc', 12000000, 14990000, 15, NULL, N'Laptop văn phòng Dell'),
('LT002', N'Dell Vostro 14', 1, 1, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe', 14.0, N'Windows 11', N'Đen', 14000000, 16990000, 12, NULL, N'Laptop làm việc văn phòng'),
('LT003', N'HP Pavilion 15', 1, 2, N'Intel Core i5-1235U', 16, 512, N'Intel Iris Xe', 15.6, N'Windows 11', N'Bạc', 13500000, 15990000, 10, NULL, N'Laptop học tập và văn phòng'),
('LT004', N'HP Victus 16', 2, 2, N'Intel Core i7-13700H', 16, 512, N'RTX 4050', 16.1, N'Windows 11', N'Đen', 22000000, 25990000, 8, NULL, N'Laptop gaming HP'),
('LT005', N'Lenovo IdeaPad Slim 5', 4, 3, N'AMD Ryzen 5 7530U', 16, 512, N'Radeon Graphics', 14.0, N'Windows 11', N'Xám', 13000000, 15990000, 14, NULL, N'Laptop mỏng nhẹ Lenovo'),
('LT006', N'Lenovo LOQ 15', 2, 3, N'Intel Core i5-13420H', 16, 512, N'RTX 4050', 15.6, N'Windows 11', N'Xám', 19000000, 22990000, 9, NULL, N'Laptop gaming Lenovo'),
('LT007', N'ASUS Vivobook 15', 1, 4, N'Intel Core i5-12500H', 16, 512, N'Intel Iris Xe', 15.6, N'Windows 11', N'Bạc', 13000000, 15990000, 16, NULL, N'Laptop văn phòng ASUS'),
('LT008', N'ASUS TUF Gaming A15', 2, 4, N'AMD Ryzen 7 7735HS', 16, 512, N'RTX 4060', 15.6, N'Windows 11', N'Đen', 24000000, 28990000, 7, NULL, N'Laptop gaming ASUS'),
('LT009', N'Acer Aspire 5', 1, 5, N'Intel Core i5-12450H', 8, 512, N'Intel UHD', 15.6, N'Windows 11', N'Bạc', 11000000, 13990000, 18, NULL, N'Laptop học tập Acer'),
('LT010', N'Acer Nitro V 15', 2, 5, N'Intel Core i5-13420H', 16, 512, N'RTX 4050', 15.6, N'Windows 11', N'Đen', 19000000, 22990000, 11, NULL, N'Laptop gaming Acer'),
('LT011', N'MacBook Air M2', 4, 6, N'Apple M2', 8, 256, N'Apple GPU', 13.6, N'macOS', N'Xám', 20000000, 23990000, 6, NULL, N'MacBook Air chip M2'),
('LT012', N'MacBook Pro 14', 5, 6, N'Apple M3 Pro', 18, 512, N'Apple GPU', 14.2, N'macOS', N'Bạc', 42000000, 46990000, 4, NULL, N'MacBook Pro cao cấp'),
('LT013', N'MSI Thin 15', 2, 7, N'Intel Core i5-12450H', 16, 512, N'RTX 4050', 15.6, N'Windows 11', N'Đen', 18000000, 21990000, 10, NULL, N'Laptop gaming MSI'),
('LT014', N'MSI Katana 15', 2, 7, N'Intel Core i7-13620H', 16, 512, N'RTX 4060', 15.6, N'Windows 11', N'Đen', 25000000, 29990000, 5, NULL, N'Laptop gaming hiệu năng cao'),
('LT015', N'Gigabyte G5', 2, 8, N'Intel Core i5-12500H', 16, 512, N'RTX 4060', 15.6, N'Windows 11', N'Đen', 21000000, 24990000, 8, NULL, N'Laptop gaming Gigabyte'),
('LT016', N'Dell XPS 13', 5, 1, N'Intel Core i7-1355U', 16, 512, N'Intel Iris Xe', 13.4, N'Windows 11', N'Bạc', 28000000, 32990000, 3, NULL, N'Laptop cao cấp Dell'),
('LT017', N'HP Envy x360', 4, 2, N'AMD Ryzen 5 7530U', 16, 512, N'Radeon Graphics', 15.6, N'Windows 11', N'Bạc', 19000000, 22990000, 6, NULL, N'Laptop xoay gập HP'),
('LT018', N'Lenovo ThinkPad E14', 1, 3, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe', 14.0, N'Windows 11', N'Đen', 17000000, 19990000, 13, NULL, N'Laptop doanh nghiệp Lenovo'),
('LT019', N'ASUS Zenbook 14', 4, 4, N'Intel Core Ultra 5', 16, 512, N'Intel Arc', 14.0, N'Windows 11', N'Xanh', 21000000, 24990000, 9, NULL, N'Laptop mỏng nhẹ ASUS'),
('LT020', N'Acer Swift Go 14', 4, 5, N'Intel Core Ultra 5', 16, 512, N'Intel Arc', 14.0, N'Windows 11', N'Bạc', 18000000, 21990000, 7, NULL, N'Laptop mỏng nhẹ Acer');
GO

-- 6. CUSTOMERS - 20 KHÁCH HÀNG (2 khách liên kết tài khoản, 18 khách vãng lai AccountID = NULL)
INSERT INTO Customers
(AccountID, CustomerName, Email, Phone, Address)
VALUES
(4,    N'Nguyễn Văn An',   'an01@gmail.com',    '0911000001', N'Quận 1, TP.HCM'),
(5,    N'Trần Thị Bình',   'binh02@gmail.com',  '0911000002', N'Quận 3, TP.HCM'),
(NULL, N'Lê Minh Cường',   'cuong03@gmail.com', '0911000003', N'Quận 5, TP.HCM'),
(NULL, N'Phạm Thị Dung',   'dung04@gmail.com',  '0911000004', N'Quận 7, TP.HCM'),
(NULL, N'Hoàng Văn Đức',   'duc05@gmail.com',   '0911000005', N'Thủ Đức, TP.HCM'),
(NULL, N'Võ Thị Giang',    'giang06@gmail.com', '0911000006', N'Quận 10, TP.HCM'),
(NULL, N'Đặng Minh Hải',   'hai07@gmail.com',   '0911000007', N'Gò Vấp, TP.HCM'),
(NULL, N'Bùi Thị Hạnh',    'hanh08@gmail.com',  '0911000008', N'Bình Thạnh, TP.HCM'),
(NULL, N'Đỗ Quốc Huy',     'huy09@gmail.com',   '0911000009', N'Tân Bình, TP.HCM'),
(NULL, N'Ngô Thị Lan',     'lan10@gmail.com',   '0911000010', N'Tân Phú, TP.HCM'),
(NULL, N'Phan Văn Long',   'long11@gmail.com',  '0911000011', N'Quận 12, TP.HCM'),
(NULL, N'Vũ Thị Mai',      'mai12@gmail.com',   '0911000012', N'Nhà Bè, TP.HCM'),
(NULL, N'Đinh Quốc Nam',   'nam13@gmail.com',   '0911000013', N'Bình Chánh, TP.HCM'),
(NULL, N'Nguyễn Thị Oanh', 'oanh14@gmail.com',  '0911000014', N'Quận 4, TP.HCM'),
(NULL, N'Trương Minh Phúc','phuc15@gmail.com',  '0911000015', N'Quận 6, TP.HCM'),
(NULL, N'Lý Thị Quỳnh',    'quynh16@gmail.com', '0911000016', N'Quận 8, TP.HCM'),
(NULL, N'Đào Văn Sơn',     'son17@gmail.com',   '0911000017', N'Bình Tân, TP.HCM'),
(NULL, N'Phạm Minh Tâm',   'tam18@gmail.com',   '0911000018', N'Quận 11, TP.HCM'),
(NULL, N'Hoàng Thị Uyên',  'uyen19@gmail.com',  '0911000019', N'Củ Chi, TP.HCM'),
(NULL, N'Đặng Văn Việt',   'viet20@gmail.com',  '0911000020', N'Hóc Môn, TP.HCM');
GO

-- 7. ORDERS - 20 ĐƠN HÀNG
INSERT INTO Orders
(OrderCode, CustomerID, CreatedBy, RecipientName, RecipientPhone,
 DeliveryAddress, OrderStatus, Subtotal, Discount, ShippingFee,
 TotalAmount, Note)
SELECT
    'DH' + RIGHT('000' + CAST(l.LaptopID AS VARCHAR(3)), 3),
    l.LaptopID,
    2,
    c.CustomerName,
    c.Phone,
    c.Address,
    CASE
        WHEN l.LaptopID <= 5 THEN N'COMPLETED'
        WHEN l.LaptopID <= 10 THEN N'SHIPPING'
        WHEN l.LaptopID <= 15 THEN N'CONFIRMED'
        ELSE N'PENDING'
    END,
    l.SalePrice,
    0,
    30000,
    l.SalePrice + 30000,
    N'Đơn hàng dữ liệu mẫu'
FROM Laptops l
JOIN Customers c ON c.CustomerID = l.LaptopID
WHERE l.LaptopID BETWEEN 1 AND 20;
GO

-- 8. ORDERDETAILS - 20 CHI TIẾT ĐƠN HÀNG
INSERT INTO OrderDetails
(OrderID, LaptopID, SKUSnapshot, LaptopNameSnapshot,
 UnitPrice, Quantity, LineTotal)
SELECT
    o.OrderID,
    l.LaptopID,
    l.SKU,
    l.LaptopName,
    l.SalePrice,
    1,
    l.SalePrice
FROM Orders o
JOIN Laptops l
    ON o.OrderCode = 'DH' + RIGHT('000' + CAST(l.LaptopID AS VARCHAR(3)), 3)
WHERE l.LaptopID BETWEEN 1 AND 20;
GO

-- 9. PAYMENTS - 20 THANH TOÁN
INSERT INTO Payments
(OrderID, PaymentMethod, PaymentStatus, Amount, TransactionCode, PaidAt)
SELECT
    OrderID,
    CASE WHEN OrderID % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CASE WHEN OrderStatus = N'COMPLETED' THEN N'PAID' ELSE N'UNPAID' END,
    TotalAmount,
    'GD' + RIGHT('000' + CAST(OrderID AS VARCHAR(3)), 3),
    CASE WHEN OrderStatus = N'COMPLETED' THEN SYSDATETIME() ELSE NULL END
FROM Orders;
GO

-- 10. ORDERSTATUSHISTORY - 20 LỊCH SỬ TRẠNG THÁI
INSERT INTO OrderStatusHistory
(OrderID, OldStatus, NewStatus, ChangedBy, ChangeNote)
SELECT
    OrderID,
    NULL,
    OrderStatus,
    2,
    N'Tạo lịch sử trạng thái dữ liệu mẫu'
FROM Orders;
GO

-- =========================================
-- KIỂM TRA DỮ LIỆU & TRUY VẤN NGHIỆP VỤ
-- =========================================

-- Thống kê số dòng của toàn bộ 10 bảng
SELECT 'Roles' AS TableName, COUNT(*) AS TotalRows FROM Roles
UNION ALL
SELECT 'Accounts', COUNT(*) FROM Accounts
UNION ALL
SELECT 'Categories', COUNT(*) FROM Categories
UNION ALL
SELECT 'Brands', COUNT(*) FROM Brands
UNION ALL
SELECT 'Laptops', COUNT(*) FROM Laptops
UNION ALL
SELECT 'Customers', COUNT(*) FROM Customers
UNION ALL
SELECT 'Orders', COUNT(*) FROM Orders
UNION ALL
SELECT 'OrderDetails', COUNT(*) FROM OrderDetails
UNION ALL
SELECT 'Payments', COUNT(*) FROM Payments
UNION ALL
SELECT 'OrderStatusHistory', COUNT(*) FROM OrderStatusHistory;
GO

-- Truy vấn 1: Danh sách Laptop kèm Danh mục và Thương hiệu
SELECT
    L.LaptopID,
    L.SKU,
    L.LaptopName,
    C.CategoryName,
    B.BrandName,
    L.CPU,
    L.RAM_GB,
    L.Storage_GB,
    L.SalePrice,
    L.StockQuantity
FROM Laptops L
INNER JOIN Categories C ON L.CategoryID = C.CategoryID
INNER JOIN Brands B ON L.BrandID = B.BrandID
WHERE L.IsActive = 1
ORDER BY L.LaptopID;
GO

-- Truy vấn 2: Thống kê doanh thu theo tháng
SELECT
    YEAR(OrderDate) AS Nam,
    MONTH(OrderDate) AS Thang,
    COUNT(OrderID) AS SoDonHang,
    SUM(TotalAmount) AS DoanhThu
FROM Orders
WHERE OrderStatus = N'COMPLETED'
GROUP BY YEAR(OrderDate), MONTH(OrderDate)
ORDER BY Nam, Thang;
GO

-- Truy vấn 3: Top 10 laptop bán chạy nhất
SELECT TOP 10
    L.LaptopID,
    L.SKU,
    L.LaptopName,
    SUM(OD.Quantity) AS TongSoLuongBan,
    SUM(OD.LineTotal) AS TongTienBan
FROM OrderDetails OD
INNER JOIN Laptops L ON OD.LaptopID = L.LaptopID
INNER JOIN Orders O ON OD.OrderID = O.OrderID
WHERE O.OrderStatus NOT IN (N'CANCELLED', N'RETURNED')
GROUP BY L.LaptopID, L.SKU, L.LaptopName
ORDER BY TongSoLuongBan DESC, TongTienBan DESC;
GO

-- Truy vấn 4: Cảnh báo laptop tồn kho thấp (StockQuantity <= 5)
SELECT
    LaptopID,
    SKU,
    LaptopName,
    StockQuantity,
    SalePrice
FROM Laptops
WHERE StockQuantity <= 5
  AND IsActive = 1
ORDER BY StockQuantity ASC;
GO
