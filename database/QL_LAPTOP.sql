
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF DB_ID(N'QL_Laptop') IS NULL
BEGIN
    CREATE DATABASE QL_Laptop;
END
GO

USE QL_Laptop;
GO


-- =========================================
-- 1. BẢNG VAI TRÒ (VaiTro)
-- =========================================
CREATE TABLE VaiTro (
    MaVaiTro INT IDENTITY(1,1) PRIMARY KEY,
    TenVaiTro NVARCHAR(50) NOT NULL UNIQUE,
    MoTa NVARCHAR(255)
);
GO

-- =========================================
-- 2. BẢNG TÀI KHOẢN (TaiKhoan)
-- =========================================
CREATE TABLE TaiKhoan (
    MaTaiKhoan INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap VARCHAR(50) NOT NULL UNIQUE,
    MatKhauMaHoa VARCHAR(255) NOT NULL,
    HoTen NVARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE,
    SoDienThoai VARCHAR(15),
    MaVaiTro INT NOT NULL,
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_TaiKhoan_VaiTro
        FOREIGN KEY (MaVaiTro) REFERENCES VaiTro(MaVaiTro)
);
GO

-- =========================================
-- 3. BẢNG DANH MỤC LAPTOP (DanhMucLaptop)
-- =========================================
CREATE TABLE DanhMucLaptop (
    MaDanhMuc INT IDENTITY(1,1) PRIMARY KEY,
    TenDanhMuc NVARCHAR(100) NOT NULL UNIQUE,
    MoTa NVARCHAR(500),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- =========================================
-- 4. BẢNG THƯƠNG HIỆU (ThuongHieu)
-- =========================================
CREATE TABLE ThuongHieu (
    MaThuongHieu INT IDENTITY(1,1) PRIMARY KEY,
    TenThuongHieu NVARCHAR(100) NOT NULL UNIQUE,
    QuocGia NVARCHAR(100),
    MoTa NVARCHAR(500),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1
);
GO

-- =========================================
-- 5. BẢNG LAPTOP (Laptop) - Ràng buộc thông số chuẩn thực tế
-- =========================================
CREATE TABLE Laptop (
    MaLaptop INT IDENTITY(1,1) PRIMARY KEY,
    MaSKU VARCHAR(30) NOT NULL UNIQUE,
    TenLaptop NVARCHAR(200) NOT NULL,

    MaDanhMuc INT NOT NULL,
    MaThuongHieu INT NOT NULL,

    CPU NVARCHAR(150),
    RAM_GB INT,
    DungLuongSSD_GB INT,
    CardDoHoa NVARCHAR(150),
    KichThuocManHinh DECIMAL(4,1),
    HeDieuHanh NVARCHAR(100),
    MauSac NVARCHAR(50),

    GiaNhap DECIMAL(18,2) NOT NULL DEFAULT 0,
    GiaNiemYet DECIMAL(18,2) NOT NULL DEFAULT 0,
    GiaBan DECIMAL(18,2) NOT NULL,
    SoLuongTon INT NOT NULL DEFAULT 0,

    DuongDanAnh NVARCHAR(500),
    MoTa NVARCHAR(MAX),

    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NgayCapNhat DATETIME2,

    CONSTRAINT FK_Laptop_DanhMucLaptop
        FOREIGN KEY (MaDanhMuc) REFERENCES DanhMucLaptop(MaDanhMuc),

    CONSTRAINT FK_Laptop_ThuongHieu
        FOREIGN KEY (MaThuongHieu) REFERENCES ThuongHieu(MaThuongHieu),

    CONSTRAINT CK_Laptop_Gia
        CHECK (GiaNhap >= 0 AND GiaNiemYet >= 0 AND GiaBan >= 0),

    CONSTRAINT CK_Laptop_SoLuongTon
        CHECK (SoLuongTon >= 0),

    CONSTRAINT CK_Laptop_RAM
        CHECK (RAM_GB IS NULL OR RAM_GB IN (4, 8, 12, 16, 18, 24, 32, 36, 48, 64, 128)),

    CONSTRAINT CK_Laptop_DungLuongSSD
        CHECK (DungLuongSSD_GB IS NULL OR DungLuongSSD_GB IN (128, 256, 512, 1024, 2048, 4096)),

    CONSTRAINT CK_Laptop_ManHinh
        CHECK (KichThuocManHinh IS NULL OR KichThuocManHinh IN (13.0, 13.3, 13.4, 13.6, 14.0, 14.2, 14.5, 15.3, 15.6, 16.0, 16.1, 17.0, 17.3, 18.0))
);
GO

-- =========================================
-- 6. BẢNG KHÁCH HÀNG (KhachHang)
-- =========================================
CREATE TABLE KhachHang (
    MaKhachHang INT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NULL,
    TenKhachHang NVARCHAR(100) NOT NULL,
    Email VARCHAR(100),
    SoDienThoai VARCHAR(15) NOT NULL,
    DiaChi NVARCHAR(300),
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_KhachHang_TaiKhoan
        FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan)
);
GO

