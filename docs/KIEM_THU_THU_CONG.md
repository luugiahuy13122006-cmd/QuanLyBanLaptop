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
| **TC05** | Thanh toán VietQR & Hủy | Chọn `BANK_TRANSFER` -> xem QR -> Tải ảnh minh chứng (`.jpg/.png` hợp lệ & thử file giả) -> Admin duyệt hoặc từ chối | Ảnh giả bị chặn (`ImageIO` + magic bytes); khi duyệt/hủy đơn hoàn tồn kho & hoàn lượt coupon | Đạt |
| **TC06** | Bảo mật & IDOR | `khach01` truy cập `/admin/dashboard` (bị `403`); thử xem/hủy/up minh chứng đơn của khách khác | Chặn 100%, không rò stacktrace ở profile `prod` | Đạt |
| **TC07** | Brute-force & Reset MK | Nhập sai MK 5 lần liên tiếp | Tài khoản bị khóa 15 phút; luồng Quên MK tạo token đổi MK mới thành công | Đạt |
| **TC08** | Nhập kho & Giá vốn BQ | `staff01` lập phiếu nhập ở `/admin/import-receipts/create` -> `admin` duyệt | Cộng tồn kho, tính lại giá vốn bình quân gia quyền, ghi `BienDongKho` & công nợ NCC | Đạt |
| **TC09** | Bảo hành & Serial | Đưa đơn sang `COMPLETED` -> lấy mã đơn hoặc Serial tra cứu ở `/bao-hanh` -> gửi yêu cầu BH -> Admin cập nhật tại `/admin/warranties` | Serial cấp phát tự động, hạn bảo hành kích hoạt theo tháng, phiếu BH cập nhật trạng thái | Đạt |
| **TC10** | POS & In Hóa đơn / Báo cáo | Bán nhanh 1 đơn ở `/admin/pos` -> mở `/admin/orders/{id}/invoice`; mở `/admin/statistics/report` xuất PDF/CSV | Hóa đơn & Báo cáo A4 có đủ người lập, ngày xuất, mã báo cáo, chữ ký, CSV mở Excel không vỡ font/injection | Đạt |
| **TC11** | Đồng thời & Socket | Chạy `ConcurrencyOrderTest`, `Phase1SecurityAndConcurrencyTest`, `MultiClientDemo` (5 client TCP) | 20 luồng tranh 10 máy: đúng 10 thành công, tồn kho `0`; 5 client TCP nhận phản hồi chuẩn | Đạt |
