# NHẬT KÝ NÂNG CẤP HỆ THỐNG — NEXORA LAPTOP

> Tài liệu ghi nhận trung thực mọi bước khảo sát, sửa lỗi, bổ sung tính năng, nâng cấp giao diện và kết quả kiểm chứng thực tế qua từng Phase.

---

## PHASE 0 — KHẢO SÁT & KIỂM TOÁN (Hoàn thành 25/11/2026)

### Đã thực hiện
- Đọc và kiểm toán toàn bộ mã nguồn 2 module (`laptop-shop-web`, `laptop-socket-client`), `database/QL_LAPTOP.sql`, 29 template HTML, `style.css`, `nexora.css`, `nexora.js`, `README.md` và `docs/*`.
- Kiểm tra baseline trên môi trường thực tế:
  - `mvn -q -DskipTests compile`: **EXIT=0**.
  - `mvn test`: **14/14 PASS** (`ConcurrencyOrderTest`: 2, `SystemIntegrationTest`: 12; chạy trên H2 `MODE=MSSQLServer`; `laptop-socket-client`: 0 test).
  - SQL Server cục bộ (`MSSQLSERVER`): đang chạy (`Running`).
  - Kết nối CDN (`bootstrap@5.3.2`): `HTTP 200` (sẵn sàng tải về `static/vendor/` cho chế độ demo offline).
- Xuất báo cáo kiểm toán đầy đủ 5 trục + ma trận phân quyền URL × Vai trò + đối chiếu khung chấm điểm tại [`docs/AUDIT_REPORT.md`](AUDIT_REPORT.md).

### Tóm tắt lỗ hổng trọng yếu phát hiện
- **Blocker (3):** `BUS-01` (endpoint `/qr-pay/**` cho phép tự xác nhận `PAID` công khai không qua chế độ DEMO/PROD hay webhook ngân hàng), `BUS-02` + `SEC-01` (`/orders/place` mở cho `CUSTOMER`, cho phép IDOR `customerId` và tự truyền `discount` 0đ), `UI-01` (toàn bộ CSS/JS/Font/Chart.js/html2pdf phụ thuộc CDN ngoài, hỏng giao diện khi phòng lab mất mạng).
- **High (17):** Bao gồm `QrCodeService` hardcode HMAC key & không expiry, không snapshot giá vốn (`GiaVonLuuTru`) / ship / VAT, duyệt phiếu nhập chưa cập nhật `purchasePrice`, kẹt hoàn tiền đơn đang giao, giỏ hàng session-only, upload ảnh laptop không check magic bytes, `SocketServer` thiếu `AUTH` / `soTimeout` / giới hạn pool & chưa có lệnh ghi DB, thiếu brute-force lockout & đổi mật khẩu mặc định, `QL_LAPTOP.sql` lỗi `UNIQUE Email` khi null & thiếu `@Version`, cùng các lỗi UI mất dữ liệu form/thiếu phân trang-sắp xếp ở `customer/my-orders` & `import-receipts/list`.

---

## KẾ HOẠCH THỰC HIỆN CÁC PHASE TIẾP THEO

1. **PHASE 1 — Sửa toàn bộ lỗi Blocker/High/Med/Low:**
   - Tách cấu hình `dev` / `prod`, xóa mật khẩu DB mặc định khỏi `application.properties`, thêm `application.properties.example`.
   - Khóa `/orders/place`, kiểm tra `discount` theo vai trò, nâng cấp chế độ thanh toán `DEMO` / `PRODUCTION` + `PaymentWebhookController` + `PaymentGatewayService`.
   - Bổ sung `@Version` trên entity chính, sửa `LaptopService.updateLaptop` (không ghi đè tồn kho trực tiếp) & upload ảnh magic bytes + `ImageIO` re-encode, hoàn thiện luồng trạng thái + hoàn tiền, chuẩn hóa `BigDecimal` & múi giờ `Asia/Ho_Chi_Minh`, sửa lỗi `CodeGeneratorService`, thêm `BienDongKho` & `NhatKyHeThong`.
   - Chống brute-force đăng nhập, chính sách mật khẩu mạnh, Quên mật khẩu (token 1 lần), bắt đổi MK mặc định ở `prod`, `SessionRegistry` thu hồi phiên, Security Headers, clamp `page`/`size`, CSV formula guard.
   - Nâng cấp `SocketServer` (`AUTH`, `ThreadPoolExecutor`, `soTimeout`, giới hạn dòng, đóng kết nối sạch, lệnh `CHECK_STOCK`, `ORDER_STATUS`, `LOW_STOCK`, `STOCK_ADJUST`) + giao diện Swing & `MultiClientDemo` (5 client) trong `laptop-socket-client`.
