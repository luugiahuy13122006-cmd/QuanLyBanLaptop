# Hệ Thống Quản Lý Website Bán Laptop (QuanLyBanLaptop)

Đồ án môn học **Công nghệ Java** – Nhóm 5 thành viên.
Ứng dụng Web thương mại điện tử & quản trị bán laptop xây dựng bằng **Spring Boot**, **Thymeleaf**, **Bootstrap 5**, **Microsoft SQL Server**, **Xử lý đồng thời (Multithreading + Pessimistic Locking)** và **Lập trình mạng TCP Socket (Server/Client tách biệt)**.

---

## 1. Thành viên nhóm & Phân công công việc

| Mã SV | Họ và tên | Vai trò | Phần phụ trách chính |
|-------|-----------|---------|----------------------|
| 2001240171 | **Lư Gia Huy** (Nhóm trưởng) | Thành viên 1 | Phân tích nghiệp vụ, thiết kế CSDL SQL Server (`QL_LAPTOP.sql`), sơ đồ ERD, dữ liệu mẫu |
| 2001240138 | **Phạm Hữu Hiệu** | Thành viên 2 | Backend & giao diện quản lý Laptop, Danh mục, Thương hiệu; Kiểm thử đa luồng tồn kho; Socket Server & Client tách biệt |
| 2001240077 | **Trương Xuân Đạt** | Thành viên 3 | Giao diện Website khách hàng (Thymeleaf + Bootstrap 5 responsive), tìm kiếm/lọc, giỏ hàng, đặt hàng, fragment dùng chung |
| 2001240193 | **Nguyễn Tuấn Khang** | Thành viên 4 | Backend Khách hàng, Đơn hàng, Thanh toán, Lịch sử trạng thái, Thống kê doanh thu, Transaction & Pessimistic Lock chống Overselling, kiểm thử 10 luồng |
| 2001240043 | **Vũ Gia Bảo** | Thành viên 5 | Tài khoản, Đăng ký/Đăng nhập/Phân quyền (Spring Security + BCrypt), Quản lý tài khoản, Tích hợp toàn hệ thống, Xử lý ngoại lệ, Kiểm thử tổng thể, README |

---

## 2. Yêu cầu môi trường (Bắt buộc)

- **JDK**: **Java 17** (LTS) – khuyến nghị Oracle JDK 17 hoặc OpenJDK 17.
- **IDE**: **IntelliJ IDEA** (khuyến nghị) hoặc Eclipse / VS Code.
- **Build tool**: **Apache Maven 3.8+** (hoặc dùng Maven tích hợp sẵn trong IntelliJ IDEA).
- **Cơ sở dữ liệu**: **Microsoft SQL Server** (2019 / 2022 / Express) + **SQL Server Management Studio (SSMS)**.

---

## 3. Cấu trúc dự án

Dự án được tổ chức theo mô hình **Maven Multi-Module** gồm 2 module độc lập (`laptop-shop-web` và `laptop-socket-client`) dùng chung cơ sở dữ liệu `QL_Laptop`:

