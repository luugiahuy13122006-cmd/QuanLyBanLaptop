-- =====================================================================
-- CƠ SỞ DỮ LIỆU QL_Laptop — NEXORA LAPTOP (Production + Đồ Án 2026)
-- =====================================================================
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

-- Xóa bảng cũ theo thứ tự ngược khóa ngoại
DROP TABLE IF EXISTS TinNhanHoTro;
DROP TABLE IF EXISTS BannerQuangCao;
DROP TABLE IF EXISTS CauHoiThuongGap;
DROP TABLE IF EXISTS BaiVietCongNghe;
DROP TABLE IF EXISTS YeuThich;
DROP TABLE IF EXISTS DanhGiaSanPham;
DROP TABLE IF EXISTS YeuCauBaoHanh;
DROP TABLE IF EXISTS SoSerialLaptop;
DROP TABLE IF EXISTS DiaChiKhachHang;
DROP TABLE IF EXISTS GioHangChiTiet;
DROP TABLE IF EXISTS MaGiamGia;
DROP TABLE IF EXISTS TokenDatLaiMatKhau;
DROP TABLE IF EXISTS KhoaTacVu;
DROP TABLE IF EXISTS NhatKyHeThong;
DROP TABLE IF EXISTS BienDongKho;
DROP TABLE IF EXISTS ChiTietPhieuNhap;
DROP TABLE IF EXISTS PhieuNhap;
DROP TABLE IF EXISTS NhaCungCap;
DROP TABLE IF EXISTS BoDemMa;
DROP TABLE IF EXISTS LichSuTrangThaiDonHang;
DROP TABLE IF EXISTS ThanhToan;
DROP TABLE IF EXISTS ChiTietDonHang;
DROP TABLE IF EXISTS DonHang;
DROP TABLE IF EXISTS KhachHang;
DROP TABLE IF EXISTS Laptop;
DROP TABLE IF EXISTS ThuongHieu;
DROP TABLE IF EXISTS DanhMucLaptop;
DROP TABLE IF EXISTS TaiKhoan;
DROP TABLE IF EXISTS VaiTro;
GO

-- 1. VAI TRÒ
CREATE TABLE VaiTro (
    MaVaiTro INT IDENTITY(1,1) PRIMARY KEY,
    TenVaiTro NVARCHAR(50) NOT NULL UNIQUE,
    MoTa NVARCHAR(255)
);
GO

-- 2. TÀI KHOẢN
CREATE TABLE TaiKhoan (
    MaTaiKhoan INT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap VARCHAR(50) NOT NULL UNIQUE,
    MatKhauMaHoa VARCHAR(255) NOT NULL,
    HoTen NVARCHAR(100) NOT NULL,
    Email VARCHAR(100),
    SoDienThoai VARCHAR(15),
    MaVaiTro INT NOT NULL,
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    SoLanDangNhapSai INT NOT NULL DEFAULT 0,
    KhoaDenLuc DATETIME2 NULL,
    BatBuocDoiMatKhau BIT NOT NULL DEFAULT 0,
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_TaiKhoan_VaiTro FOREIGN KEY (MaVaiTro) REFERENCES VaiTro(MaVaiTro)
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_TaiKhoan_Email
ON TaiKhoan(Email)
WHERE Email IS NOT NULL;
GO

-- 3. DANH MỤC LAPTOP
CREATE TABLE DanhMucLaptop (
    MaDanhMuc INT IDENTITY(1,1) PRIMARY KEY,
    TenDanhMuc NVARCHAR(100) NOT NULL UNIQUE,
    MoTa NVARCHAR(500),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- 4. THƯƠNG HIỆU
CREATE TABLE ThuongHieu (
    MaThuongHieu INT IDENTITY(1,1) PRIMARY KEY,
    TenThuongHieu NVARCHAR(100) NOT NULL UNIQUE,
    QuocGia NVARCHAR(100),
    MoTa NVARCHAR(500),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1
);
GO

-- 5. LAPTOP
CREATE TABLE Laptop (
    MaLaptop INT IDENTITY(1,1) PRIMARY KEY,
    MaSKU VARCHAR(30) NOT NULL UNIQUE,
    TenLaptop NVARCHAR(200) NOT NULL,
    TenKhongDau NVARCHAR(250),
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
    MoTa NVARCHAR(4000),
    TrangThaiHoatDong BIT NOT NULL DEFAULT 1,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NgayCapNhat DATETIME2,
    BaoHanhThang INT NOT NULL DEFAULT 12,
    LuotXem INT NOT NULL DEFAULT 0,
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_Laptop_DanhMucLaptop FOREIGN KEY (MaDanhMuc) REFERENCES DanhMucLaptop(MaDanhMuc),
    CONSTRAINT FK_Laptop_ThuongHieu FOREIGN KEY (MaThuongHieu) REFERENCES ThuongHieu(MaThuongHieu),
    CONSTRAINT CK_Laptop_Gia CHECK (GiaNhap >= 0 AND GiaNiemYet >= 0 AND GiaBan >= 0),
    CONSTRAINT CK_Laptop_SoLuongTon CHECK (SoLuongTon >= 0),
    CONSTRAINT CK_Laptop_RAM CHECK (RAM_GB IS NULL OR RAM_GB IN (4, 8, 12, 16, 18, 24, 32, 36, 48, 64, 128)),
    CONSTRAINT CK_Laptop_DungLuongSSD CHECK (DungLuongSSD_GB IS NULL OR DungLuongSSD_GB IN (128, 256, 512, 1024, 2048, 4096)),
    CONSTRAINT CK_Laptop_ManHinh CHECK (KichThuocManHinh IS NULL OR KichThuocManHinh IN (13.0, 13.3, 13.4, 13.6, 14.0, 14.2, 14.5, 15.3, 15.6, 16.0, 16.1, 17.0, 17.3, 18.0))
);
GO

-- 6. KHÁCH HÀNG
CREATE TABLE KhachHang (
    MaKhachHang INT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan INT NULL,
    TenKhachHang NVARCHAR(100) NOT NULL,
    TenKhongDau NVARCHAR(150),
    Email VARCHAR(100),
    SoDienThoai VARCHAR(15) NOT NULL,
    DiaChi NVARCHAR(300),
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_KhachHang_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan)
);
GO

-- 7. ĐƠN HÀNG
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
    ThueVAT DECIMAL(18,2) NOT NULL DEFAULT 0,
    MaGiamGia VARCHAR(30),
    TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    DonViVanChuyen NVARCHAR(100),
    MaVanDon NVARCHAR(60),
    GhiChu NVARCHAR(500),
    NgayCapNhat DATETIME2,
    HanThanhToan DATETIME2,
    LyDoHuy NVARCHAR(500),
    LyDoHoanTien NVARCHAR(500),
    NganHangHoan NVARCHAR(100),
    SoTaiKhoanHoan VARCHAR(30),
    TenChuTaiKhoanHoan NVARCHAR(100),
    NgayYeuCauHoan DATETIME2,
    NgayHoanTien DATETIME2,
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_DonHang_KhachHang FOREIGN KEY (MaKhachHang) REFERENCES KhachHang(MaKhachHang),
    CONSTRAINT FK_DonHang_TaiKhoan FOREIGN KEY (NguoiTao) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_DonHang_TrangThai CHECK (TrangThaiDonHang IN (
        N'PENDING', N'CONFIRMED', N'PREPARING', N'SHIPPING',
        N'COMPLETED', N'CANCELLED', N'RETURNED', N'REFUND_REQUESTED', N'REFUNDED'
    )),
    CONSTRAINT CK_DonHang_Tien CHECK (TamTinh >= 0 AND GiamGia >= 0 AND PhiVanChuyen >= 0 AND ThueVAT >= 0 AND TongTien >= 0)
);
GO