2. **PHASE 2 — Tính năng vận hành thật (P0 → P1 → P2):**
   - **P0:** Giỏ hàng DB + gộp khi đăng nhập, Sổ nhiều địa chỉ giao hàng, Phí vận chuyển + VAT + Mã vận đơn, Mã giảm giá (`MaGiamGia` có lock chống dùng quá lượt), Số Serial & Bảo hành công khai (`/bao-hanh`), Hóa đơn in A4/A5 + PDF, Quản lý Nhà cung cấp & công nợ đơn giản, Dịch vụ thông báo Email (DEV log / PROD `JavaMailSender`).
   - **P1:** Đánh giá sản phẩm (khách đã mua, chờ duyệt), Yêu thích (Wishlist) + So sánh tối đa 4 laptop + Đã xem gần đây, Bộ lọc nâng cao không reload (`fetch` + `pushState`), Gợi ý sản phẩm, Dashboard mở rộng (lợi nhuận/ngày-tuần-tháng/tỉ lệ hủy/AOV/hiệu suất nhân viên), POS tại quầy nhanh + quét mã vạch/SKU, thông báo thời gian thực SSE cho nhân viên.
   - **P2:** Tin công nghệ / Blog & FAQ & Banner động quản trị được, Chat hỗ trợ khách ↔ nhân viên (SSE + polling), PWA cơ bản (`manifest.json`, `sw.js`), `sitemap.xml`, `robots.txt`, SEO/OG/JSON-LD, đa ngôn ngữ `vi`/`en`.
3. **PHASE 3 — Hệ thống thiết kế & Làm mới toàn bộ giao diện (Offline-ready):**
   - Tải toàn bộ thư viện Bootstrap, Bootstrap Icons, Chart.js, html2pdf và WOFF2 font Việt về `static/vendor/`.
   - Nghiên cứu xu hướng 2025–2026 (Apple, ROG, MSI, Lenovo, Dell, Razer, Framework, TGDĐ, FPT, CellphoneS, Phong Vũ, GearVN), ghi bài học vào đây.
   - Xây Design System `tokens.css` → `base.css` → `components.css` → `pages/*.css`, Dark/Light mode không nháy trắng, `Command Palette Ctrl/⌘+K`, hiệu ứng 3D tilt, bay giỏ hàng, autocomplete, View Transitions API, modal/toast dùng chung, làm mới **toàn bộ** 29+ template + mọi trang mới.
4. **PHASE 4 — Kiểm thử toàn diện, DB SQL Server thực & Tài liệu bảo vệ đồ án:**
   - Chạy `mvn clean verify`, test đa luồng 20/50 luồng lặp 5 lần, test ma trận bảo mật URL×Vai trò, test Socket 5 client, kiểm thử tĩnh mọi template, chạy app thật đối chiếu SQL Server & `browser` đa màn hình (360/768/1280/1920), hoàn thiện toàn bộ tài liệu `docs/` & `README.md`, rồi push lên GitHub.

---

## PHASE 1 — SỬA LỖI NGHIỆP VỤ, BẢO MẬT, ĐỒNG THỜI & SOCKET (Hoàn thành 25/11/2026)

### Đã sửa & kiểm chứng
1. **Cấu hình & Hồ sơ môi trường (`dev`/`prod`):**
   - Gỡ mật khẩu SQL mặc định khỏi [`application.properties`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/application.properties); tách [`application-dev.properties`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/application-dev.properties), [`application-prod.properties`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/application-prod.properties), [`application.properties.example`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/application.properties.example). Bật `Asia/Ho_Chi_Minh`, `HttpOnly` + `SameSite=Lax`, `spring.task.scheduling.pool.size=4`.
