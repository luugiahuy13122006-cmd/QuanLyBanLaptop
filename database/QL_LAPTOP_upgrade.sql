-- =====================================================================
-- SCRIPT NÂNG CẤP CƠ SỞ DỮ LIỆU QL_Laptop (IDEMPOTENT - CHẠY LẶP KHÔNG LỖI)
-- Dành cho CSDL đã có sẵn dữ liệu trước khi nâng cấp lên phiên bản NEXORA v2
-- =====================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE QL_Laptop;
GO

-- 1. NÂNG CẤP BẢNG TaiKhoan
IF COL_LENGTH('TaiKhoan', 'SoLanDangNhapSai') IS NULL
    ALTER TABLE TaiKhoan ADD SoLanDangNhapSai INT NOT NULL CONSTRAINT DF_TaiKhoan_Failed DEFAULT 0;
IF COL_LENGTH('TaiKhoan', 'KhoaDenLuc') IS NULL
    ALTER TABLE TaiKhoan ADD KhoaDenLuc DATETIME2 NULL;
IF COL_LENGTH('TaiKhoan', 'BatBuocDoiMatKhau') IS NULL
    ALTER TABLE TaiKhoan ADD BatBuocDoiMatKhau BIT NOT NULL CONSTRAINT DF_TaiKhoan_MustChange DEFAULT 0;
IF COL_LENGTH('TaiKhoan', 'PhienBan') IS NULL
    ALTER TABLE TaiKhoan ADD PhienBan INT NOT NULL CONSTRAINT DF_TaiKhoan_Version DEFAULT 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_TaiKhoan_Email' AND object_id = OBJECT_ID('TaiKhoan'))
BEGIN
    -- Nếu trước đây có UNIQUE constraint mặc định trên Email gây lỗi null trùng, gỡ bỏ để thay bằng filtered unique index
    DECLARE @uqName NVARCHAR(128);
    SELECT TOP 1 @uqName = kc.name
    FROM sys.key_constraints kc
    JOIN sys.index_columns ic ON kc.parent_object_id = ic.object_id AND kc.unique_index_id = ic.index_id
    JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
    WHERE kc.parent_object_id = OBJECT_ID('TaiKhoan') AND kc.type = 'UQ' AND c.name = 'Email';
    IF @uqName IS NOT NULL
        EXEC('ALTER TABLE TaiKhoan DROP CONSTRAINT [' + @uqName + ']');

    CREATE UNIQUE NONCLUSTERED INDEX UX_TaiKhoan_Email ON TaiKhoan(Email) WHERE Email IS NOT NULL;
END
GO

-- 2. NÂNG CẤP BẢNG Laptop
IF COL_LENGTH('Laptop', 'TenKhongDau') IS NULL
    ALTER TABLE Laptop ADD TenKhongDau NVARCHAR(250) NULL;
IF COL_LENGTH('Laptop', 'BaoHanhThang') IS NULL
    ALTER TABLE Laptop ADD BaoHanhThang INT NOT NULL CONSTRAINT DF_Laptop_Warranty DEFAULT 12;
IF COL_LENGTH('Laptop', 'LuotXem') IS NULL
    ALTER TABLE Laptop ADD LuotXem INT NOT NULL CONSTRAINT DF_Laptop_ViewCount DEFAULT 0;
IF COL_LENGTH('Laptop', 'PhienBan') IS NULL
    ALTER TABLE Laptop ADD PhienBan INT NOT NULL CONSTRAINT DF_Laptop_Version DEFAULT 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Laptop_TenKhongDau' AND object_id = OBJECT_ID('Laptop'))
    CREATE INDEX IX_Laptop_TenKhongDau ON Laptop(TenKhongDau);
GO

-- 3. NÂNG CẤP BẢNG KhachHang
IF COL_LENGTH('KhachHang', 'TenKhongDau') IS NULL
    ALTER TABLE KhachHang ADD TenKhongDau NVARCHAR(150) NULL;