```text
QuanLyBanLaptop/
├── pom.xml                             # Parent Maven Multi-Module
├── README.md                           # Hướng dẫn cài đặt, chạy và kiểm thử
├── .gitignore
├── database/
│   └── QL_LAPTOP.sql                   # Script SQL Server (10 bảng + mẫu)
├── docs/
│   ├── BaoCao_LaptopShop.docx               # Báo cáo đồ án hoàn chỉnh (Word)
│   ├── BaoCao_LaptopShop.pdf                # Báo cáo đồ án hoàn chỉnh (PDF)
│   └── Slide_LaptopShop_CongNgheJava.pptx   # Slide thuyết trình đồ án
├── laptop-shop-web/                    # Module 1: Web (8080) & Socket (9090)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/laptopshop/
│       │   │   ├── LaptopShopApplication.java
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── CustomUserDetails.java
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   └── DataInitializer.java
│       │   │   ├── controller/
│       │   │   │   ├── StoreController.java
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── AdminDashboardController.java
│       │   │   │   ├── AccountController.java
│       │   │   │   ├── LaptopController.java
│       │   │   │   ├── CategoryController.java
│       │   │   │   ├── BrandController.java
│       │   │   │   ├── CustomerController.java
│       │   │   │   ├── OrderController.java
│       │   │   │   ├── StatisticsController.java
│       │   │   │   └── StockController.java
│       │   │   ├── service/
│       │   │   │   ├── AccountService.java, BrandService.java
│       │   │   │   ├── CategoryService.java, CustomerService.java
│       │   │   │   ├── LaptopService.java, OrderService.java
│       │   │   │   ├── StatisticsService.java
│       │   │   │   └── impl/
│       │   │   │       ├── AccountServiceImpl.java
│       │   │   │       ├── CustomerServiceImpl.java
│       │   │   │       ├── OrderServiceImpl.java
│       │   │   │       └── StatisticsServiceImpl.java
│       │   │   ├── repository/
│       │   │   │   ├── AccountRepository.java, RoleRepository.java
│       │   │   │   ├── LaptopRepository.java, CategoryRepository.java
│       │   │   │   ├── BrandRepository.java, CustomerRepository.java
│       │   │   │   ├── OrderRepository.java, OrderDetailRepository.java
│       │   │   │   └── PaymentRepository.java, OrderStatusHistoryRepository.java
│       │   │   ├── entity/
│       │   │   │   ├── Account, Role, Customer, Laptop, Category, Brand
│       │   │   │   ├── Order, OrderDetail, OrderStatus, OrderStatusHistory
│       │   │   │   └── Payment, PaymentMethod, PaymentStatus
│       │   │   ├── dto/
│       │   │   │   ├── AccountDTO, BrandDTO, CategoryDTO, CustomerDTO
│       │   │   │   ├── LaptopDTO, RegisterDTO, CartItemDTO, CheckoutFormDTO
│       │   │   │   ├── PlaceOrderRequest, OrderItemRequest
│       │   │   │   └── MonthlyRevenueDTO, OrderStatusCountDTO, TopSellingLaptopDTO
│       │   │   ├── exception/
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── BusinessException.java
│       │   │   │   └── ResourceNotFoundException.java
│       │   │   └── socket/
│       │   │       └── SocketServer.java     # TCP Socket Server (:9090)
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application.properties.example
│       │       ├── static/
│       │       │   ├── css/style.css
│       │       │   └── images/
│       │       │       ├── laptop-default.svg
│       │       │       └── laptops/LT001..LT020/  # 5 ảnh/mẫu (1.jpg–5.jpg)
│       │       └── templates/
│       │           ├── store/                # index, detail, cart, checkout, ...
│       │           ├── auth/                 # login, register, profile
│       │           ├── admin/                # dashboard, accounts/, brands/, ...
│       │           ├── fragments/            # header, footer, admin-layout
│       │           └── error/                # 403, 404, error
│       └── test/
│           ├── java/com/laptopshop/
│           │   ├── ConcurrencyOrderTest.java     # Test 10 luồng tranh 5 máy
│           │   └── SystemIntegrationTest.java    # Test RBAC, FK, Socket, tồn kho
│           └── resources/
│               └── application.properties        # H2 (MODE=MSSQLServer)
└── laptop-socket-client/               # Module 2: Java Socket Client độc lập
    ├── pom.xml
    └── src/main/java/com/laptopshop/client/
        ├── LaptopClientApp.java        # Client CLI tương tác đơn lẻ (:9090)
        └── MultiClientDemo.java        # Demo 3 Socket Client chạy song song
```

### Giải thích chi tiết hai module