2. **Thanh toán, Webhook & QR:**
   - Thêm [`PaymentGatewayService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/PaymentGatewayService.java) & [`PaymentWebhookController.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/controller/PaymentWebhookController.java) (`/api/webhooks/payment`, HMAC-SHA256 + kiểm tra khớp số tiền + chống trùng lặp `gatewayRef`).
   - Tách `DEMO` vs `PRODUCTION`: ở `PRODUCTION`, khách không thể tự bấm xác nhận chuyển khoản qua QR/button. Nâng cấp [`QrCodeService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/QrCodeService.java) ký kèm tổng tiền & hạn thanh toán từ biến môi trường `QR_HMAC_SECRET`.
3. **Toàn vẹn dữ liệu, Kho & Đơn hàng:**
   - Bổ sung `@Version` (`PhienBan`), `TenKhongDau` (tìm tiếng Việt không dấu qua [`VietnameseTextNormalizer.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/util/VietnameseTextNormalizer.java)), trạng thái `PREPARING`, cột giá vốn snapshot `GiaVonLuuTru`, `PhiVanChuyen`, `ThueVAT`, `MaGiamGia`, `DonViVanChuyen`, `MaVanDon`.
   - Thêm sổ biến động kho [`StockMovement`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/entity/StockMovement.java) (`BienDongKho`), nhật ký hệ thống [`AuditLog`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/entity/AuditLog.java) (`NhatKyHeThong`), khóa tác vụ định kỳ [`JobLock`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/entity/JobLock.java) (`KhoaTacVu`), token đặt lại MK [`PasswordResetToken`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/entity/PasswordResetToken.java).
   - Sửa [`OrderServiceImpl.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/impl/OrderServiceImpl.java): gộp trùng `laptopId` + khóa `PESSIMISTIC_WRITE` tăng dần, giới hạn `STAFF` giảm giá <= 5%, xử lý hoàn tiền khi đang giao trả hàng, `TransactionSynchronization` dọn ảnh minh chứng sau commit/rollback.
   - Sửa [`ImportReceiptServiceImpl.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/impl/ImportReceiptServiceImpl.java) (giá nhập bình quân gia quyền) & [`LaptopService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/LaptopService.java) (không ghi đè tồn kho khi sửa laptop, kiểm tra magic bytes + re-encode `ImageIO` JPG).
   - Sửa [`CodeGeneratorService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/CodeGeneratorService.java) & thứ tự lấy khóa tránh cạn pool Hikari.
4. **Bảo mật & Web:**
   - Khóa `/orders/place` cho `ADMIN`/`STAFF`, chính sách MK >= 8 ký tự (chữ + số, không `.trim()`), khóa 15 phút sau 5 lần đăng nhập sai, quy trình Quên/Đặt lại MK + ép đổi MK mặc định ở `prod`, `SessionRegistry` + `Security Headers` + ẩn stacktrace ở `prod` + chặn CSV formula injection + `PaginationUtils` clamp `page`/`size`.
5. **Socket Server & Client:**
   - Nâng cấp [`SocketServer.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/socket/SocketServer.java) (`ThreadPoolExecutor` 8..32, `soTimeout=30s`, `MAX_LINE=4096`, `AUTH|<token>`, lệnh `CHECK_STOCK`, `ORDER_STATUS`, `LOW_STOCK`, `STOCK_ADJUST`), [`LaptopClientApp.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-socket-client/src/main/java/com/laptopshop/client/LaptopClientApp.java), [`StockMonitorFrame.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-socket-client/src/main/java/com/laptopshop/client/StockMonitorFrame.java) & [`MultiClientDemo.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-socket-client/src/main/java/com/laptopshop/client/MultiClientDemo.java) (5 client đồng thời).

### Kết quả kiểm chứng Phase 1
- Lệnh chạy: `mvn.cmd test` trên toàn bộ reactor 2 module (`laptop-shop-web` + `laptop-socket-client`).
- Kết quả: **21/21 PASS (`Failures: 0, Errors: 0, Skipped: 0`)**, `BUILD SUCCESS` trên cả 2 module (`ConcurrencyOrderTest`: 2, `SystemIntegrationTest`: 12, [`Phase1SecurityAndConcurrencyTest`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/test/java/com/laptopshop/Phase1SecurityAndConcurrencyTest.java): 7 — gồm 20 luồng tranh mua 10 máy, 50 luồng sinh mã đơn hàng, ma trận IDOR & role, Webhook HMAC + idempotency, brute-force lockout + reset MK, upload magic bytes & Socket 5 client đồng thời có `AUTH` + `STOCK_ADJUST`).

---

## PHASE 2 — TÍNH NĂNG CỬA HÀNG VẬN HÀNH THẬT P0 → P1 → P2 (Hoàn thành 25/11/2026)

