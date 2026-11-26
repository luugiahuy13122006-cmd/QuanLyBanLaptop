# Hệ Thống Quản Lý & Bán Hàng Trực Tuyến **NEXORA LAPTOP** (`QuanLyBanLaptop`)

> Đồ án môn học **Công nghệ Java** — Nhóm 5 thành viên • Phiên bản nâng cấp chuẩn vận hành cửa hàng thật (Production-ready + Offline-safe + Dark/Light Design System).

---

## 1. Thành viên nhóm & Phân công công việc

| Mã SV | Họ và tên | Vai trò | Phần phụ trách chính |
|---|---|---|---|
| 2001240171 | **Lư Gia Huy** (Nhóm trưởng) | Thành viên 1 | Phân tích nghiệp vụ, thiết kế CSDL SQL Server (`QL_LAPTOP.sql`, 26 bảng), sơ đồ ERD, dữ liệu mẫu |
| 2001240138 | **Phạm Hữu Hiệu** | Thành viên 2 | Quản lý Laptop/Danh mục/Thương hiệu/Phiếu nhập kho/Nhà cung cấp/Serial, Socket Server TCP (`9090`) & `laptop-socket-client` |
| 2001240077 | **Trương Xuân Đạt** | Thành viên 3 | Design System Light/Dark (`static/css/*`), Offline Vendor (`static/vendor/*`), Website bán hàng, So sánh, Bảo hành công khai, Tin tức, Chat |
| 2001240193 | **Nguyễn Tuấn Khang** | Thành viên 4 | Giỏ hàng DB, Checkout (VietQR + COD + Webhook), Mã giảm giá, Vận đơn, Hóa đơn A4 PDF, Hoàn tiền, POS, Thống kê & Xuất báo cáo A4/CSV, `@Scheduled` & Pessimistic Lock |
| 2001240043 | **Vũ Gia Bảo** | Thành viên 5 | Spring Security + BCrypt + Lockout + Reset MK, Quản lý Tài khoản & Audit Log, Bảo vệ IDOR, Kiểm thử tổng hợp (`SystemIntegrationTest`, `Phase1SecurityAndConcurrencyTest`, `Phase2FeaturesTest`), Tài liệu |

---

## 2. Công nghệ & Kiến trúc

- **Backend:** Java 17+, Spring Boot 3.3.4, Spring Web MVC, Spring Security 6 + BCrypt, Spring Data JPA / Hibernate (`ddl-auto=validate`), Jakarta Bean Validation, JavaMail, ZXing QR.
- **Frontend:** Thymeleaf + Fragment dùng chung + Bootstrap 5.3.2 + Bootstrap Icons + Chart.js 4.4.4 + html2pdf.js 0.10.1 + Google Fonts WOFF2 (**đã tải cục bộ trong `static/vendor/`**, chạy đầy đủ khi mất mạng).
- **CSDL:** Microsoft SQL Server (`QL_Laptop` — 26 bảng tiếng Việt chuẩn PascalCase, `PhysicalNamingStrategyStandardImpl`, `globally_quoted_identifiers=true`).
- **Xử lý đồng thời & Thời gian thực:** `@Transactional`, khóa bi quan `PESSIMISTIC_WRITE` theo thứ tự ID tăng dần chống bán quá tồn/chết khóa, bảng `BoDemMa` sinh mã đơn/phiếu nhập an toàn đa luồng, `KhoaTacVu` chống chạy trùng `@Scheduled`, và Server-Sent Events (`/admin/sse/notifications`).
- **Lập trình mạng TCP Socket:** `SocketServer` (`ServerSocket` + `ThreadPoolExecutor` 8..32 luồng, cổng `9090`, xác thực `AUTH|<token>`) và module client tách biệt `laptop-socket-client` (`LaptopClientApp`, `StockMonitorFrame`, `MultiClientDemo`).

---

## 3. Cấu trúc thư mục chính

```text
QuanLyBanLaptop-main/
├── pom.xml                                  # Parent Maven Multi-Module
├── README.md
├── database/
│   └── QL_LAPTOP.sql                        # Khởi tạo sạch 26 bảng + >= 20 bản ghi mẫu/bảng chính
├── laptop-shop-web/                         # Module Web & Socket Server (Spring Boot)
└── laptop-socket-client/                    # Module TCP Client tách biệt (CLI & Swing GUI)
```

