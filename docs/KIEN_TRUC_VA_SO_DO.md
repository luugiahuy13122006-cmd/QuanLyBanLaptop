# KIẾN TRÚC HỆ THỐNG & SƠ ĐỒ NGHIỆP VỤ — NEXORA LAPTOP

## 1. Kiến trúc tổng thể (Layered CSR + Socket + SQL Server)

```mermaid
flowchart LR
  Browser["Trình duyệt Khách / Admin\n(Thymeleaf + Offline Vendor)"] -->|"HTTP / SSE"| Sec["Spring Security 6\n(BCrypt, CSRF, RBAC, Lockout)"]
  Sec --> Ctrl["Controllers\n(Store, Order, Warranty, POS, Auth...)"]
  TcpClients["laptop-socket-client\n(CLI / Swing / MultiClientDemo)"] -->|"TCP :9090 (AUTH)"| Sock["SocketServer\n(ThreadPoolExecutor 8..32)"]
  Ctrl --> Svc["Services (@Transactional)\n(Order, Laptop, Import, Coupon, Warranty, Stats)"]
  Sock --> Repo
  Svc -->|"PESSIMISTIC_WRITE\nOrdered Locking"| Repo["Spring Data JPA Repositories\n(29 Entities)"]
  Repo --> DB["SQL Server QL_Laptop\n(29 bảng PascalCase)"]
  Scheduler["OrderTimeoutScheduler & KhoaTacVu"] --> Svc
```

---

## 2. Vòng đời Trạng thái Đơn hàng (`OrderStatus`)

```mermaid
stateDiagram-v2
  [*] --> PENDING: Đặt hàng (COD / VietQR)
  PENDING --> CONFIRMED: Xác nhận thanh toán / Duyệt đơn
  CONFIRMED --> PREPARING: Đóng gói & gán vận đơn
  PREPARING --> SHIPPING: Giao cho ĐVVC
  SHIPPING --> COMPLETED: Giao thành công (cấp Serial + COD->PAID)
  PENDING --> CANCELLED: Khách/Admin hủy hoặc Quá hạn 30p (hoàn kho + coupon)
  CONFIRMED --> REFUND_REQUESTED: Khách yêu cầu hoàn tiền
  PREPARING --> REFUND_REQUESTED: Khách yêu cầu hoàn tiền
  REFUND_REQUESTED --> REFUNDED: Admin xác nhận hoàn tiền (hoàn kho)
  SHIPPING --> RETURNED: Hoàn hàng (hoàn kho)
```

---

## 3. Luồng đặt hàng đồng thời an toàn (`placeOrder` + `PESSIMISTIC_WRITE`)

```mermaid
sequenceDiagram
  actor C1 as Khách A / POS A
  actor C2 as Khách B / POS B
  participant O as OrderServiceImpl
  participant L as LaptopRepo (FOR UPDATE)
  participant G as CodeGenerator (BoDemMa)
  participant S as StockMovement & Serial

  par Hai giao dịch tranh cùng SKU
    C1->>O: placeOrder(laptopId=1, qty=1)
    C2->>O: placeOrder(laptopId=1, qty=1)
  end
  O->>L: findAllByIdInForUpdate([1]) sorted ASC
  Note over L: C1 giữ PESSIMISTIC_WRITE trước, C2 chờ trong Transaction
  O->>O: Kiểm tra active, tồn kho >= qty, coupon lock
  O->>L: Trừ StockQuantity (-1)
  O->>G: nextOrderCode() -> DH021
  O->>S: Ghi BienDongKho & DonHang
  O-->>C1: Commit (thành công)
  L-->>C2: Giải phóng lock cho C2 đọc tồn kho mới
  O-->>C2: Kiểm tra lại tồn kho (còn hàng -> OK / hết -> BusinessException)
```