-- 8. CHI TIẾT ĐƠN HÀNG
CREATE TABLE ChiTietDonHang (
    MaChiTietDonHang INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,
    MaLaptop INT,
    MaSKULuuTru VARCHAR(30) NOT NULL,
    TenLaptopLuuTru NVARCHAR(200) NOT NULL,
    DonGia DECIMAL(18,2) NOT NULL,
    GiaVonLuuTru DECIMAL(18,2) NOT NULL DEFAULT 0,
    SoLuong INT NOT NULL,
    ThanhTien DECIMAL(18,2) NOT NULL,

    CONSTRAINT FK_ChiTietDonHang_DonHang FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),
    CONSTRAINT FK_ChiTietDonHang_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT CK_ChiTietDonHang_DonGia CHECK (DonGia >= 0),
    CONSTRAINT CK_ChiTietDonHang_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietDonHang_ThanhTien CHECK (ThanhTien >= 0)
);
GO

-- 9. THANH TOÁN
CREATE TABLE ThanhToan (
    MaThanhToan INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,
    PhuongThucThanhToan NVARCHAR(30) NOT NULL,
    TrangThaiThanhToan NVARCHAR(20) NOT NULL DEFAULT N'UNPAID',
    SoTien DECIMAL(18,2) NOT NULL,
    MaGiaoDich VARCHAR(100),
    ThoiGianThanhToan DATETIME2,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    AnhMinhChung VARCHAR(100),
    NgayTaiMinhChung DATETIME2,
    LyDoTuChoi NVARCHAR(500),
    NguoiXacNhan INT,
    MaThamChieuCong VARCHAR(120),
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_ThanhToan_DonHang FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),
    CONSTRAINT FK_ThanhToan_NguoiXacNhan FOREIGN KEY (NguoiXacNhan) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_ThanhToan_PhuongThuc CHECK (PhuongThucThanhToan IN (N'CASH', N'BANK_TRANSFER', N'CARD', N'E_WALLET')),
    CONSTRAINT CK_ThanhToan_TrangThai CHECK (TrangThaiThanhToan IN (N'UNPAID', N'PENDING', N'PENDING_CONFIRMATION', N'PAID', N'FAILED', N'REFUNDED')),
    CONSTRAINT CK_ThanhToan_SoTien CHECK (SoTien >= 0)
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UX_ThanhToan_GatewayRef
ON ThanhToan(MaThamChieuCong)
WHERE MaThamChieuCong IS NOT NULL;
GO

