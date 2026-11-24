# ĐỀ CƯƠNG BÁO CÁO ĐỒ ÁN 5 CHƯƠNG — NEXORA LAPTOP

## CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI & PHÂN TÍCH YÊU CẦU
1.1. Đặt vấn đề & bài toán quản lý bán laptop thực tế (online đa kênh + tại quầy POS + bảo hành theo Serial).  
1.2. Đối tượng sử dụng & Phân quyền: Khách hàng (`CUSTOMER`), Nhân viên (`STAFF`), Quản trị viên (`ADMIN`), Hệ thống POS/Kho kết nối TCP Socket.  
1.3. Yêu cầu chức năng (Storefront, Tìm không dấu, Giỏ hàng DB, Mã giảm giá, VietQR + Webhook + COD, Nhập kho giá vốn bình quân, Bảo hành Serial, Thống kê & Xuất báo cáo A4/PDF/CSV).  
1.4. Yêu cầu phi chức năng: Toàn vẹn giao dịch (`@Transactional`), chống bán quá tồn (`PESSIMISTIC_WRITE`), bảo mật (BCrypt, Lockout, IDOR, CSRF, CSP), Offline-safe (`static/vendor/`).

## CHƯƠNG 2: PHÂN TÍCH & THIẾT KẾ HỆ THỐNG
2.1. Kiến trúc phân lớp Controller → Service → Repository và giao tiếp TCP Socket tách module.  
2.2. Thiết kế Cơ sở dữ liệu (`QL_Laptop` — 29 bảng tiếng Việt PascalCase, quan hệ 1–N, khóa ngoại, index tìm kiếm & lọc `UX_TaiKhoan_Email`, `IX_Laptop_TenKhongDau`). *(Đính kèm `docs/So_do_ERD_QL_LAPTOP.png` & `docs/KIEN_TRUC_VA_SO_DO.md`)*.  
2.3. Thiết kế cơ chế xử lý đồng thời: khóa hàng theo thứ tự ID tăng dần, bảng `BoDemMa` chống trùng mã, `KhoaTacVu` chống chạy trùng `@Scheduled`.  
2.4. Đặc tả giao thức TCP Socket cổng `9090` (`AUTH`, `CHECK_STOCK`, `STOCK_ADJUST`, `LOW_STOCK`, `ThreadPoolExecutor` 8..32 luồng).

## CHƯƠNG 3: CÀI ĐẶT & XÂY DỰNG CHƯƠNG TRÌNH
3.1. Công nghệ sử dụng (Java 17, Spring Boot 3.3.4, Thymeleaf, Bootstrap 5.3.2 Offline, SQL Server 2019/2022, ZXing, Chart.js, html2pdf).  
3.2. Phân hệ Bảo mật & Tài khoản (`SecurityConfig`, `AccountServiceImpl`, Reset mật khẩu qua token, ép đổi MK mặc định ở `prod`).  
3.3. Phân hệ Cửa hàng, Thanh toán & Xử lý đồng thời (`OrderServiceImpl`, `CouponService`, `WarrantyService`, `PaymentWebhookController`).  
3.4. Phân hệ Giao diện Light/Dark Mode, Command Palette (`Ctrl+K`) và Báo cáo A4/PDF/CSV.  
3.5. Phân hệ TCP Socket Server & Client tách biệt (`laptop-socket-client`).

## CHƯƠNG 4: KIỂM THỬ & ĐÁNH GIÁ KẾT QUẢ
4.1. Môi trường kiểm thử (SQL Server thật với `ddl-auto=validate`, H2 `MODE=MSSQLServer`, Chromium 4 kích thước màn hình `360/768/1280/1920`).  
4.2. Kết quả kiểm thử tự động: `ConcurrencyOrderTest`, `SystemIntegrationTest`, `Phase1SecurityAndConcurrencyTest`, `Phase2FeaturesTest`.  
4.3. Kết quả kiểm thử đa client TCP Socket (`MultiClientDemo` 5 client song song).  
4.4. Kết quả kiểm thử thủ công & trình duyệt Light/Dark Mode (`docs/KIEM_THU_THU_CONG.md`).

## CHƯƠNG 5: KẾT LUẬN & HƯỚNG PHÁT TRIỂN
5.1. Tổng kết các kết quả đạt được so với khung tiêu chí chấm điểm (`docs/CHECKLIST_RUBRIC.md`).  
5.2. Hạn chế còn tồn tại & hướng mở rộng (tích hợp cổng thanh toán VNPAY/MoMo thật từ chế độ `PRODUCTION`, xuất hóa đơn điện tử chuẩn XML/API Tổng cục Thuế, phân tích dự báo nhập hàng).
