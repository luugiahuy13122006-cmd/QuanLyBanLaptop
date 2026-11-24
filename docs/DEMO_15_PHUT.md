# KỊCH BẢN TRÌNH BÀY BẢO VỆ ĐỒ ÁN TRONG 15 PHÚT — NEXORA LAPTOP

Mục tiêu: Trình bày mạch lạc toàn bộ **kiến trúc, nghiệp vụ thực tế, xử lý đồng thời, lập trình mạng TCP Socket và kiểm thử tự động** khớp 100% khung chấm điểm.

---

## Phút 0–2: Giới thiệu tổng quan & Kiến trúc
- Mở trình duyệt tại `http://localhost:8080` (chỉ ra giao diện Dark/Light Mode, Offline-first `static/vendor/` không phụ thuộc mạng phòng lab).
- Giới thiệu cấu trúc Maven 2 module (`laptop-shop-web` + `laptop-socket-client`), mô hình Controller → Service → Repository, và 29 bảng trên SQL Server (`ddl-auto=validate`).

## Phút 2–5: Luồng khách hàng thực tế (Storefront -> Checkout -> Bảo hành)
1. **Tìm kiếm không dấu & Bộ lọc:** Gõ `dell inspiron` hoặc nhấn `Ctrl + K` mở Command Palette, lọc hãng/giá, thêm 1 máy vào **So sánh (`/so-sanh`)**.
2. **Giỏ hàng DB & Mã giảm giá:** Thêm sản phẩm vào giỏ, đăng nhập `khach01 / 123456` (minh họa tự gộp giỏ session vào DB). Mở `/dat-hang`, áp mã `NEXORA10`, cho thấy hệ thống tự tính Freeship đơn từ 15 triệu.
3. **Thanh toán VietQR & COD:** Tạo 1 đơn `BANK_TRANSFER` (hiển thị QR có chữ ký HMAC) và 1 đơn `CASH` (COD).

## Phút 5–9: Phân hệ Quản trị & Vận hành cửa hàng (`admin / 123456`)
1. **Phân quyền:** Thử đăng nhập `staff01` -> thấy bị ẩn menu Tài khoản & Audit Log (nếu truy cập `/admin/accounts` nhận `403`).
2. **POS & Hóa đơn A4 (`/admin/pos`):** Quét/chọn nhanh SKU, xuất hóa đơn `/admin/orders/{id}/invoice` (in/tải PDF offline).
3. **Nhập kho & Giá vốn bình quân (`/admin/import-receipts`):** Duyệt phiếu nhập `PENDING` -> tự cộng tồn kho, cập nhật `PurchasePrice` bình quân gia quyền, ghi `BienDongKho` & công nợ nhà cung cấp.
4. **Serial & Bảo hành (`/admin/warranties` -> `/bao-hanh`):** Chuyển 1 đơn sang `COMPLETED` -> hệ thống tự cấp Serial kích hoạt hạn bảo hành; mở `/bao-hanh` tra cứu ngay theo mã đơn/Serial.
5. **Thống kê & Xuất báo cáo (`/admin/statistics` -> `/admin/statistics/report`):** Trình bày Chart.js doanh thu vs nhập kho, lợi nhuận gộp COGS, hiệu suất nhân viên, xuất báo cáo A4 (PDF/CSV).

## Phút 9–12: Xử lý đồng thời (`PESSIMISTIC_WRITE`) & Lập trình mạng TCP Socket
1. Giải thích đoạn mã `OrderServiceImpl.placeOrder()`: gom các `laptopId` trùng, sắp xếp ID tăng dần tránh deadlock, dùng `findAllByIdInForUpdate` (`PESSIMISTIC_WRITE`), khóa `MaGiamGia`, cập nhật `BoDemMa`.
2. Mở terminal chạy `MultiClientDemo` (5 client TCP đồng thời gửi `AUTH`, `CHECK_STOCK|LT001`, `SEARCH|asus`, `LOW_STOCK`) và mở `StockMonitorFrame` (nếu demo Swing GUI) để cho thấy Server TCP (`SocketServer` cổng `9090`) phục vụ song song qua `ThreadPoolExecutor` (8..32 luồng).

## Phút 12–15: Minh chứng Kiểm thử tự động & Hỏi đáp
- Chạy nhanh lệnh `mvn test` (hoặc trình chiếu kết quả chạy): **25/25 PASS** gồm `ConcurrencyOrderTest` (10 luồng tranh 5 máy), `Phase1SecurityAndConcurrencyTest` (20 luồng tranh 10 máy, 50 luồng sinh mã đơn, IDOR matrix, brute-force lockout), `Phase2FeaturesTest` (render 43 route + vendor offline), `SystemIntegrationTest`.