-- 10. LỊCH SỬ TRẠNG THÁI ĐƠN HÀNG
CREATE TABLE LichSuTrangThaiDonHang (
    MaLichSu INT IDENTITY(1,1) PRIMARY KEY,
    MaDonHang INT NOT NULL,
    TrangThaiCu NVARCHAR(20),
    TrangThaiMoi NVARCHAR(20) NOT NULL,
    NguoiThayDoi INT,
    GhiChuThayDoi NVARCHAR(500),
    ThoiGianThayDoi DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_LichSu_DonHang FOREIGN KEY (MaDonHang) REFERENCES DonHang(MaDonHang),
    CONSTRAINT FK_LichSu_TaiKhoan FOREIGN KEY (NguoiThayDoi) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_LichSu_TrangThaiMoi CHECK (TrangThaiMoi IN (
        N'PENDING', N'CONFIRMED', N'PREPARING', N'SHIPPING',
        N'COMPLETED', N'CANCELLED', N'RETURNED', N'REFUND_REQUESTED', N'REFUNDED'
    ))
);
GO

-- 11. BỘ ĐẾM MÃ
CREATE TABLE BoDemMa (
    TenBoDem VARCHAR(30) PRIMARY KEY,
    GiaTriHienTai INT NOT NULL DEFAULT 0
);
GO

-- 12. NHÀ CUNG CẤP
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

-- 13. PHIẾU NHẬP KHO
CREATE TABLE PhieuNhap (
    MaPhieuNhap INT IDENTITY(1,1) PRIMARY KEY,
    MaCodePhieuNhap VARCHAR(30) NOT NULL UNIQUE,
    NhaCungCap NVARCHAR(150) NOT NULL,
    MaNhaCungCap INT NULL,
    GhiChu NVARCHAR(500),
    TrangThai NVARCHAR(20) NOT NULL DEFAULT N'PENDING',
    TongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    DaThanhToan DECIMAL(18,2) NOT NULL DEFAULT 0,
    TrangThaiCongNo NVARCHAR(20) NOT NULL DEFAULT N'UNPAID',
    NguoiTao INT NOT NULL,
    NgayTao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    NguoiDuyet INT,
    NgayDuyet DATETIME2,
    LyDoTuChoi NVARCHAR(500),
    PhienBan INT NOT NULL DEFAULT 0,

    CONSTRAINT FK_PhieuNhap_NCC FOREIGN KEY (MaNhaCungCap) REFERENCES NhaCungCap(MaNhaCungCap),
    CONSTRAINT FK_PhieuNhap_NguoiTao FOREIGN KEY (NguoiTao) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_PhieuNhap_NguoiDuyet FOREIGN KEY (NguoiDuyet) REFERENCES TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_PhieuNhap_TrangThai CHECK (TrangThai IN (N'PENDING', N'APPROVED', N'REJECTED')),
    CONSTRAINT CK_PhieuNhap_TongTien CHECK (TongTien >= 0 AND DaThanhToan >= 0)
);
GO

-- 14. CHI TIẾT PHIẾU NHẬP
CREATE TABLE ChiTietPhieuNhap (
    MaChiTietPhieuNhap INT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuNhap INT NOT NULL,
    MaLaptop INT NOT NULL,
    SoLuong INT NOT NULL,
    DonGiaNhap DECIMAL(18,2) NOT NULL,
    ThanhTien DECIMAL(18,2) NOT NULL,

    CONSTRAINT FK_ChiTietPhieuNhap_PhieuNhap FOREIGN KEY (MaPhieuNhap) REFERENCES PhieuNhap(MaPhieuNhap),
    CONSTRAINT FK_ChiTietPhieuNhap_Laptop FOREIGN KEY (MaLaptop) REFERENCES Laptop(MaLaptop),
    CONSTRAINT CK_ChiTietPhieuNhap_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_ChiTietPhieuNhap_Tien CHECK (DonGiaNhap >= 0 AND ThanhTien >= 0)
);
GO

-- 15. BIẾN ĐỘNG KHO
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

-- 16. NHẬT KÝ HỆ THỐNG
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

-- 17. KHÓA TÁC VỤ ĐỊNH KỲ
CREATE TABLE KhoaTacVu (
    TenTacVu NVARCHAR(80) PRIMARY KEY,
    MayChuGiuKhoa NVARCHAR(120),
    KhoaDenLuc DATETIME2,
    LanChayCuoi DATETIME2
);
GO

-- 18. TOKEN ĐẶT LẠI MẬT KHẨU
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

-- 19. GIỎ HÀNG CHI TIẾT
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

-- 20. ĐỊA CHỈ KHÁCH HÀNG
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

-- 21. MÃ GIẢM GIÁ
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

-- 22. SỐ SERIAL LAPTOP
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

-- 23. YÊU CẦU BẢO HÀNH
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

-- 24. ĐÁNH GIÁ SẢN PHẨM
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

-- 25. YÊU THÍCH
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

-- 26. BÀI VIẾT CÔNG NGHỆ
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

-- 27. CÂU HỎI THƯỜNG GẶP (FAQ)
CREATE TABLE CauHoiThuongGap (
    MaCauHoi INT IDENTITY(1,1) PRIMARY KEY,
    ChuDe NVARCHAR(80) NOT NULL,
    CauHoi NVARCHAR(300) NOT NULL,
    CauTraLoi NVARCHAR(2000) NOT NULL,
    ThuTu INT NOT NULL DEFAULT 0,
    HienThi BIT NOT NULL DEFAULT 1
);
GO

-- 28. BANNER QUẢNG CÁO
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

