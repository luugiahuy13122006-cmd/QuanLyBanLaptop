# ĐỐI CHIẾU KHUNG CHẤM ĐIỂM ĐỒ ÁN MÔN CÔNG NGHỆ JAVA — NEXORA LAPTOP

Bảng đối chiếu toàn bộ yêu cầu đồ án và các hạng mục đạt điểm tối đa kèm vị trí mã nguồn kiểm chứng thực tế.

| Mã | Hạng mục chấm điểm | Hiện trạng | Minh chứng mã nguồn & Kiểm thử |
|---|---|---|---|
| **1.1** | Kiến trúc phân lớp Controller → Service → Repository | Đạt | `com.laptopshop.{controller,service,service.impl,repository}` |
| **1.2** | Spring Boot 3 + Spring Data JPA + Thymeleaf + Bootstrap 5 | Đạt | `laptop-shop-web/pom.xml` (Spring Boot 3.3.4, Java 17), `static/vendor/bootstrap/` |
| **1.3** | CSDL quan hệ ≥ 4 bảng, có quan hệ 1–N & chuẩn hóa | Đạt | **29 bảng** trong `database/QL_LAPTOP.sql`, `Order 1–N OrderDetail/Payment/OrderStatusHistory`, `ImportReceipt 1–N ImportReceiptDetail`, `Laptop 1–N ProductSerial/ProductReview` |
| **1.4** | Dữ liệu mẫu ≥ 20 bản ghi mỗi bảng chính | Đạt | Kiểm chứng trực tiếp SQL Server `QL_Laptop`: `DanhMucLaptop (20)`, `ThuongHieu (20)`, `Laptop (20)`, `KhachHang (20)`, `DonHang (20)`, `ChiTietDonHang (20)`, `ThanhToan (20)`, `PhieuNhap (20)`, `SoSerialLaptop (20)` |
| **1.5** | Schema khớp entity với `ddl-auto=validate` | Đạt | Khởi động `LaptopShopApplication` kết nối trực tiếp SQL Server `QL_Laptop` với `ddl-auto=validate` thành công 0 lỗi |
| **2.1** | CRUD đầy đủ + Tìm kiếm + Phân trang + Sắp xếp | Đạt | `StoreController`, `LaptopController`, `CategoryController`, `BrandController`, `CustomerController`, `OrderController`, `AccountController`, `CouponAndSupplierController` + fragment `ui :: paginationBar` |
| **2.2** | Tìm kiếm tiếng Việt không dấu/có dấu | Đạt | `VietnameseTextNormalizer.java` + cột `TenKhongDau` trên bảng `Laptop` |
| **2.3** | Validation + báo lỗi cạnh ô nhập + thông báo thành công + confirm xóa | Đạt | DTO `@NotBlank`/`@Min`/`@Pattern` + `th:errors`, toast/flash `flashMessages`, modal xác nhận `data-confirm` (`toast.js` + `ui.html`) |
| **3.1** | Xử lý đồng thời có ý nghĩa nghiệp vụ (`PESSIMISTIC_WRITE`, chống overselling) | Đạt | `OrderServiceImpl.placeOrder()` khóa `Laptop` theo ID tăng dần (`findAllByIdInForUpdate`), gộp SKU trùng, khóa `Coupon` (`CouponService`), `BoDemMa` (`CodeGeneratorService`), `KhoaTacVu` (`JobLockService`) |
| **3.2** | Kiểm thử đa luồng chứng minh an toàn đồng thời | Đạt | `ConcurrencyOrderTest` (10 luồng tranh 5 máy + `updateStockSafe` 10 luồng) & `Phase1SecurityAndConcurrencyTest` (20 luồng tranh 10 máy & 50 luồng sinh mã đơn không trùng) |
| **3.3** | `@Transactional` & hoàn kho/coupon khi hủy hoặc hoàn tiền | Đạt | `@Transactional` tại `OrderServiceImpl`, `ImportReceiptServiceImpl`, `WarrantyService`, `CouponService`; ghi `BienDongKho` (`StockMovementService`) |
| **4.1** | Lập trình mạng TCP Socket server & module client tách biệt | Đạt | `SocketServer.java` (cổng `9090`, `ThreadPoolExecutor` 8..32, `soTimeout=30s`, `AUTH`) + module `laptop-socket-client` (`LaptopClientApp`, `StockMonitorFrame`, `MultiClientDemo`) |
| **4.2** | Phục vụ đồng thời ≥ 3 client TCP | Đạt | `MultiClientDemo` chạy **5 client đồng thời** + `Phase1SecurityAndConcurrencyTest.testSocketAuthConcurrentClientsAndAdjustStock()` |
| **5.1** | Spring Security + BCrypt + phân quyền URL & method (`ADMIN`/`STAFF`/`CUSTOMER`) | Đạt | `SecurityConfig.java` (`@EnableMethodSecurity`), `AccountServiceImpl` (BCrypt + lockout 5 lần sai + ép đổi MK mặc định ở prod) |
| **5.2** | Bảo vệ IDOR & kiểm soát truy cập tài nguyên riêng tư | Đạt | `OrderServiceImpl.getOrderForCustomer` kiểm tra sở hữu; `Phase1SecurityAndConcurrencyTest.testSecurityAndIdorMatrix()` xác minh khách B nhận 403/404 khi chạm đơn/minh chứng khách A |
| **5.3** | Giao diện dùng Fragment chung, menu theo vai trò, trang lỗi thân thiện, tiếng Việt có dấu | Đạt | `fragments/{head,scripts,header,footer,admin-layout,ui,brand}.html`, `error/{403,404,error}.html`, kiểm tra 43 route `200 OK` |
| **6.1** | Offline-safe (không phụ thuộc Internet phòng lab) | Đạt | Toàn bộ Bootstrap, Icons (+WOFF2/WOFF), Chart.js, html2pdf, Google Fonts WOFF2 nằm tại `static/vendor/` có `onerror` CDN fallback |
| **6.2** | Nghiệp vụ thực tế cửa hàng nâng cao (POS, VietQR+Webhook+COD, Bảo hành Serial, Coupon, Xuất báo cáo A4/PDF/CSV, Light/Dark UI) | Đạt | `OrderController` (`/invoice`), `EngageAndPosController` (`/admin/pos`, `/yeu-thich`, `/so-sanh`), `WarrantyController` (`/bao-hanh`), `StatisticsController` (`/report`, `/report.csv`) |