IF COL_LENGTH('KhachHang', 'PhienBan') IS NULL
    ALTER TABLE KhachHang ADD PhienBan INT NOT NULL CONSTRAINT DF_KhachHang_Version DEFAULT 0;
GO

-- 4. NÂNG CẤP BẢNG DonHang & LichSuTrangThaiDonHang (thêm PREPARING, ship, VAT, coupon, tracking, PhienBan)
IF COL_LENGTH('DonHang', 'PhiVanChuyen') IS NULL
    ALTER TABLE DonHang ADD PhiVanChuyen DECIMAL(18,2) NOT NULL CONSTRAINT DF_DonHang_Ship DEFAULT 0;
IF COL_LENGTH('DonHang', 'ThueVAT') IS NULL
    ALTER TABLE DonHang ADD ThueVAT DECIMAL(18,2) NOT NULL CONSTRAINT DF_DonHang_VAT DEFAULT 0;
IF COL_LENGTH('DonHang', 'MaGiamGia') IS NULL
    ALTER TABLE DonHang ADD MaGiamGia VARCHAR(30) NULL;
IF COL_LENGTH('DonHang', 'DonViVanChuyen') IS NULL
    ALTER TABLE DonHang ADD DonViVanChuyen NVARCHAR(100) NULL;
IF COL_LENGTH('DonHang', 'MaVanDon') IS NULL
    ALTER TABLE DonHang ADD MaVanDon NVARCHAR(60) NULL;
IF COL_LENGTH('DonHang', 'PhienBan') IS NULL
    ALTER TABLE DonHang ADD PhienBan INT NOT NULL CONSTRAINT DF_DonHang_Version DEFAULT 0;
GO

IF OBJECT_ID('CK_DonHang_TrangThai', 'C') IS NOT NULL
BEGIN
    ALTER TABLE DonHang DROP CONSTRAINT CK_DonHang_TrangThai;
    ALTER TABLE DonHang ADD CONSTRAINT CK_DonHang_TrangThai CHECK (TrangThaiDonHang IN (
        N'PENDING', N'CONFIRMED', N'PREPARING', N'SHIPPING',
        N'COMPLETED', N'CANCELLED', N'RETURNED', N'REFUND_REQUESTED', N'REFUNDED'
    ));
END
GO

IF OBJECT_ID('CK_LichSu_TrangThaiMoi', 'C') IS NOT NULL
BEGIN
    ALTER TABLE LichSuTrangThaiDonHang DROP CONSTRAINT CK_LichSu_TrangThaiMoi;
    ALTER TABLE LichSuTrangThaiDonHang ADD CONSTRAINT CK_LichSu_TrangThaiMoi CHECK (TrangThaiMoi IN (
        N'PENDING', N'CONFIRMED', N'PREPARING', N'SHIPPING',
        N'COMPLETED', N'CANCELLED', N'RETURNED', N'REFUND_REQUESTED', N'REFUNDED'
    ));
END
GO

-- 5. NÂNG CẤP ChiTietDonHang
IF COL_LENGTH('ChiTietDonHang', 'GiaVonLuuTru') IS NULL
BEGIN
    ALTER TABLE ChiTietDonHang ADD GiaVonLuuTru DECIMAL(18,2) NOT NULL CONSTRAINT DF_CTDH_Cost DEFAULT 0;
    EXEC('UPDATE c SET GiaVonLuuTru = ISNULL(l.GiaNhap, 0) FROM ChiTietDonHang c JOIN Laptop l ON l.MaLaptop = c.MaLaptop WHERE c.GiaVonLuuTru = 0');
END
GO

-- 6. NÂNG CẤP ThanhToan
IF COL_LENGTH('ThanhToan', 'MaThamChieuCong') IS NULL
    ALTER TABLE ThanhToan ADD MaThamChieuCong VARCHAR(120) NULL;