-- 29. TIN NHẮN HỖ TRỢ
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

-- INDEXES
CREATE INDEX IX_Laptop_TenLaptop ON Laptop(TenLaptop);
CREATE INDEX IX_Laptop_TenKhongDau ON Laptop(TenKhongDau);
CREATE INDEX IX_Laptop_MaDanhMuc ON Laptop(MaDanhMuc);
CREATE INDEX IX_Laptop_MaThuongHieu ON Laptop(MaThuongHieu);
CREATE INDEX IX_DonHang_NgayDatHang ON DonHang(NgayDatHang);
CREATE INDEX IX_DonHang_TrangThai ON DonHang(TrangThaiDonHang);
CREATE INDEX IX_KhachHang_TenKhachHang ON KhachHang(TenKhachHang);
CREATE UNIQUE NONCLUSTERED INDEX UX_KhachHang_MaTaiKhoan ON KhachHang(MaTaiKhoan) WHERE MaTaiKhoan IS NOT NULL;
GO

-- =====================================================================
-- DỮ LIỆU MẪU ĐẦY ĐỦ (>= 20 BẢN GHI MỖI BẢNG CHÍNH)
-- =====================================================================
INSERT INTO VaiTro (TenVaiTro, MoTa) VALUES
(N'ADMIN', N'Quản trị hệ thống'),
(N'STAFF', N'Nhân viên cửa hàng'),
(N'CUSTOMER', N'Khách hàng');
GO