- **Module 1 – `laptop-shop-web`**: Ứng dụng Spring Boot trung tâm (mở web tại cổng `8080` và tự khởi chạy `SocketServer` tại cổng `9090`), chia rõ theo kiến trúc phân tầng:
  - `config/`: Cấu hình Spring Security 6 (`SecurityConfig`), nạp quyền từ `TaiKhoan` (`CustomUserDetails*`) và đồng bộ dữ liệu khởi tạo (`DataInitializer`).
  - `controller/` & `templates/`: Phục vụ cả 2 phân hệ — cửa hàng khách hàng (`StoreController`, `templates/store/*`), xác thực (`AuthController`, `templates/auth/*`) và quản trị (`AdminDashboardController`, `Account/Laptop/Category/Brand/Customer/Order/Statistics/StockController`, `templates/admin/*`).
  - `service/` & `repository/`: Xử lý nghiệp vụ giao dịch (`@Transactional`), chống bán quá tồn kho bằng khóa bi quan `PESSIMISTIC_WRITE` (`OrderServiceImpl`, `LaptopService`), thống kê doanh thu (`StatisticsServiceImpl`) và truy vấn JPA tới SQL Server.
  - `entity/`, `dto/`, `exception/`: Ánh xạ 10 bảng CSDL + 3 enum trạng thái/thanh toán, kiểm tra dữ liệu form/API và xử lý lỗi tập trung (`403`, `404`, lỗi nghiệp vụ).
  - `socket/SocketServer.java`: Lắng nghe TCP cổng `9090`, dùng thread pool xử lý đồng thời nhiều kết nối từ `laptop-socket-client` qua các repository dùng chung.
  - `src/test/`: Bộ kiểm thử JUnit 5 chạy độc lập bằng H2 (`MODE=MSSQLServer`) kiểm chứng đa luồng, bảo mật và mạng Socket.
- **Module 2 – `laptop-socket-client`**: Chương trình Java thuần (Maven module tách biệt khỏi Spring Web) kết nối TCP tới `localhost:9090` để tra cứu danh mục, thương hiệu và laptop qua CLI (`LaptopClientApp`) hoặc mô phỏng 3 client chạy đồng thời (`MultiClientDemo`).

---

## 4. Hướng dẫn cài đặt & tạo Cơ sở dữ liệu (SQL Server)

1. Mở **SQL Server Configuration Manager** → vào *SQL Server Network Configuration* → bật **TCP/IP** (cổng mặc định `1433`) và khởi động lại dịch vụ SQL Server.
2. Đảm bảo tài khoản `sa` đã được bật (**SQL Server and Windows Authentication mode**).
3. Mở **SQL Server Management Studio (SSMS)** → chọn *File → Open → File...* → mở `database/QL_LAPTOP.sql`.
4. Nhấn **Execute (F5)** để chạy toàn bộ script.
   - Script tự động tạo database `QL_Laptop` gồm 10 bảng tiếng Việt: `VaiTro`, `TaiKhoan`, `DanhMucLaptop`, `ThuongHieu`, `Laptop`, `KhachHang`, `DonHang`, `ChiTietDonHang`, `ThanhToan`, `LichSuTrangThaiDonHang`.
   - Chèn sẵn 3 vai trò, 5 tài khoản mẫu và **20 bản ghi mẫu** cho mỗi bảng chính (`DanhMucLaptop`, `ThuongHieu`, `Laptop`, `KhachHang`, `DonHang`, `ChiTietDonHang`, `ThanhToan`, `LichSuTrangThaiDonHang`).

---

## 5. Cấu hình kết nối trên máy cá nhân

Mở file `laptop-shop-web/src/main/resources/application.properties` (hoặc copy từ `application.properties.example`) và điền thông tin kết nối SQL Server trên máy bạn:

```properties
spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=QL_Laptop;encrypt=true;trustServerCertificate=true}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD:YOUR_SQL_SERVER_PASSWORD}
```

> Thay `YOUR_SQL_SERVER_PASSWORD` bằng mật khẩu `sa` trên máy bạn (hoặc truyền qua biến môi trường `DB_PASSWORD`).

---

## 6. Hướng dẫn chạy Server (Ứng dụng Web + Socket Server)