-- =========================================
-- 7. BẢNG ĐƠN HÀNG (DonHang)
-- =========================================
CREATE TABLE DonHang (
    MaDonHang INT IDENTITY(1,1) PRIMARY KEY,
    MaCodeDonHang VARCHAR(30) NOT NULL UNIQUE,

    MaKhachHang INT,
    NguoiTao INT,

    TenNguoiNhan NVARCHAR(100) NOT NULL,
    SoDienThoaiNhan VARCHAR(15) NOT NULL,
    DiaChiGiaoHang NVARCHAR(300) NOT NULL,

    NgayDatHang DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    TrangThaiDonHang NVARCHAR(20) NOT NULL DEFAULT N'PENDING',

    TamTinh DECIMAL(18,2) NOT NULL DEFAULT 0,
    GiamGia DECIMAL(18,2) NOT NULL DEFAULT 0,
    PhiVanChuyen DECIMAL(18,2) NOT NULL DEFAULT 0,
    TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,

    GhiChu NVARCHAR(500),
    NgayCapNhat DATETIME2,

    CONSTRAINT FK_DonHang_KhachHang
        FOREIGN KEY (MaKhachHang) REFERENCES KhachHang(MaKhachHang),

    CONSTRAINT FK_DonHang_TaiKhoan
        FOREIGN KEY (NguoiTao) REFERENCES TaiKhoan(MaTaiKhoan),

    CONSTRAINT CK_DonHang_TrangThai
        CHECK (TrangThaiDonHang IN (
            N'PENDING', N'CONFIRMED', N'SHIPPING',
            N'COMPLETED', N'CANCELLED', N'RETURNED'
        )),

    CONSTRAINT CK_DonHang_Tien
        CHECK (TamTinh >= 0 AND GiamGia >= 0 AND PhiVanChuyen >= 0 AND TongTien >= 0)
);
GO

-- =========================================
-- 8. BẢNG CHI TIẾT ĐƠN HÀNG (ChiTietDonHang)
-- =========================================
CREATE TABLE ChiTietDonHang (
    MaChiTietDonHang INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,
    MaLaptop INT,

    MaSKULuuTru VARCHAR(30) NOT NULL,
    TenLaptopLuuTru NVARCHAR(200) NOT NULL,

    DonGia DECIMAL(18,2) NOT NULL,
    SoLuong INT NOT NULL,
    ThanhTien DECIMAL(18,2) NOT NULL,

    CONSTRAINT FK_ChiTietDonHang_DonHang
        FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),

    CONSTRAINT FK_ChiTietDonHang_Laptop
        FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),

    CONSTRAINT CK_ChiTietDonHang_DonGia CHECK (DonGia >= 0),
    CONSTRAINT CK_ChiTietDonHang_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietDonHang_ThanhTien CHECK (ThanhTien >= 0)
);
GO