---

## 4. Cài đặt CSDL SQL Server (Nhanh gọn trên mọi máy)

Chỉ cần cài sẵn **JDK 17+**, **Maven** và **Microsoft SQL Server (SSMS)**:
1. Mở **SQL Server Management Studio (SSMS)**, kết nối tới SQL Server trên máy và chạy file `database/QL_LAPTOP.sql` để tạo mới CSDL `QL_Laptop` cùng đầy đủ dữ liệu mẫu.
   - *(Hoặc chạy bằng dòng lệnh: `sqlcmd -S localhost -E -b -f 65001 -i database\QL_LAPTOP.sql`)*

---

## 5. Cấu hình & Chạy ứng dụng

Không cần thiết lập biến môi trường phức tạp hay chỉnh cấu hình hệ điều hành:
1. Mở file [`laptop-shop-web/src/main/resources/application.properties`](laptop-shop-web/src/main/resources/application.properties), kiểm tra và sửa lại cho khớp SQL Server trên máy của bạn (mặc định là `localhost:1433`, `username=sa`, `password=123`).
2. Nhấn **Run `LaptopShopApplication.java`** trong IntelliJ IDEA (hoặc chạy qua Maven):
   ```powershell
   cd laptop-shop-web
   mvn spring-boot:run
   ```
- Website & Quản trị: `http://localhost:8080`
- Kiểm tra đa luồng tồn kho (Admin): `http://localhost:8080/admin/stock/test`
- TCP Socket Server: `localhost:9090` (tự khởi động cùng `LaptopShopApplication`)

*(Ghi chú production: khi deploy thực tế với `SPRING_PROFILES_ACTIVE=prod`, cấu hình biến môi trường `DB_USERNAME`, `DB_PASSWORD`, `QR_HMAC_SECRET`, `PAYMENT_WEBHOOK_SECRET`, `PAYMENT_MODE=PRODUCTION`, `QR_TUNNEL_ENABLED=false`.)*

### Chạy TCP Socket Client tách biệt (`laptop-socket-client` — phục vụ chấm điểm Lập trình mạng)
Module `laptop-socket-client` là ứng dụng **Client TCP độc lập** kết nối tới `SocketServer` cổng `9090` (không ảnh hưởng khi bạn bè/giảng viên chỉ chạy `LaptopShopApplication`):
```powershell
cd laptop-socket-client
mvn compile
java -cp target/classes com.laptopshop.client.LaptopClientApp localhost 9090
# Hoặc demo 5 client TCP kết nối song song:
java -cp target/classes com.laptopshop.client.MultiClientDemo
```

### Tài khoản mẫu (Mật khẩu mặc định: `123456`)
| Vai trò | Tên đăng nhập | Quyền hạn chính |
|---|---|---|
| **ADMIN** | `admin` | Toàn quyền hệ thống, duyệt phiếu nhập kho, cấu hình phân quyền nhân viên, quản lý NCC/coupon/tài khoản, xem Audit Log |
| **STAFF** | `staff01` | Phân quyền động theo từng chức năng (`VIEW`/`EDIT`/`FULL`) do Admin cấu hình (đơn hàng, bảo hành, đánh giá, hỗ trợ trực tuyến, lập phiếu nhập) |
| **CUSTOMER** | `khach01` | Mua sắm, đăng nhập Username/Email, giỏ hàng DB, sổ địa chỉ, đặt hàng COD/VietQR, đánh giá ẩn danh (`K******`), hủy/hoàn tiền, tra cứu bảo hành |

---

## 6. Kiểm thử tự động

Chạy toàn bộ kiểm thử tích hợp, đa luồng, bảo mật, phân quyền, Socket 5 luồng và render 43 route giao diện bằng H2 (`MODE=MSSQLServer`):

```powershell
mvn clean verify
```
Bao gồm 4 bộ test: `ConcurrencyOrderTest`, `SystemIntegrationTest`, `Phase1SecurityAndConcurrencyTest`, `Phase2FeaturesTest`.