### Cách 1: Chạy bằng IntelliJ IDEA (Khuyên dùng)
1. Mở IntelliJ IDEA → **File → Open** → chọn thư mục gốc `QuanLyBanLaptop` (IntelliJ sẽ tự nhận diện dự án Maven gồm 2 module).
2. Chọn **Project SDK = JDK 17** (*File → Project Structure → Project → SDK*).
3. Chờ Maven tải thư viện ở góc dưới bên phải.
4. Mở file `laptop-shop-web/src/main/java/com/laptopshop/LaptopShopApplication.java` → nhấn **Run** (`▶`).
5. Khi khởi động thành công:
   - **Website bán hàng & Quản trị**: http://localhost:8080
   - **TCP Socket Server**: tự động lắng nghe tại cổng **9090**

### Cách 2: Chạy bằng dòng lệnh Maven
```bash
cd laptop-shop-web
mvn spring-boot:run
```

### Tài khoản đăng nhập mẫu (Mật khẩu chung: `123456`)
| Vai trò | Tên đăng nhập | Quyền hạn |
|---------|---------------|-----------|
| **ADMIN** | `admin` | Toàn quyền: Laptop, Danh mục, Thương hiệu, Khách hàng, Đơn hàng, Thống kê, Đa luồng, **Quản lý Tài khoản** |
| **STAFF** | `staff01` | Quản lý Laptop, Danh mục, Thương hiệu, Khách hàng, Đơn hàng, Thống kê, Đa luồng (không vào được Quản lý Tài khoản) |
| **CUSTOMER** | `khach01` | Mua hàng, giỏ hàng, đặt hàng, xem lịch sử & hủy đơn hàng chờ xử lý |

---

## 7. Hướng dẫn chạy chương trình Socket Client tách biệt (Thành viên 2)

> **Lưu ý:** Phải chạy `LaptopShopApplication` (Server) trước để cổng `9090` mở.

### 7.1. Chạy 1 Client tương tác qua menu CLI
- Trong IntelliJ IDEA: mở `laptop-socket-client/src/main/java/com/laptopshop/client/LaptopClientApp.java` → nhấn **Run**.
- Hoặc chạy bằng dòng lệnh:
```bash
cd laptop-socket-client
mvn compile
java -cp target/classes com.laptopshop.client.LaptopClientApp localhost 9090
```
Các lệnh hỗ trợ: `PING`, `LIST_CATEGORIES`, `LIST_BRANDS`, `LIST_LAPTOPS`, `SEARCH|<từ khóa>`, `GET|<id>`, `QUIT`.

### 7.2. Chạy Demo 3 Client kết nối đồng thời
- Trong IntelliJ IDEA: mở `laptop-socket-client/src/main/java/com/laptopshop/client/MultiClientDemo.java` → nhấn **Run**.
- Hoặc dòng lệnh:
```bash
java -cp target/classes com.laptopshop.client.MultiClientDemo localhost 9090
```

---

## 8. Hướng dẫn chạy Kiểm thử tự động (JUnit 5 + Đa luồng + Phân quyền)

Bộ kiểm thử sử dụng H2 in-memory chế độ tương thích SQL Server (`MODE=MSSQLServer`) nên chạy được độc lập trên mọi máy:

```bash
mvn test
```

Gồm 2 bộ kiểm thử chính:
1. `ConcurrencyOrderTest` (TV4): kiểm thử 10 luồng đặt hàng đồng thời tranh 5 laptop (chỉ 5 đơn thành công, tồn kho = 0, không âm kho) và kiểm thử hủy đơn hoàn kho + thống kê doanh thu.
2. `SystemIntegrationTest` (TV5): kiểm thử đăng ký & BCrypt, phân quyền Spring Security (`ADMIN` / `STAFF` / `CUSTOMER`), ràng buộc khóa ngoại & trùng mã, cập nhật tồn kho đa luồng (`updateStockSafe`), và `SocketServer` phục vụ đồng thời 3 client TCP.