### Đã bổ sung & kiểm chứng
- **P0 (Vận hành cốt lõi):**
  - Giỏ hàng CSDL (`GioHangChiTiet` + [`CartService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/CartService.java)) tự động gộp giỏ session vào DB khi đăng nhập ([`SecurityConfig.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/config/SecurityConfig.java)).
  - Sổ nhiều địa chỉ giao hàng (`DiaChiKhachHang` + [`CustomerAddressService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/CustomerAddressService.java)) & chọn địa chỉ khi checkout.
  - Tính phí ship (`0đ` với đơn `>= 15.000.000đ` hoặc nhận tại quầy, ngược lại `50.000đ`), tách thuế VAT 10% trên hóa đơn, trạng thái `PREPARING` + cập nhật đơn vị/mã vận đơn (`/admin/orders/{id}/tracking`).
  - Mã giảm giá (`MaGiamGia` + [`CouponService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/CouponService.java)) dùng khóa bi quan chống vượt `SoLuotToiDa`, hoàn lượt khi hủy/hoàn tiền, API kiểm tra nhanh `/api/coupons/check`.
  - Quản lý số Serial (`SoSerialLaptop`) & tra cứu/yêu cầu bảo hành công khai (`/bao-hanh`) + tự động cấp serial kích hoạt hạn bảo hành khi đơn `COMPLETED`.
  - Hóa đơn in A4/A5 & phiếu giao (`/admin/orders/{id}/invoice`, `/don-hang-cua-toi/{id}/hoa-don`), quản lý Nhà cung cấp & công nợ (`NhaCungCap`), màn hình xem Nhật ký hệ thống (`/admin/audit-logs`) & Biến động kho (`/admin/stock-movements`), dịch vụ [`EmailService.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/EmailService.java).
- **P1 & P2 (Tăng trưởng, phân tích, POS & trải nghiệm):**
  - Đánh giá sản phẩm đã mua có kiểm duyệt & phản hồi (`DanhGiaSanPham`), Yêu thích (`/yeu-thich`), So sánh tối đa 4 laptop (`/so-sanh`), Sản phẩm đã xem gần đây, API lọc không reload (`/api/store/filter`) & autocomplete (`/api/store/autocomplete`).
  - Dashboard & Thống kê nâng cao: doanh thu hôm nay/tuần/tháng, lợi nhuận gộp từ giá vốn snapshot, tỉ lệ hủy, AOV, hiệu suất nhân viên ([`StatisticsServiceImpl.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/java/com/laptopshop/service/impl/StatisticsServiceImpl.java)).
  - Bán hàng nhanh tại quầy (`/admin/pos`), thông báo thời gian thực SSE (`/admin/sse/notifications`), Tin tức công nghệ & FAQ & Banner động (`/tin-tuc`), Chat hỗ trợ trực tuyến (`/ho-tro`, `/api/chat/**`), PWA (`manifest.json`, `sw.js`), SEO (`robots.txt`, `sitemap.xml`), đa ngôn ngữ `vi`/`en` (`messages*.properties`).
- **CSDL đồng bộ SQL Server:** Cập nhật toàn diện [`database/QL_LAPTOP.sql`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/database/QL_LAPTOP.sql) (29 bảng, `>= 20` bản ghi mẫu trên các bảng chính) và tạo [`database/QL_LAPTOP_upgrade.sql`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/database/QL_LAPTOP_upgrade.sql) chạy lặp idempotent.
- **Kết quả kiểm thử Phase 2:** `mvn.cmd test` đạt **24/24 PASS** (`Failures: 0, Errors: 0, Skipped: 0`) gồm `Phase2FeaturesTest`.

---

## PHASE 3 — HỆ THỐNG THIẾT KẾ & LÀM MỚI TOÀN BỘ GIAO DIỆN (Hoàn thành 25/11/2026)