-- =========================================
-- 9. BẢNG THANH TOÁN (ThanhToan)
-- =========================================
CREATE TABLE ThanhToan (
    MaThanhToan INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,

    PhuongThucThanhToan NVARCHAR(30) NOT NULL,
    TrangThaiThanhToan NVARCHAR(20) NOT NULL DEFAULT N'UNPAID',

    SoTien DECIMAL(18,2) NOT NULL,
    MaGiaoDich VARCHAR(100),
    ThoiGianThanhToan DATETIME2,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_ThanhToan_DonHang
        FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),

    CONSTRAINT CK_ThanhToan_PhuongThuc
        CHECK (PhuongThucThanhToan IN (N'CASH', N'BANK_TRANSFER', N'CARD', N'E_WALLET')),

    CONSTRAINT CK_ThanhToan_TrangThai
        CHECK (TrangThaiThanhToan IN (N'UNPAID', N'PENDING', N'PAID', N'FAILED', N'REFUNDED')),

    CONSTRAINT CK_ThanhToan_SoTien CHECK (SoTien >= 0)
);
GO

-- =========================================
-- 10. BẢNG LỊCH SỬ TRẠNG THÁI ĐƠN HÀNG (LichSuTrangThaiDonHang)
-- =========================================
CREATE TABLE LichSuTrangThaiDonHang (
    MaLichSu INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,
    TrangThaiCu NVARCHAR(20),
    TrangThaiMoi NVARCHAR(20) NOT NULL,
    NguoiThayDoi INT,
    GhiChuThayDoi NVARCHAR(500),
    ThoiGianThayDoi DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_LichSu_DonHang
        FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),

    CONSTRAINT FK_LichSu_TaiKhoan
        FOREIGN KEY (NguoiThayDoi) REFERENCES TaiKhoan(MaTaiKhoan),

    CONSTRAINT CK_LichSu_TrangThaiMoi
        CHECK (TrangThaiMoi IN (
            N'PENDING', N'CONFIRMED', N'SHIPPING',
            N'COMPLETED', N'CANCELLED', N'RETURNED'
        ))
);
GO

-- =========================================
-- INDEX TỐI ƯU TÌM KIẾM & RÀNG BUỘC DUY NHẤT CÓ ĐIỀU KIỆN
-- =========================================
CREATE INDEX IX_Laptop_TenLaptop ON Laptop(TenLaptop);
CREATE INDEX IX_Laptop_MaDanhMuc ON Laptop(MaDanhMuc);
CREATE INDEX IX_Laptop_MaThuongHieu ON Laptop(MaThuongHieu);
CREATE INDEX IX_DonHang_NgayDatHang ON DonHang(NgayDatHang);
CREATE INDEX IX_DonHang_TrangThai ON DonHang(TrangThaiDonHang);
CREATE INDEX IX_KhachHang_TenKhachHang ON KhachHang(TenKhachHang);

CREATE UNIQUE NONCLUSTERED INDEX UX_KhachHang_MaTaiKhoan
ON KhachHang(MaTaiKhoan)
WHERE MaTaiKhoan IS NOT NULL;
GO

-- =========================================
-- DỮ LIỆU MẪU CHUẨN THỰC TẾ (TỐI THIỂU 20 BẢN GHI MỖI BẢNG CHÍNH)
-- =========================================

-- 1. VAI TRÒ
INSERT INTO VaiTro (TenVaiTro, MoTa) VALUES
(N'ADMIN', N'Quản trị hệ thống'),
(N'STAFF', N'Nhân viên cửa hàng'),
(N'CUSTOMER', N'Khách hàng');
GO

