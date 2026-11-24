# ĐẶC TẢ GIAO THỨC TCP SOCKET (`SocketServer` — Cổng `9090`)

- **Máy chủ:** [`SocketServer.java`](../laptop-shop-web/src/main/java/com/laptopshop/socket/SocketServer.java) (module `laptop-shop-web`), tự khởi động khi `laptop.socket.enabled=true`.
- **Client tách biệt:** Module `laptop-socket-client` gồm [`LaptopClientApp.java`](../laptop-socket-client/src/main/java/com/laptopshop/client/LaptopClientApp.java) (CLI tương tác), [`StockMonitorFrame.java`](../laptop-socket-client/src/main/java/com/laptopshop/client/StockMonitorFrame.java) (Swing GUI), và [`MultiClientDemo.java`](../laptop-socket-client/src/main/java/com/laptopshop/client/MultiClientDemo.java) (chạy 5 luồng client song song).
- **Tham số vận hành:**
  - `ThreadPoolExecutor`: core = `8`, max = `32`, queue = `100` (từ chối gọn khi quá tải).
  - Timeout socket đọc: `30_000 ms`, giới hạn độ dài dòng lệnh: `4096` ký tự.
  - Bảng mã ký tự: `UTF-8`, mỗi lệnh một dòng kết thúc `\n`.

---

## 1. Quy tắc xác thực (`AUTH`)

1. Ngoại trừ `PING` và `QUIT`, **mọi lệnh nghiệp vụ đều yêu cầu xác thực trước**.
2. Client gửi: `AUTH|<SOCKET_AUTH_TOKEN>` (mặc định dev: `NEXORA_SOCKET_DEV_TOKEN_2026`).
3. Phản hồi: `OK AUTHENTICATED` hoặc `ERR AUTH_REQUIRED` / `ERR INVALID_TOKEN`.

---

## 2. Danh sách lệnh & Định dạng gói tin

| Lệnh | Tham số | Phản hồi thành công | Mô tả |
|---|---|---|---|
| `PING` | *(không)* | `OK PONG` | Kiểm tra kết nối sống |
| `AUTH` | `<token>` | `OK AUTHENTICATED` | Xác thực phiên làm việc TCP |
| `LIST_CATEGORIES` | *(không)* | `BEGIN` ... `catId \| ten \| moTa` ... `END` | Danh sách danh mục đang hoạt động |
| `LIST_BRANDS` | *(không)* | `BEGIN` ... `brandId \| ten \| quocGia` ... `END` | Danh sách thương hiệu đang hoạt động |
| `LIST_LAPTOPS` | *(không)* | `BEGIN` ... `id \| sku \| ten \| hang \| gia \| tonKho` ... `END` | Danh sách laptop đang mở bán |
| `SEARCH` | `<từ khóa>` | `BEGIN` ... danh sách khớp ... `END` | Tìm theo tên/không dấu/SKU |
| `GET` | `<laptopId>` | `BEGIN` ... chi tiết sản phẩm ... `END` | Thông tin chi tiết 1 laptop đang mở bán |
| `CHECK_STOCK` | `<sku>` | `OK <sku>\|<ten>\|<tonKho>` hoặc `ERR NOT_FOUND` | Kiểm tra nhanh tồn kho tại quầy theo SKU |
| `ORDER_STATUS` | `<orderCode>` | `OK <orderCode>\|<trangThai>\|<tongTien>` | Tra cứu tiến độ đơn hàng qua TCP |
| `LOW_STOCK` | *(không)* | `BEGIN` ... sản phẩm tồn `<= 5` ... `END` | Liệt kê cảnh báo sắp hết hàng |
| `STOCK_ADJUST` | `<laptopId>\|<delta>` | `OK STOCK_UPDATED id=<id> stock=<mới>` | Cộng/trừ tồn kho an toàn bằng `PESSIMISTIC_WRITE` + ghi `BienDongKho` |
| `QUIT` | *(không)* | `OK BYE` | Đóng kết nối phiên TCP hiện tại |

---

## 3. Phiên trao đổi mẫu

```text
> AUTH|NEXORA_SOCKET_DEV_TOKEN_2026
< OK AUTHENTICATED
> CHECK_STOCK|LT001
< OK LT001|Dell Inspiron 15 3530|15
> LOW_STOCK
< BEGIN
< 16|LT016|Dell XPS 13 9340|3
< 12|LT012|Apple MacBook Pro 14 M4|4
< 14|LT014|MSI Katana 15 B13VEK|5
< END
> QUIT
< OK BYE
```