IF COL_LENGTH('ThanhToan', 'PhienBan') IS NULL
    ALTER TABLE ThanhToan ADD PhienBan INT NOT NULL CONSTRAINT DF_ThanhToan_Version DEFAULT 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_ThanhToan_GatewayRef' AND object_id = OBJECT_ID('ThanhToan'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_ThanhToan_GatewayRef ON ThanhToan(MaThamChieuCong) WHERE MaThamChieuCong IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ThanhToan_NguoiXacNhan')
    ALTER TABLE ThanhToan ADD CONSTRAINT FK_ThanhToan_NguoiXacNhan FOREIGN KEY (NguoiXacNhan) REFERENCES TaiKhoan(MaTaiKhoan);
GO

-- 7. TẠO CÁC BẢNG MỚI (IF OBJECT_ID(...) IS NULL)
IF OBJECT_ID('NhaCungCap', 'U') IS NULL
CREATE TABLE NhaCungCap (
    MaNhaCungCap INT IDENTITY(1,1) PRIMARY KEY,
    TenNhaCungCap NVARCHAR(150) NOT NULL UNIQUE,
    MaSoThue VARCHAR(30),
    NguoiLienHe NVARCHAR(100),
    SoDienThoai VARCHAR(20),
    Email VARCHAR(100),
    DiaChi NVARCHAR(300),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF COL_LENGTH('PhieuNhap', 'MaNhaCungCap') IS NULL
    ALTER TABLE PhieuNhap ADD MaNhaCungCap INT NULL;
IF COL_LENGTH('PhieuNhap', 'DaThanhToan') IS NULL
    ALTER TABLE PhieuNhap ADD DaThanhToan DECIMAL(18,2) NOT NULL CONSTRAINT DF_PN_Paid DEFAULT 0;
IF COL_LENGTH('PhieuNhap', 'TrangThaiCongNo') IS NULL
    ALTER TABLE PhieuNhap ADD TrangThaiCongNo NVARCHAR(20) NOT NULL CONSTRAINT DF_PN_PayStatus DEFAULT N'UNPAID';
IF COL_LENGTH('PhieuNhap', 'PhienBan') IS NULL
    ALTER TABLE PhieuNhap ADD PhienBan INT NOT NULL CONSTRAINT DF_PN_Version DEFAULT 0;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_PhieuNhap_NCC')
    ALTER TABLE PhieuNhap ADD CONSTRAINT FK_PhieuNhap_NCC FOREIGN KEY (MaNhaCungCap) REFERENCES NhaCungCap(MaNhaCungCap);
GO

IF OBJECT_ID('BienDongKho', 'U') IS NULL
CREATE TABLE BienDongKho (
    MaBienDong INT IDENTITY(1,1) PRIMARY KEY,
    MaLaptop INT NOT NULL,
    LoaiBienDong NVARCHAR(30) NOT NULL,
    SoLuongThayDoi INT NOT NULL,
    TonTruoc INT NOT NULL,
    TonSau INT NOT NULL,
    MaThamChieu NVARCHAR(60),
    LyDo NVARCHAR(500),
    NguoiThucHien INT,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_BienDongKho_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop)
);
GO

IF OBJECT_ID('NhatKyHeThong', 'U') IS NULL
CREATE TABLE NhatKyHeThong (
    MaNhatKy BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT,
    TenDangNhap NVARCHAR(50),
    HanhDong NVARCHAR(60) NOT NULL,
    DoiTuong NVARCHAR(60) NOT NULL,
    MaDoiTuong NVARCHAR(60),
    MoTa NVARCHAR(1000),
    DiaChiIP NVARCHAR(64),
    ThoiGian DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('KhoaTacVu', 'U') IS NULL
CREATE TABLE KhoaTacVu (
    TenTacVu NVARCHAR(80) PRIMARY KEY,
    MayChuGiuKhoa NVARCHAR(120),
    KhoaDenLuc DATETIME2,
    LanChayCuoi DATETIME2
);
GO

IF OBJECT_ID('TokenDatLaiMatKhau', 'U') IS NULL
CREATE TABLE TokenDatLaiMatKhau (
    MaToken BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NOT NULL,
    MaBamToken NVARCHAR(128) NOT NULL UNIQUE,
    HetHanLuc DATETIME2 NOT NULL,
    DaSuDung BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_TokenReset_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan)
);
GO