-- Mật khẩu mẫu demo: 123456
INSERT INTO TaiKhoan (TenDangNhap, MatKhauMaHoa, HoTen, Email, SoDienThoai, MaVaiTro) VALUES
('admin',   '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Quản trị viên',  'admin@nexoralaptop.vn',   '0900000001', 1),
('staff01', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nhân viên 01',   'staff01@nexoralaptop.vn', '0900000002', 2),
('staff02', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nhân viên 02',   'staff02@nexoralaptop.vn', '0900000003', 2),
('khach01', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Nguyễn Văn An',  'an01@gmail.com',          '0911000001', 3),
('khach02', '$2a$10$mw/JoYTyBE6edzMrlctm/OLeRTvVpdB.4o6rMpm4dVGhe/AropZ0y', N'Trần Thị Bình',  'binh02@gmail.com',        '0911000002', 3);
GO

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

INSERT INTO Laptop
(MaSKU, TenLaptop, TenKhongDau, MaDanhMuc, MaThuongHieu, CPU, RAM_GB, DungLuongSSD_GB, CardDoHoa, KichThuocManHinh, HeDieuHanh, MauSac,
 GiaNhap, GiaNiemYet, GiaBan, SoLuongTon, DuongDanAnh, MoTa, BaoHanhThang)
VALUES
('LT001', N'Dell Inspiron 15 3530', N'dell inspiron 15 3530 intel core i5-1335u', 1, 1, N'Intel Core i5-1335U', 8, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13000000, 17990000, 14990000, 15, NULL, N'Laptop văn phòng Dell Inspiron 15 thiết kế bền bỉ, màn hình 15.6 inch Full HD sắc nét.', 12),
('LT002', N'Dell Vostro 14 3430', N'dell vostro 14 3430 intel core i5-1335u', 7, 1, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe Graphics', 14.0, N'Windows 11 Home', N'Đen', 14500000, 19490000, 16990000, 12, NULL, N'Laptop doanh nghiệp Dell Vostro 14 nhỏ gọn, bảo mật vân tay, hiệu năng ổn định.', 12),
('LT003', N'HP Pavilion 15-eg3091TU', N'hp pavilion 15-eg3091tu intel core i5-1335u', 1, 2, N'Intel Core i5-1335U', 16, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13800000, 18490000, 15990000, 10, NULL, N'Laptop HP Pavilion 15 vỏ kim loại sang trọng, phù hợp học tập và văn phòng.', 12),
('LT004', N'HP Victus 16-r0127TX', N'hp victus 16-r0127tx rtx 4050', 2, 2, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 16.1, N'Windows 11 Home', N'Đen', 23500000, 30990000, 27490000, 8, NULL, N'Laptop gaming HP Victus màn hình lớn 16.1 inch 144Hz, tản nhiệt tối ưu.', 24),
('LT005', N'Lenovo IdeaPad Slim 5 14', N'lenovo ideapad slim 5 14 ryzen 5 7530u', 4, 3, N'AMD Ryzen 5 7530U', 16, 512, N'AMD Radeon Graphics', 14.0, N'Windows 11 Home', N'Xám', 13500000, 17490000, 15990000, 14, NULL, N'Laptop mỏng nhẹ Lenovo IdeaPad Slim 5 vỏ nhôm cao cấp, thời lượng pin ấn tượng.', 24),
('LT006', N'Lenovo LOQ 15IRX9', N'lenovo loq 15irx9 rtx 4050', 2, 3, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Xám', 20000000, 26990000, 22990000, 9, NULL, N'Laptop gaming Lenovo LOQ 15 cấu hình mạnh mẽ cân mọi tựa game Esport và AAA.', 24),
('LT007', N'ASUS Vivobook 15 X1504ZA', N'asus vivobook 15 x1504za core i5', 1, 4, N'Intel Core i5-1235U', 16, 512, N'Intel Iris Xe Graphics', 15.6, N'Windows 11 Home', N'Bạc', 13200000, 17990000, 15990000, 16, NULL, N'Laptop ASUS Vivobook 15 bản lề mở 180 độ, kháng khuẩn ASUS Antimicrobial Guard.', 24),
('LT008', N'ASUS TUF Gaming A15 FA507', N'asus tuf gaming a15 fa507 rtx 4060', 2, 4, N'AMD Ryzen 7 8845HS', 16, 512, N'NVIDIA GeForce RTX 4060', 15.6, N'Windows 11 Home', N'Đen', 25000000, 32990000, 28990000, 7, NULL, N'Laptop gaming chuẩn độ bền quân đội MIL-STD-810H, màn hình 144Hz chuẩn màu sRGB.', 24),
('LT009', N'Acer Aspire 5 A515-58P', N'acer aspire 5 a515-58p core i5', 6, 5, N'Intel Core i5-1335U', 8, 512, N'Intel UHD Graphics', 15.6, N'Windows 11 Home', N'Bạc', 11500000, 16990000, 13990000, 18, NULL, N'Laptop sinh viên Acer Aspire 5 giá tốt, dễ dàng nâng cấp RAM và SSD.', 12),
('LT010', N'Acer Nitro V 15 ANV15-51', N'acer nitro v 15 rtx 4050', 2, 5, N'Intel Core i5-13420H', 16, 512, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Đen', 19500000, 25990000, 22990000, 11, NULL, N'Laptop gaming quốc dân Acer Nitro V 15 tản nhiệt 2 quạt, hiệu năng vượt tầm giá.', 24),
('LT011', N'Apple MacBook Air 13 M2', N'apple macbook air 13 m2 macos', 4, 6, N'Apple M2', 16, 512, N'Apple GPU 10-Core', 13.6, N'macOS', N'Xám', 21000000, 27990000, 23990000, 6, NULL, N'MacBook Air M2 màn hình Liquid Retina 13.6 inch, pin lên tới 18 giờ.', 12),
('LT012', N'Apple MacBook Pro 14 M4', N'apple macbook pro 14 m4 macos', 5, 6, N'Apple M4', 16, 1024, N'Apple GPU 10-Core', 14.2, N'macOS', N'Đen', 39000000, 46990000, 44990000, 4, NULL, N'MacBook Pro 14 chip M4 chuyên dựng phim, lập trình và đồ họa chuyên nghiệp.', 12),
('LT013', N'MSI Thin 15 B13UC', N'msi thin 15 b13uc rtx 3050', 2, 7, N'Intel Core i7-13620H', 16, 512, N'NVIDIA GeForce RTX 3050', 15.6, N'Windows 11 Home', N'Đen', 18500000, 23990000, 21990000, 10, NULL, N'Laptop gaming mỏng nhẹ MSI Thin 15 trọng lượng chỉ 1.86kg.', 24),
('LT014', N'MSI Katana 15 B13VEK', N'msi katana 15 b13vek rtx 4050', 2, 7, N'Intel Core i7-13620H', 16, 1024, N'NVIDIA GeForce RTX 4050', 15.6, N'Windows 11 Home', N'Đen', 26000000, 35490000, 29990000, 5, NULL, N'MSI Katana 15 trang bị bàn phím RGB 4 vùng, SSD 1TB siêu tốc.', 24),
('LT015', N'Gigabyte Gaming G5 MF5', N'gigabyte gaming g5 mf5 rtx 4060', 2, 8, N'Intel Core i7-13620H', 16, 512, N'NVIDIA GeForce RTX 4060', 15.6, N'Windows 11 Home', N'Đen', 22000000, 28490000, 24990000, 8, NULL, N'Gigabyte G5 hiệu năng RTX 4060 cực mạnh cho game thủ và kỹ thuật.', 24),
('LT016', N'Dell XPS 13 9340', N'dell xps 13 9340 core ultra 7', 5, 1, N'Intel Core Ultra 7', 16, 1024, N'Intel Arc Graphics', 13.4, N'Windows 11 Pro', N'Bạc', 38000000, 46990000, 46990000, 3, NULL, N'Siêu phẩm doanh nhân Dell XPS 13 viền siêu mỏng InfinityEdge, vỏ nhôm CNC.', 24),
('LT017', N'HP Envy x360 16 AI', N'hp envy x360 16 ai ryzen 5', 8, 2, N'AMD Ryzen 5 7530U', 16, 512, N'AMD Radeon Graphics', 16.0, N'Windows 11 Home', N'Bạc', 21000000, 26990000, 22990000, 6, NULL, N'Laptop xoay gập 360 độ HP Envy x360 màn hình cảm ứng đa điểm sắc nét.', 12),
('LT018', N'Lenovo ThinkPad E14 Gen 6', N'lenovo thinkpad e14 gen 6 core ultra 5', 7, 3, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Pro', N'Đen', 17500000, 22990000, 19990000, 13, NULL, N'Lenovo ThinkPad E14 bàn phím trứ danh, độ bền chuẩn quân đội cho lập trình viên.', 36),
('LT019', N'ASUS Zenbook 14 OLED', N'asus zenbook 14 oled core ultra 5', 11, 4, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Home', N'Xanh', 22000000, 27990000, 24990000, 9, NULL, N'ASUS Zenbook 14 màn hình Lumina OLED 3K chuẩn màu 100% DCI-P3.', 24),
('LT020', N'Acer Swift Go 14 AI', N'acer swift go 14 ai core ultra 5', 10, 5, N'Intel Core Ultra 5', 16, 512, N'Intel Arc Graphics', 14.0, N'Windows 11 Home', N'Bạc', 19000000, 24990000, 21990000, 7, NULL, N'Acer Swift Go 14 AI siêu mỏng nhẹ 1.3kg tích hợp NPU xử lý AI.', 24);
GO

INSERT INTO KhachHang (MaTaiKhoan, TenKhachHang, TenKhongDau, Email, SoDienThoai, DiaChi) VALUES
(4,    N'Nguyễn Văn An',    N'nguyen van an',    'an01@gmail.com',    '0911000001', N'Quận 1, TP.HCM'),
(5,    N'Trần Thị Bình',    N'tran thi binh',    'binh02@gmail.com',  '0911000002', N'Quận 3, TP.HCM'),
(NULL, N'Lê Minh Cường',    N'le minh cuong',    'cuong03@gmail.com', '0911000003', N'Quận 5, TP.HCM'),
(NULL, N'Phạm Thị Dung',    N'pham thi dung',    'dung04@gmail.com',  '0911000004', N'Quận 7, TP.HCM'),
(NULL, N'Hoàng Văn Đức',    N'hoang van duc',    'duc05@gmail.com',   '0911000005', N'Thủ Đức, TP.HCM'),
(NULL, N'Võ Thị Giang',     N'vo thi giang',     'giang06@gmail.com', '0911000006', N'Quận 10, TP.HCM'),
(NULL, N'Đặng Minh Hải',    N'dang minh hai',    'hai07@gmail.com',   '0911000007', N'Gò Vấp, TP.HCM'),
(NULL, N'Bùi Thị Hạnh',     N'bui thi hanh',     'hanh08@gmail.com',  '0911000008', N'Bình Thạnh, TP.HCM'),
(NULL, N'Đỗ Quốc Huy',      N'do quoc huy',      'huy09@gmail.com',   '0911000009', N'Tân Bình, TP.HCM'),
(NULL, N'Ngô Thị Lan',      N'ngo thi lan',      'lan10@gmail.com',   '0911000010', N'Tân Phú, TP.HCM'),
(NULL, N'Phan Văn Long',    N'phan van long',    'long11@gmail.com',  '0911000011', N'Quận 12, TP.HCM'),
(NULL, N'Vũ Thị Mai',       N'vu thi mai',       'mai12@gmail.com',   '0911000012', N'Nhà Bè, TP.HCM'),
(NULL, N'Đinh Quốc Nam',    N'dinh quoc nam',    'nam13@gmail.com',   '0911000013', N'Bình Chánh, TP.HCM'),
(NULL, N'Nguyễn Thị Oanh',  N'nguyen thi oanh',  'oanh14@gmail.com',  '0911000014', N'Quận 4, TP.HCM'),
(NULL, N'Trương Minh Phúc', N'truong minh phuc', 'phuc15@gmail.com',  '0911000015', N'Quận 6, TP.HCM'),
(NULL, N'Lý Thị Quỳnh',     N'ly thi quynh',     'quynh16@gmail.com', '0911000016', N'Quận 8, TP.HCM'),
(NULL, N'Đào Văn Sơn',      N'dao van son',      'son17@gmail.com',   '0911000017', N'Bình Tân, TP.HCM'),
(NULL, N'Phạm Minh Tâm',    N'pham minh tam',    'tam18@gmail.com',   '0911000018', N'Quận 11, TP.HCM'),
(NULL, N'Hoàng Thị Uyên',   N'hoang thi uyen',   'uyen19@gmail.com',  '0911000019', N'Củ Chi, TP.HCM'),
(NULL, N'Đặng Văn Việt',    N'dang van viet',    'viet20@gmail.com',  '0911000020', N'Hóc Môn, TP.HCM');
GO

INSERT INTO DonHang
(MaCodeDonHang, MaKhachHang, NguoiTao, TenNguoiNhan, SoDienThoaiNhan, DiaChiGiaoHang,
 TrangThaiDonHang, TamTinh, GiamGia, PhiVanChuyen, ThueVAT, TongTien, GhiChu)
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
    l.GiaBan, 0, 0, ROUND(l.GiaBan * 10.0 / 110.0, 0), l.GiaBan,
    N'Đơn hàng dữ liệu mẫu'
FROM Laptop l
JOIN KhachHang c ON c.MaKhachHang = l.MaLaptop
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

INSERT INTO ChiTietDonHang
(MaDonHang, MaLaptop, MaSKULuuTru, TenLaptopLuuTru, DonGia, GiaVonLuuTru, SoLuong, ThanhTien)
SELECT
    d.MaDonHang, l.MaLaptop, l.MaSKU, l.TenLaptop, l.GiaBan, l.GiaNhap, 1, l.GiaBan
FROM DonHang d
JOIN Laptop l ON d.MaCodeDonHang = 'DH' + RIGHT('000' + CAST(l.MaLaptop AS VARCHAR(3)), 3)
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

INSERT INTO ThanhToan
(MaDonHang, PhuongThucThanhToan, TrangThaiThanhToan, SoTien, MaGiaoDich, ThoiGianThanhToan)
SELECT
    MaDonHang,
    CASE WHEN MaDonHang % 4 = 0 THEN N'CASH' ELSE N'BANK_TRANSFER' END,
    CASE WHEN TrangThaiDonHang IN (N'COMPLETED', N'CONFIRMED', N'SHIPPING') THEN N'PAID' ELSE N'UNPAID' END,
    TongTien,
    'TXN-' + MaCodeDonHang,
    CASE WHEN TrangThaiDonHang IN (N'COMPLETED', N'CONFIRMED', N'SHIPPING') THEN SYSDATETIME() ELSE NULL END
FROM DonHang;
GO

INSERT INTO LichSuTrangThaiDonHang (MaDonHang, TrangThaiCu, TrangThaiMoi, NguoiThayDoi, GhiChuThayDoi)
SELECT MaDonHang, NULL, TrangThaiDonHang, 2, N'Khởi tạo trạng thái đơn hàng mẫu'
FROM DonHang;
GO

INSERT INTO BoDemMa (TenBoDem, GiaTriHienTai) VALUES
('DON_HANG', 20),
('PHIEU_NHAP', 20);
GO

INSERT INTO NhaCungCap (TenNhaCungCap, MaSoThue, NguoiLienHe, SoDienThoai, Email, DiaChi) VALUES
(N'Digiworld Việt Nam (DGW)', '0302861742', N'Nguyễn Quốc Huy', '02838231111', 'sales@dgw.com.vn', N'Quận 3, TP.HCM'),
(N'FPT Synnex Trading', '0102196012', N'Trần Minh Đức', '02473006666', 'partner@synnexfpt.com', N'Cầu Giấy, Hà Nội'),
(N'PSD - Petrosetco Distribution', '0305279325', N'Lê Thu Hà', '02839100999', 'contact@psd.com.vn', N'Quận 1, TP.HCM'),
(N'Viễn Sơn Distribution', '0301432001', N'Phạm Anh Tuấn', '02839290099', 'info@microstar.vn', N'Quận 3, TP.HCM'),
(N'Minh Thông IT Distributor', '0304455123', N'Võ Hoài Nam', '02838336666', 'sales@minhthong.vn', N'Quận 10, TP.HCM');
GO

INSERT INTO PhieuNhap
(MaCodePhieuNhap, NhaCungCap, MaNhaCungCap, GhiChu, TrangThai, TongTien, DaThanhToan, TrangThaiCongNo, NguoiTao, NgayTao, NguoiDuyet, NgayDuyet)
SELECT
    'PN' + RIGHT('000' + CAST(l.MaLaptop AS VARCHAR(3)), 3),
    N'Nhà phân phối ' + t.TenThuongHieu,
    ((l.MaLaptop - 1) % 5) + 1,
    N'Nhập kho dữ liệu mẫu',
    CASE WHEN l.MaLaptop <= 18 THEN N'APPROVED' ELSE N'PENDING' END,
    l.GiaNhap * 5,
    CASE WHEN l.MaLaptop <= 18 THEN l.GiaNhap * 5 ELSE 0 END,
    CASE WHEN l.MaLaptop <= 18 THEN N'PAID' ELSE N'UNPAID' END,
    2,
    SYSDATETIME(),
    CASE WHEN l.MaLaptop <= 18 THEN 1 ELSE NULL END,
    CASE WHEN l.MaLaptop <= 18 THEN SYSDATETIME() ELSE NULL END
FROM Laptop l
JOIN ThuongHieu t ON t.MaThuongHieu = l.MaThuongHieu
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

INSERT INTO ChiTietPhieuNhap (MaPhieuNhap, MaLaptop, SoLuong, DonGiaNhap, ThanhTien)
SELECT p.MaPhieuNhap, l.MaLaptop, 5, l.GiaNhap, l.GiaNhap * 5
FROM PhieuNhap p
JOIN Laptop l ON p.MaCodePhieuNhap = 'PN' + RIGHT('000' + CAST(l.MaLaptop AS VARCHAR(3)), 3)
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

INSERT INTO SoSerialLaptop (MaLaptop, SoSerial, MaChiTietDonHang, TrangThai, BaoHanhDen)
SELECT
    l.MaLaptop,
    'SN-' + l.MaSKU + '-2026-01',
    CASE WHEN l.MaLaptop <= 5 THEN ctdh.MaChiTietDonHang ELSE NULL END,
    CASE WHEN l.MaLaptop <= 5 THEN N'SOLD' ELSE N'IN_STOCK' END,
    CASE WHEN l.MaLaptop <= 5 THEN DATEADD(MONTH, l.BaoHanhThang, CAST(GETDATE() AS DATE)) ELSE NULL END
FROM Laptop l
LEFT JOIN ChiTietDonHang ctdh ON ctdh.MaLaptop = l.MaLaptop
WHERE l.MaLaptop BETWEEN 1 AND 20;
GO

INSERT INTO MaGiamGia (MaCode, MoTa, LoaiGiam, GiaTriGiam, GiamToiDa, DonToiThieu, SoLuotToiDa) VALUES
(N'NEXORA10', N'Giảm 10% tối đa 1.500.000đ cho đơn từ 15 triệu', N'PERCENT', 10, 1500000, 15000000, 200),
(N'SINHVIEN500K', N'Ưu đãi tựu trường giảm thẳng 500.000đ', N'FIXED', 500000, NULL, 10000000, 500),
(N'VIP2026', N'Giảm 1.000.000đ cho laptop cao cấp từ 25 triệu', N'FIXED', 1000000, NULL, 25000000, 100);
GO

INSERT INTO DiaChiKhachHang (MaKhachHang, TenNguoiNhan, SoDienThoai, TinhThanh, QuanHuyen, PhuongXa, DiaChiChiTiet, MacDinh) VALUES
(1, N'Nguyễn Văn An', '0911000001', N'TP. Hồ Chí Minh', N'Quận 1', N'Phường Bến Nghé', N'120 Nguyễn Huệ', 1),
(1, N'Nguyễn Văn An (Văn phòng)', '0911000001', N'TP. Hồ Chí Minh', N'Quận 3', N'Phường Võ Thị Sáu', N'45B Võ Văn Tần', 0),
(2, N'Trần Thị Bình', '0911000002', N'TP. Hồ Chí Minh', N'Quận 3', N'Phường 6', N'88 Pasteur', 1);
GO

INSERT INTO DanhGiaSanPham (MaLaptop, MaKhachHang, SoSao, TieuDe, NoiDung, DaMuaHang, DaDuyet, PhanHoiShop) VALUES
(1, 1, 5, N'Máy chạy mượt, giao nhanh', N'Đặt buổi sáng, chiều đã nhận được máy nguyên seal, màn hình sắc nét.', 1, 1, N'NEXORA LAPTOP cảm ơn anh An đã tin tưởng ủng hộ!'),
(2, 2, 5, N'Thiết kế gọn nhẹ chuẩn doanh nhân', N'Bàn phím gõ êm, mở máy nhanh, nhân viên tư vấn rất nhiệt tình.', 1, 1, N'Cảm ơn chị Bình nhiều ạ!');
GO

INSERT INTO BannerQuangCao (TieuDe, TieuDePhu, DuongDanAnh, LienKet, NhanNut, ThuTu, HienThi) VALUES
(N'Laptop AI 2026 — Chuẩn Sức Mạnh Mới', N'Giảm đến 20% + Tặng balo NEXORA & Gói bảo hành VIP', '/images/laptops/LT016/1.jpg', '/?categoryId=10', N'Khám phá Laptop AI', 1, 1),
(N'Đấu Trường Gaming RTX 4060', N'Hiệu năng đồ họa bùng nổ — Trả góp 0% & Freeship toàn quốc', '/images/laptops/LT008/1.jpg', '/?categoryId=2', N'Xem Laptop Gaming', 2, 1);
GO

INSERT INTO BaiVietCongNghe (TieuDe, DuongDan, TomTat, NoiDung, AnhBia, TacGia, HienThi) VALUES
(N'Cách chọn Laptop phù hợp ngành Công nghệ thông tin 2026', 'chon-laptop-it-2026',
 N'Hướng dẫn chọn CPU, RAM 16GB+, SSD NVMe và màn hình dịu mắt cho sinh viên IT và lập trình viên.',
 N'Khi học và làm việc trong ngành CNTT, việc sở hữu một chiếc laptop ổn định là yếu tố then chốt. Ưu tiên RAM từ 16GB trở lên để chạy Docker, IDE, máy ảo mượt mà. SSD tối thiểu 512GB chuẩn NVMe. Với sinh viên làm AI/Đồ họa, nên chọn GPU rời RTX 4050 trở lên.',
 '/images/laptops/LT018/1.jpg', N'NEXORA Tech Lab', 1),
(N'So sánh Intel Core Ultra vs AMD Ryzen AI thế hệ mới', 'intel-ultra-vs-ryzen-ai',
 N'Phân tích chi tiết hiệu năng NPU, thời lượng pin và khả năng xử lý tác vụ AI cục bộ.',
 N'Cả Intel Core Ultra và AMD Ryzen 8000 đều tích hợp NPU riêng giúp tăng tốc tác vụ họp trực tuyến, lọc ồn và xử lý mô hình AI cục bộ với mức tiêu thụ điện cực thấp.',
 '/images/laptops/LT019/1.jpg', N'NEXORA Editorial', 1);
GO

INSERT INTO CauHoiThuongGap (ChuDe, CauHoi, CauTraLoi, ThuTu, HienThi) VALUES
(N'Bảo hành', N'Thời gian bảo hành laptop tại NEXORA là bao lâu?', N'Mọi sản phẩm chính hãng tại NEXORA được bảo hành từ 12 đến 36 tháng theo tiêu chuẩn hãng, 1 đổi 1 trong 15 ngày đầu nếu có lỗi phần cứng từ nhà sản xuất.', 1, 1),
(N'Thanh toán', N'Cửa hàng hỗ trợ những hình thức thanh toán nào?', N'Chúng tôi hỗ trợ Thanh toán tiền mặt khi nhận hàng (COD), Chuyển khoản nhanh qua mã VietQR, và thanh toán trực tiếp tại showroom.', 2, 1),
(N'Vận chuyển', N'Đơn hàng bao nhiêu được miễn phí giao hàng?', N'Đơn hàng từ 15.000.000đ trở lên hoặc khách đến nhận tại showroom được miễn phí hoàn toàn phí vận chuyển.', 3, 1);
GO
