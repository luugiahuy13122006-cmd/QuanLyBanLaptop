# KỊCH BẢN KIỂM THỬ THỦ CÔNG & NGHIỆM THU QA — NEXORA LAPTOP

Tài khoản mẫu (mật khẩu chung `123456`):
- Quản trị viên: `admin` (`ADMIN`)
- Nhân viên: `staff01` (`STAFF`)
- Khách hàng: `khach01` (`CUSTOMER`)

| Mã TC | Phân hệ | Thao tác thực hiện | Kết quả mong đợi | Trạng thái |
|---|---|---|---|---|
| **TC01** | Trang chủ & Tìm không dấu | Truy cập `/`, nhập `dell inspiron` hoặc `macbook`, bấm lọc giá/hãng & chuyển chế độ Sáng/Tối | Lọc đúng sản phẩm, không lỗi console, không tràn ngang ở 360/768/1280/1920 | Đạt |
| **TC02** | Offline Vendor | Tắt mạng / chế độ Offline DevTools, tải lại `/` và `/admin/statistics` | Bootstrap, Icons, Font, Chart.js tải từ `/vendor/` bình thường | Đạt |
| **TC03** | Giỏ hàng DB & Checkout | Chưa login -> thêm giỏ (session), sau đó login `khach01` -> mở `/gio-hang` -> `/dat-hang` | Giỏ session tự gộp vào DB; áp mã `NEXORA10` giảm đúng; đơn `>= 15Tr` freeship | Đạt |
| **TC04** | Thanh toán COD | Chọn `CASH` -> Đặt hàng | Đơn `PENDING`, thanh toán `UNPAID`; Admin chuyển `COMPLETED` -> Payment tự sang `PAID` | Đạt |
| **TC05** | Thanh toán VietQR & Hủy | Chọn `BANK_TRANSFER` -> xem QR -> xác nhận chuyển khoản qua QR / Webhook -> hủy đơn trước xác nhận TT | Tự động xác nhận thanh toán khi đúng chữ ký HMAC; khi hủy đơn tự động hoàn tồn kho & hoàn lượt coupon | Đạt |
| **TC06** | Bảo mật & Phân quyền | `khach01` truy cập `/admin/dashboard` (`403`); `staff01` truy cập `/admin/coupons` hoặc `/admin/suppliers` (`403`); kiểm tra phân quyền động | Chặn 100%, nhân viên chỉ thao tác đúng chức năng được Admin cấp | Đạt |
| **TC07** | Đăng ký & Brute-force | Đăng ký 4 trường (`email`, `username`, `password`, `confirmPassword`) kiểm tra trùng đỏ; sai MK 5 lần | Báo lỗi đỏ khi trùng email/username; đăng nhập bằng Email thành công; khóa sau 5 lần sai | Đạt |
| **TC08** | Nhập kho & Nhà cung cấp | `staff01` chọn NCC từ dropdown tại `/admin/import-receipts/create` -> `admin` duyệt | Cộng tồn kho, tính lại giá vốn bình quân gia quyền & công nợ NCC; Admin sửa/ngừng hợp tác/xóa NCC | Đạt |
| **TC09** | Bảo hành & Serial | Đưa đơn sang `COMPLETED` -> lấy mã đơn hoặc Serial tra cứu ở `/bao-hanh` -> gửi yêu cầu BH -> Admin cập nhật tại `/admin/warranties` | Serial cấp phát tự động, hạn bảo hành kích hoạt theo tháng, phiếu BH cập nhật trạng thái | Đạt |
| **TC10** | Hóa đơn, Báo cáo & Chat SSE | Mở `/admin/orders/{id}/invoice` & `/admin/statistics/report` xuất PDF/CSV; khách nhắn `/ho-tro` | Hóa đơn & Báo cáo A4 đầy đủ thông tin; Admin/Staff nhận thông báo SSE + badge tin chưa đọc ngay | Đạt |
| **TC11** | Đồng thời & Socket | Chạy `ConcurrencyOrderTest`, `Phase1SecurityAndConcurrencyTest`, `MultiClientDemo` (5 client TCP) | 20 luồng tranh 10 máy: đúng 10 thành công, tồn kho `0`; 5 client TCP nhận phản hồi chuẩn | Đạt |