IF OBJECT_ID('GioHangChiTiet', 'U') IS NULL
CREATE TABLE GioHangChiTiet (
    MaChiTietGio INT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NOT NULL,
    MaLaptop INT NOT NULL,
    SoLuong INT NOT NULL DEFAULT 1,
    NgayCapNhat DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_GioHang_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_GioHang_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT UX_GioHang_TaiKhoan_Laptop UNIQUE (MaTaiKhoan, MaLaptop)
);
GO

IF OBJECT_ID('DiaChiKhachHang', 'U') IS NULL
CREATE TABLE DiaChiKhachHang (
    MaDiaChi INT IDENTITY(1,1) PRIMARY KEY,
    MaKhachHang INT NOT NULL,
    TenNguoiNhan NVARCHAR(100) NOT NULL,
    SoDienThoai VARCHAR(15) NOT NULL,
    TinhThanh NVARCHAR(100),
    QuanHuyen NVARCHAR(100),
    PhuongXa NVARCHAR(100),
    DiaChiChiTiet NVARCHAR(300) NOT NULL,
    MacDinh BIT NOT NULL DEFAULT 0,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_DiaChi_KhachHang FOREIGN KEY (MaKhachHang) REFERENCES KhachHang(MaKhachHang)
);
GO

IF OBJECT_ID('MaGiamGia', 'U') IS NULL
CREATE TABLE MaGiamGia (
    MaGiamGiaId INT IDENTITY(1,1) PRIMARY KEY,
    MaCode NVARCHAR(30) NOT NULL UNIQUE,
    MoTa NVARCHAR(300),
    LoaiGiam NVARCHAR(20) NOT NULL DEFAULT N'PERCENT',
    GiaTriGiam DECIMAL(18,2) NOT NULL,
    GiamToiDa DECIMAL(18,2),
    DonToiThieu DECIMAL(18,2) NOT NULL DEFAULT 0,
    SoLuotToiDa INT NOT NULL DEFAULT 100,
    DaSuDung INT NOT NULL DEFAULT 0,
    BatDauLuc DATETIME2,
    KetThucLuc DATETIME2,
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    PhienBan INT NOT NULL DEFAULT 0
);
GO

IF OBJECT_ID('SoSerialLaptop', 'U') IS NULL
CREATE TABLE SoSerialLaptop (
    MaSerial INT IDENTITY(1,1) PRIMARY KEY,
    MaLaptop INT NOT NULL,
    SoSerial NVARCHAR(60) NOT NULL UNIQUE,
    MaChiTietDonHang INT NULL,
    TrangThai NVARCHAR(25) NOT NULL DEFAULT N'IN_STOCK',
    BaoHanhDen DATE NULL,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Serial_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT FK_Serial_CTDH FOREIGN KEY (MaChiTietDonHang) REFERENCES ChiTietDonHang(MaChiTietDonHang)
);
GO

IF OBJECT_ID('YeuCauBaoHanh', 'U') IS NULL
CREATE TABLE YeuCauBaoHanh (
    MaYeuCau INT IDENTITY(1,1) PRIMARY KEY,
    SoSerial NVARCHAR(60) NOT NULL,
    TenKhachHang NVARCHAR(100) NOT NULL,
    SoDienThoai VARCHAR(15) NOT NULL,
    MoTaLoi NVARCHAR(1000) NOT NULL,
    TrangThai NVARCHAR(20) NOT NULL DEFAULT N'RECEIVED',
    GhiChuKyThuat NVARCHAR(1000),
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NgayCapNhat DATETIME2
);
GO

IF OBJECT_ID('DanhGiaSanPham', 'U') IS NULL
CREATE TABLE DanhGiaSanPham (
    MaDanhGia INT IDENTITY(1,1) PRIMARY KEY,
    MaLaptop INT NOT NULL,
    MaKhachHang INT NOT NULL,
    SoSao INT NOT NULL,
    TieuDe NVARCHAR(150),
    NoiDung NVARCHAR(1000) NOT NULL,
    DaMuaHang BIT NOT NULL DEFAULT 1,
    DaDuyet BIT NOT NULL DEFAULT 0,
    PhanHoiShop NVARCHAR(500),
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_DanhGia_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT FK_DanhGia_KhachHang FOREIGN KEY (MaKhachHang) REFERENCES KhachHang(MaKhachHang)
);
GO