### Đã nâng cấp & kiểm chứng
1. **Design System Light/Dark + Offline-first Vendor:**
   - Hoàn thiện kiến trúc CSS modular ([`tokens.css`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/static/css/tokens.css), [`base.css`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/static/css/base.css), [`components.css`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/static/css/components.css), [`pages/storefront.css`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/static/css/pages/storefront.css)) hỗ trợ Dark/Light mode mượt mà không flash trắng và hiệu ứng GPU-only tôn trọng `prefers-reduced-motion`.
   - Lưu cục bộ 100% thư viện tại `static/vendor/` (Bootstrap 5.3.2, Bootstrap Icons 1.11.3 + WOFF2/WOFF, Chart.js 4.4.4, html2pdf.js 0.10.1, Google Fonts WOFF2) kèm `onerror` fallback trong [`head.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/head.html), [`scripts.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/scripts.html), [`invoice.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/orders/invoice.html), [`report.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/statistics/report.html) và [`statistics/index.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/statistics/index.html).
2. **Storefront, Customer, Auth, Error & Admin Templates:**
   - Làm mới toàn bộ các fragment ([`header.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/header.html), [`footer.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/footer.html), [`admin-layout.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/admin-layout.html), [`ui.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/fragments/ui.html)) với `Command Palette (Ctrl/⌘+K)`, `data-confirm` modal dùng chung, phân trang trượt `paginationBar`, và SSE thông báo real-time.
   - Làm mới 100% template khu vực cửa hàng (`store/*`), khách hàng (`customer/*`), xác thực (`auth/*`), trang lỗi (`error/*`), quản trị hiện có (`dashboard`, `laptops`, `categories`, `brands`, `customers`, `accounts`, `orders`, `import-receipts`, `statistics`).
   - Bổ sung trọn bộ giao diện quản trị Phase 2–3: [`admin/orders/invoice.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/orders/invoice.html), [`admin/pos/index.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/pos/index.html), [`admin/warranties/index.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/warranties/index.html), [`admin/coupons/list.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/coupons/list.html), [`admin/suppliers/list.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/suppliers/list.html), [`admin/audit-logs/list.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/audit-logs/list.html), [`admin/stock-movements/list.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/stock-movements/list.html), [`admin/reviews/list.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/reviews/list.html), [`admin/content/index.html`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/main/resources/templates/admin/content/index.html).
3. **Kiểm chứng render tự động:**
   - Bổ sung test `testAllUpdatedTemplatesAndOfflineVendorAssetsRenderCleanly` trong [`Phase2FeaturesTest.java`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/laptop-shop-web/src/test/java/com/laptopshop/Phase2FeaturesTest.java) kiểm tra render `200 OK` toàn bộ **43 route** công khai, khách hàng, quản trị và tài nguyên tĩnh cục bộ.
   - `mvn.cmd test`: **25/25 PASS (`Failures: 0, Errors: 0, Skipped: 0`)**, `BUILD SUCCESS` trên cả 2 module.

---

## PHASE 4 — KIỂM THỬ TOÀN DIỆN, SQL SERVER THỰC TẾ, TRÌNH DUYỆT & TÀI LIỆU (Hoàn thành 25/11/2026)

### Đã kiểm chứng thực tế
1. **CSDL SQL Server thật (`QL_Laptop`):**
   - Thực thi sạch [`database/QL_LAPTOP.sql`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/database/QL_LAPTOP.sql) (`-b -f 65001` UTF-8) và chạy lặp **2 lần liên tiếp** [`database/QL_LAPTOP_upgrade.sql`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/database/QL_LAPTOP_upgrade.sql) — `0` lỗi (idempotent 100%).
   - Xác minh đủ **29 bảng** trong `QL_Laptop` (`DanhMucLaptop`, `ThuongHieu`, `Laptop`, `KhachHang`, `DonHang`, `ChiTietDonHang`, `ThanhToan`, `LichSuTrangThaiDonHang`, `PhieuNhap`, `ChiTietPhieuNhap`, `SoSerialLaptop` đều có **20 bản ghi mẫu**).
   - Khởi động thực tế `laptop-shop-web-1.0.0.jar` kết nối TCP JDBC tới SQL Server với `spring.jpa.hibernate.ddl-auto=validate`: `Initialized JPA EntityManagerFactory` thành công 0 lỗi sai lệch schema.
2. **Mạng TCP Socket (Server/Client tách biệt):**
   - Chạy `MultiClientDemo` (5 client kết nối đồng thời tới `SocketServer` cổng `9090` với `AUTH|<token>`): cả 5 client (`LIST_CATEGORIES`, `LIST_LAPTOPS`, `SEARCH|asus`, `CHECK_STOCK|LT001`, `LOW_STOCK`) hoàn tất sạch (`exit 0`).
3. **Kiểm thử tự động & Stress đa luồng:**
   - `mvn.cmd clean verify` đạt **25/25 PASS (`Failures: 0, Errors: 0, Skipped: 0`)**, `BUILD SUCCESS` cả 2 module (`ConcurrencyOrderTest`: 2, `Phase1SecurityAndConcurrencyTest`: 7, `Phase2FeaturesTest`: 4, `SystemIntegrationTest`: 12).
   - Chạy lặp **5 lần liên tiếp** tổ hợp đồng thời `ConcurrencyOrderTest,Phase1SecurityAndConcurrencyTest`: `ALL_5_REPEATS_PASSED` (`exit 0`).
4. **Kiểm thử trình duyệt thực tế & khắc phục UI:**
   - Rà soát 4 khung màn hình (`360x800`, `768x1024`, `1280x800`, `1920x1080`), chế độ Light/Dark Mode và khu vực quản trị (`/admin/dashboard`, `/admin/pos`, `/admin/statistics` Chart.js, `/admin/statistics/report`, `/admin/warranties`, `/admin/coupons`).
   - Đã khắc phục các lỗi phát hiện qua trình duyệt: tràn ngang `360px` tại Flash Sale, đồng bộ `data-bs-theme` + contrast bảng thông số Dark Mode, giữ 1 dòng không gãy ký hiệu `₫` / `Ctrl K` / `Quản trị`, và tối ưu scroll reveal.
5. **Hoàn thiện tài liệu bảo vệ:**
   - Cập nhật [`README.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/README.md), [`docs/PHAN_CONG_FILE_THANH_VIEN.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/PHAN_CONG_FILE_THANH_VIEN.md) và tạo mới [`docs/CHECKLIST_RUBRIC.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/CHECKLIST_RUBRIC.md), [`docs/KIEM_THU_THU_CONG.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/KIEM_THU_THU_CONG.md), [`docs/GIAO_THUC_SOCKET.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/GIAO_THUC_SOCKET.md), [`docs/DEMO_15_PHUT.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/DEMO_15_PHUT.md), [`docs/KIEN_TRUC_VA_SO_DO.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/KIEN_TRUC_VA_SO_DO.md), [`docs/BAO_CAO_KHUNG_5_CHUONG.md`](file:///D:/QuanLyBanLaptop-main/QuanLyBanLaptop-main/docs/BAO_CAO_KHUNG_5_CHUONG.md).


### Nghiên cứu xu hướng UI/UX công nghệ 2025–2026 & Thương hiệu Laptop
- **Tham khảo 10 trang bán lẻ & hãng công nghệ:** Apple Mac Store (bento spec grid, whitespace khoáng đạt, micro-copy rõ), ASUS ROG / MSI (dark glass surface, glow viền tinh tế khi hover, badge GPU/CPU nổi bật), Lenovo / Dell / Framework (comparison table cố định cột đầu, spec chips cân xứng), Razer (tối giản high-contrast, CTA nổi bật), TGDĐ / FPT Shop / CellphoneS / Phong Vũ / GearVN (filter ngang chip + thanh giá rõ, FLASH SALE countdown, tra cứu bảo hành 1 ô, giỏ hàng & checkout 2 cột tóm tắt dính).
- **Bài học áp dụng:**
  1. Giảm tải thị giác bằng biến màu theo semantic (`--bg`, `--surface`, `--surface-2`, `--border`, `--text`, `--accent`, `--radius`, `--shadow`) hỗ trợ mượt cả **Light Mode** lẫn **Dark Mode** (kích hoạt trong `<head>` trước khi vẽ DOM để không nháy trắng).
  2. Thẻ sản phẩm đồng bộ tỷ lệ 16:10, spec pills (CPU/RAM/SSD/GPU/Màn hình), badge giảm %, hover nâng 3D nhẹ (`perspective` + `transform` GPU 60fps), nút Yêu thích & So sánh nhanh, tôn trọng `prefers-reduced-motion`.
  3. Navbar glassmorphic tích hợp `Command Palette (Ctrl/⌘ + K)`, autocomplete tìm kiếm tức thì, nút đổi giao diện Sáng/Tối, đổi ngôn ngữ `VI/EN`, và huy hiệu giỏ hàng/so sánh động.
  4. Offline-first 100%: toàn bộ Bootstrap 5.3.2, Bootstrap Icons 1.11.3 (+ `.woff2`/`.woff`), Chart.js 4.4.4, html2pdf 0.10.1 và bộ font tiếng Việt (`Plus Jakarta Sans`, `Inter`, `JetBrains Mono`) được lưu cục bộ tại `static/vendor/` kèm `onerror` fallback.