-- 2. TÀI KHOẢN (Mật khẩu mặc định: 123456 - đã mã hóa BCrypt chuẩn)
INSERT INTO TaiKhoan (TenDangNhap, MatKhauMaHoa, HoTen, Email, SoDienThoai, MaVaiTro) VALUES
('admin',   '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Quản trị viên',  'admin@qlLaptop.com',   '0900000001', 1),
('staff01', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nhân viên 01',   'staff01@qlLaptop.com', '0900000002', 2),
('staff02', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nhân viên 02',   'staff02@qlLaptop.com', '0900000003', 2),
('khach01', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nguyễn Văn An',  'an01@gmail.com',       '0911000001', 3),
('khach02', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Trần Thị Bình',  'binh02@gmail.com',     '0911000002', 3);
GO

-- 3. DANH MỤC LAPTOP (20 DANH MỤC)
INSERT INTO DanhMucLaptop (TenDanhMuc, MoTa) VALUES
(N'Laptop văn phòng', N'Laptop phục vụ công việc văn phòng'),
(N'Laptop gaming', N'Laptop chuyên chơi game hiệu năng cao'),
(N'Laptop đồ họa', N'Laptop phục vụ thiết kế và đồ họa'),
(N'Laptop mỏng nhẹ', N'Laptop có thiết kế mỏng nhẹ di động'),
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

-- 4. THƯƠNG HIỆU (20 THƯƠNG HIỆU)
INSERT INTO ThuongHieu (TenThuongHieu, QuocGia, MoTa) VALUES
(N'Dell', N'Mỹ', N'Thương hiệu máy tính Dell'),
(N'HP', N'Mỹ', N'Thương hiệu máy tính HP'),
(N'Lenovo', N'Trung Quốc', N'Thương hiệu máy tính Lenovo'),
(N'ASUS', N'Đài Loan', N'Thương hiệu máy tính ASUS'),
(N'Acer', N'Đài Loan', N'Thương hiệu máy tính Acer'),
(N'Apple', N'Mỹ', N'Thương hiệu máy tính Apple MacBook'),
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

-- 5. LAPTOP (20 SẢN PHẨM THÔNG SỐ CHUẨN THỰC TẾ + GIÁ NIÊM YẾT & GIẢM GIÁ THEO PHONG VŨ)
INSERT INTO Laptop
(MaSKU, TenLaptop, MaDanhMuc, MaThuongHieu, CPU, RAM_GB, DungLuongSSD_GB, CardDoHoa, KichThuocManHinh, HeDieuHanh, MauSac,
 GiaNhap, GiaNiemYet, GiaBan, SoLuongTon, DuongDanAnh, MoTa)
VALUES
('LT001', N'Dell Inspiron 15 3530', 1, 1, N'Intel Core i5-1335U', 8, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13000000, 17990000, 14990000, 15, NULL, N'Laptop văn phòng Dell Inspiron 15 thiết kế bền bỉ, màn hình 15.6 inch Full HD sắc nét.'),
('LT002', N'Dell Vostro 14 3430', 7, 1, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe Graphics', 14.0, N'Windows 11 Home', N'Đen', 14500000, 19490000, 16990000, 12, NULL, N'Laptop doanh nghiệp Dell Vostro 14 nhỏ gọn, bảo mật vân tay, hiệu năng ổn định.'),
('LT003', N'HP Pavilion 15-eg3091TU', 1, 2, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13800000, 18490000, 15990000, 10, NULL, N'Laptop HP Pavilion 15 vỏ kim loại sang trọng, phù hợp học tập và văn phòng.'),
('LT004', N'HP Victus 16-r0127TX', 2, 2, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 16.1, N'Windows 11 Home', N'Đen', 23500000, 30990000, 27490000, 8, NULL, N'Laptop gaming HP Victus màn hình lớn 16.1 inch 144Hz, tản nhiệt tối ưu.'),
('LT005', N'Lenovo IdeaPad Slim 5 14', 4, 3, N'AMD Ryzen 5 7530U', 16, 512, N'AMD Radeon Graphics', 14.0, N'Windows 11 Home', N'Xám', 13500000, 17490000, 15990000, 14, NULL, N'Laptop mỏng nhẹ Lenovo IdeaPad Slim 5 vỏ nhôm cao cấp, thời lượng pin ấn tượng.'),
('LT006', N'Lenovo LOQ 15IRX9', 2, 3, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Xám', 20000000, 26990000, 22990000, 9, NULL, N'Laptop gaming Lenovo LOQ 15 cấu hình mạnh mẽ cân mọi tựa game Esport và AAA.'),
('LT007', N'ASUS Vivobook 15 X1504ZA', 1, 4, N'Intel Core i5-1235U', 16, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13200000, 17990000, 15990000, 16, NULL, N'Laptop ASUS Vivobook 15 bản lề mở 180 độ, kháng khuẩn ASUS Antimicrobial Guard.'),
('LT008', N'ASUS TUF Gaming A15 FA507', 2, 4, N'AMD Ryzen 7 8845HS', 16, 512, N'NVIDIA GeForce RTX 4060', 15.6, N'Windows 11 Home', N'Đen', 25000000, 32990000, 28990000, 7, NULL, N'Laptop gaming chuẩn độ bền quân đội MIL-STD-810H, màn hình 144Hz chuẩn màu sRGB.'),
('LT009', N'Acer Aspire 5 A515-58P', 6, 5, N'Intel Core i5-1335U', 8, 512, N'Intel UHD Graphics', 15.6, N'Windows 11 Home', N'Bạc', 11500000, 16990000, 13990000, 18, NULL, N'Laptop sinh viên Acer Aspire 5 giá tốt, dễ dàng nâng cấp RAM và SSD.'),
('LT010', N'Acer Nitro V 15 ANV15-51', 2, 5, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Đen', 19500000, 25990000, 22990000, 11, NULL, N'Laptop gaming quốc dân Acer Nitro V 15 tản nhiệt 2 quạt, hiệu năng vượt tầm giá.'),
('LT011', N'Apple MacBook Air 13 M2', 4, 6, N'Apple M2', 16, 512, N'Apple GPU 10-Core', 13.6, N'macOS', N'Xám', 21000000, 27990000, 23990000, 6, NULL, N'MacBook Air M2 màn hình Liquid Retina 13.6 inch, pin lên tới 18 giờ.'),
('LT012', N'Apple MacBook Pro 14 M4', 5, 6, N'Apple M4', 16, 1024, N'Apple GPU 10-Core', 14.2, N'macOS', N'Đen', 39000000, 46990000, 44990000, 4, NULL, N'MacBook Pro 14 chip M4 chuyên dựng phim, lập trình và đồ họa chuyên nghiệp.'),
('LT013', N'MSI Thin 15 B13UC', 2, 7, N'Intel Core i7-13620H', 16, 512, N'NVIDIA GeForce RTX 3050', 15.6, N'Windows 11 Home', N'Đen', 18500000, 23990000, 21990000, 10, NULL, N'Laptop gaming mỏng nhẹ MSI Thin 15 trọng lượng chỉ 1.86kg.'),
('LT014', N'MSI Katana 15 B13VEK', 2, 7, N'Intel Core i7-13620H', 16, 1024, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Đen', 26000000, 35490000, 29990000, 5, NULL, N'MSI Katana 15 trang bị bàn phím RGB 4 vùng, SSD 1TB siêu tốc.'),
('LT015', N'Gigabyte Gaming G5 MF5', 2, 8, N'Intel Core i7-13620H', 16, 512, N'NVIDIA GeForce RTX 4060', 15.6, N'Windows 11 Home', N'Đen', 22000000, 28490000, 24990000, 8, NULL, N'Gigabyte G5 hiệu năng RTX 4060 cực mạnh cho game thủ và kỹ thuật.'),
('LT016', N'Dell XPS 13 9340', 5, 1, N'Intel Core Ultra 7', 16, 1024, N'Intel Arc Graphics', 13.4, N'Windows 11 Pro', N'Bạc', 38000000, 46990000, 46990000, 3, NULL, N'Siêu phẩm doanh nhân Dell XPS 13 viền siêu mỏng InfinityEdge, vỏ nhôm CNC.'),
('LT017', N'HP Envy x360 16 AI', 8, 2, N'AMD Ryzen 5 7530U', 16, 512, N'AMD Radeon Graphics', 16.0, N'Windows 11 Home', N'Bạc', 21000000, 26990000, 22990000, 6, NULL, N'Laptop xoay gập 360 độ HP Envy x360 màn hình cảm ứng đa điểm sắc nét.'),
('LT018', N'Lenovo ThinkPad E14 Gen 6', 7, 3, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Pro', N'Đen', 17500000, 22990000, 19990000, 13, NULL, N'Lenovo ThinkPad E14 bàn phím trứ danh, độ bền chuẩn quân đội cho lập trình viên.'),
('LT019', N'ASUS Zenbook 14 OLED', 11, 4, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Home', N'Xanh', 22000000, 27990000, 24990000, 9, NULL, N'ASUS Zenbook 14 màn hình Lumina OLED 3K chuẩn màu 100% DCI-P3.'),
('LT020', N'Acer Swift Go 14 AI', 10, 5, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Home', N'Bạc', 19000000, 24990000, 21990000, 7, NULL, N'Acer Swift Go 14 AI siêu mỏng nhẹ 1.3kg tích hợp NPU xử lý AI.');
GO

-- 6. KHÁCH HÀNG (20 KHÁCH HÀNG)
INSERT INTO KhachHang (MaTaiKhoan, TenKhachHang, Email, SoDienThoai, DiaChi) VALUES
(4,    N'Nguyễn Văn An',    'an01@gmail.com',    '0911000001', N'Quận 1, TP.HCM'),
(5,    N'Trần Thị Bình',    'binh02@gmail.com',  '0911000002', N'Quận 3, TP.HCM'),
(NULL, N'Lê Minh Cường',    'cuong03@gmail.com', '0911000003', N'Quận 5, TP.HCM'),
(NULL, N'Phạm Thị Dung',    'dung04@gmail.com',  '0911000004', N'Quận 7, TP.HCM'),
(NULL, N'Hoàng Văn Đức',    'duc05@gmail.com',   '0911000005', N'Thủ Đức, TP.HCM'),
(NULL, N'Võ Thị Giang',     'giang06@gmail.com', '0911000006', N'Quận 10, TP.HCM'),
(NULL, N'Đặng Minh Hải',    'hai07@gmail.com',   '0911000007', N'Gò Vấp, TP.HCM'),
(NULL, N'Bùi Thị Hạnh',     'hanh08@gmail.com',  '0911000008', N'Bình Thạnh, TP.HCM'),
(NULL, N'Đỗ Quốc Huy',      'huy09@gmail.com',   '0911000009', N'Tân Bình, TP.HCM'),
(NULL, N'Ngô Thị Lan',      'lan10@gmail.com',   '0911000010', N'Tân Phú, TP.HCM'),
(NULL, N'Phan Văn Long',    'long11@gmail.com',  '0911000011', N'Quận 12, TP.HCM'),
(NULL, N'Vũ Thị Mai',       'mai12@gmail.com',   '0911000012', N'Nhà Bè, TP.HCM'),
(NULL, N'Đinh Quốc Nam',    'nam13@gmail.com',   '0911000013', N'Bình Chánh, TP.HCM'),
(NULL, N'Nguyễn Thị Oanh',  'oanh14@gmail.com',  '0911000014', N'Quận 4, TP.HCM'),
(NULL, N'Trương Minh Phúc', 'phuc15@gmail.com',  '0911000015', N'Quận 6, TP.HCM'),
(NULL, N'Lý Thị Quỳnh',     'quynh16@gmail.com', '0911000016', N'Quận 8, TP.HCM'),
(NULL, N'Đào Văn Sơn',      'son17@gmail.com',   '0911000017', N'Bình Tân, TP.HCM'),
(NULL, N'Phạm Minh Tâm',    'tam18@gmail.com',   '0911000018', N'Quận 11, TP.HCM'),
(NULL, N'Hoàng Thị Uyên',   'uyen19@gmail.com',  '0911000019', N'Củ Chi, TP.HCM'),
(NULL, N'Đặng Văn Việt',    'viet20@gmail.com',  '0911000020', N'Hóc Môn, TP.HCM');
GO

-- 7. ĐƠN HÀNG (20 ĐƠN HÀNG)
INSERT INTO DonHang
(MaCodeDonHang, MaKhachHang, NguoiTao, TenNguoiNhan, SoDienThoaiNhan, DiaChiGiaoHang,
 TrangThaiDonHang, TamTinh, GiamGia, PhiVanChuyen, TongTien, GhiChu)
SELECT
    'DH' + RIGHT('000' + CAST(l.MaLaptop AS VARCHAR(3)), 3),
    l.MaLaptop,
    2,
    c.TenKhachHang,
    c.SoDienThoai,
    c.DiaChi,
    CASE
        WHEN l.MaLaptop <= 5 THEN N'COMPLETED'
        WHEN l.MaLaptop <= 10 THEN N'SHIPPING'
        WHEN l.MaLaptop <= 15 THEN N'CONFIRMED'
        ELSE N'PENDING'
    END,
    l.GiaBan,
    0,
    30000,
    l.GiaBan + 30000,
    N'Đơn hàng dữ liệu mẫu'
FROM Laptop l
JOIN KhachHang c ON c.MaKhachHang = l.MaLaptop
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

-- 8. CHI TIẾT ĐƠN HÀNG (20 BẢN GHI)
INSERT INTO ChiTietDonHang
(MaDonHang, MaLaptop, MaSKULuuTru, TenLaptopLuuTru, DonGia, SoLuong, ThanhTien)
SELECT
    d.MaDonHang,
    l.MaLaptop,
    l.MaSKU,
    l.TenLaptop,
    l.GiaBan,
    1,
    l.GiaBan
FROM DonHang d
JOIN Laptop l
    ON d.MaCodeDonHang = 'DH' + RIGHT('000' + CAST(l.MaLaptop AS VARCHAR(3)), 3)
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

-- 9. THANH TOÁN (20 BẢN GHI)
INSERT INTO ThanhToan
(MaDonHang, PhuongThucThanhToan, TrangThaiThanhToan, SoTien, MaGiaoDich, ThoiGianThanhToan)
SELECT
    MaDonHang,
    CASE WHEN MaDonHang % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CASE WHEN TrangThaiDonHang = N'COMPLETED' THEN N'PAID' ELSE N'UNPAID' END,
    TongTien,
    'GD' + RIGHT('000' + CAST(MaDonHang AS VARCHAR(3)), 3),
    CASE WHEN TrangThaiDonHang = N'COMPLETED' THEN SYSDATETIME() ELSE NULL END
FROM DonHang;
GO

-- 10. LỊCH SỬ TRẠNG THÁI ĐƠN HÀNG (20 BẢN GHI)
INSERT INTO LichSuTrangThaiDonHang
(MaDonHang, TrangThaiCu, TrangThaiMoi, NguoiThayDoi, GhiChuThayDoi)
SELECT
    MaDonHang,
    NULL,
    TrangThaiDonHang,
    2,
    N'Khởi tạo trạng thái đơn hàng mẫu'
FROM DonHang;
GO

-- KIỂM TRA SỐ DÒNG TOÀN BỘ 10 BẢNG
SELECT 'VaiTro' AS TenBang, COUNT(*) AS SoDong FROM VaiTro
UNION ALL SELECT 'TaiKhoan', COUNT(*) FROM TaiKhoan
UNION ALL SELECT 'DanhMucLaptop', COUNT(*) FROM DanhMucLaptop
UNION ALL SELECT 'ThuongHieu', COUNT(*) FROM ThuongHieu
UNION ALL SELECT 'Laptop', COUNT(*) FROM Laptop
UNION ALL SELECT 'KhachHang', COUNT(*) FROM KhachHang
UNION ALL SELECT 'DonHang', COUNT(*) FROM DonHang
UNION ALL SELECT 'ChiTietDonHang', COUNT(*) FROM ChiTietDonHang
UNION ALL SELECT 'ThanhToan', COUNT(*) FROM ThanhToan
UNION ALL SELECT 'LichSuTrangThaiDonHang', COUNT(*) FROM LichSuTrangThaiDonHang;
GO