IF OBJECT_ID('YeuThich', 'U') IS NULL
CREATE TABLE YeuThich (
    MaYeuThich INT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NOT NULL,
    MaLaptop INT NOT NULL,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_YeuThich_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_YeuThich_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT UX_YeuThich UNIQUE (MaTaiKhoan, MaLaptop)
);
GO

IF OBJECT_ID('BaiVietCongNghe', 'U') IS NULL
CREATE TABLE BaiVietCongNghe (
    MaBaiViet INT IDENTITY(1,1) PRIMARY KEY,
    TieuDe NVARCHAR(200) NOT NULL,
    DuongDan NVARCHAR(150) NOT NULL UNIQUE,
    TomTat NVARCHAR(500),
    NoiDung NVARCHAR(4000) NOT NULL,
    AnhBia NVARCHAR(300),
    TacGia NVARCHAR(100),
    HienThi BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NgayCapNhat DATETIME2
);
GO

IF OBJECT_ID('CauHoiThuongGap', 'U') IS NULL
CREATE TABLE CauHoiThuongGap (
    MaCauHoi INT IDENTITY(1,1) PRIMARY KEY,
    ChuDe NVARCHAR(80) NOT NULL,
    CauHoi NVARCHAR(300) NOT NULL,
    CauTraLoi NVARCHAR(2000) NOT NULL,
    ThuTu INT NOT NULL DEFAULT 0,
    HienThi BIT NOT NULL DEFAULT 1
);
GO

IF OBJECT_ID('BannerQuangCao', 'U') IS NULL
CREATE TABLE BannerQuangCao (
    MaBanner INT IDENTITY(1,1) PRIMARY KEY,
    TieuDe NVARCHAR(150) NOT NULL,
    TieuDePhu NVARCHAR(300),
    DuongDanAnh NVARCHAR(300),
    LienKet NVARCHAR(300),
    NhanNut NVARCHAR(60),
    ThuTu INT NOT NULL DEFAULT 0,
    HienThi BIT NOT NULL DEFAULT 1
);
GO

IF OBJECT_ID('TinNhanHoTro', 'U') IS NULL
CREATE TABLE TinNhanHoTro (
    MaTinNhan BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhienChat NVARCHAR(80) NOT NULL,
    TenNguoiGui NVARCHAR(100) NOT NULL,
    VaiTroNguoiGui NVARCHAR(20) NOT NULL,
    NoiDung NVARCHAR(1000) NOT NULL,
    DaDoc BIT NOT NULL DEFAULT 0,
    ThoiGian DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- Bổ sung quyền nhân viên & ẩn danh đánh giá & SĐT null cho đăng ký tối giản
IF COL_LENGTH('KhachHang', 'SoDienThoai') IS NOT NULL
    ALTER TABLE KhachHang ALTER COLUMN SoDienThoai VARCHAR(15) NULL;
GO

IF COL_LENGTH('DanhGiaSanPham', 'AnDanh') IS NULL
    ALTER TABLE DanhGiaSanPham ADD AnDanh BIT NOT NULL CONSTRAINT DF_DanhGia_AnDanh DEFAULT 0;
GO

IF OBJECT_ID('PhanQuyenNhanVien', 'U') IS NULL
CREATE TABLE PhanQuyenNhanVien (
    MaPhanQuyen INT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NOT NULL,
    MaChucNang NVARCHAR(40) NOT NULL,
    QuyenHan NVARCHAR(20) NOT NULL DEFAULT N'NONE',
    CONSTRAINT FK_PhanQuyen_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT UX_PhanQuyen_TK_CN UNIQUE (MaTaiKhoan, MaChucNang)
);
GO
