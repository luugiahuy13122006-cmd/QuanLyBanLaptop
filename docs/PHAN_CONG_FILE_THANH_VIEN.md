# Bảng Ánh Xạ Thư Mục & File Theo Phân Công Thành Viên (Dùng Để Commit GitHub)

Dự án `QuanLyBanLaptop` là **một repository chung** trên GitHub cho cả 5 thành viên. Dưới đây là danh sách chính xác các thư mục/file tương ứng với phần việc của từng thành viên để mỗi người commit đúng phần mình phụ trách:

## 1. Thành viên 1 – Lư Gia Huy (2001240171 – Nhóm trưởng)
**Nhiệm vụ:** Cơ sở dữ liệu và phân tích thiết kế.
- `.gitignore`
- `database/QL_LAPTOP.sql`
- `docs/So_do_ERD_QL_LAPTOP.png`
- `docs/Chuong1_2_ThietKeCSDL.docx`

## 2. Thành viên 2 – Phạm Hữu Hiệu (2001240138)
**Nhiệm vụ:** Backend quản lý laptop, danh mục, thương hiệu và lập trình mạng Socket Server/Client.
- Toàn bộ module `laptop-socket-client/`:
  - `laptop-socket-client/pom.xml`
  - `laptop-socket-client/src/main/java/com/laptopshop/client/LaptopClientApp.java`
  - `laptop-socket-client/src/main/java/com/laptopshop/client/MultiClientDemo.java`
- Trong `laptop-shop-web/`:
  - `src/main/java/com/laptopshop/entity/Category.java`, `Brand.java`, `Laptop.java`
  - `src/main/java/com/laptopshop/dto/CategoryDTO.java`, `BrandDTO.java`, `LaptopDTO.java`
  - `src/main/java/com/laptopshop/repository/CategoryRepository.java`, `BrandRepository.java`, `LaptopRepository.java`
  - `src/main/java/com/laptopshop/service/CategoryService.java`, `BrandService.java`, `LaptopService.java`
  - `src/main/java/com/laptopshop/controller/CategoryController.java`, `BrandController.java`, `LaptopController.java`, `StockController.java`
  - `src/main/java/com/laptopshop/socket/SocketServer.java`
  - `src/main/resources/templates/admin/laptops/` (`list.html`, `form.html`)
  - `src/main/resources/templates/admin/categories/` (`list.html`, `form.html`)
  - `src/main/resources/templates/admin/brands/` (`list.html`, `form.html`)
  - `src/main/resources/templates/admin/stock/test.html`

## 3. Thành viên 3 – Trương Xuân Đạt (2001240077)
**Nhiệm vụ:** Giao diện website khách hàng, fragment dùng chung, CSS responsive, giỏ hàng & đặt hàng.
- Trong `laptop-shop-web/`:
  - `src/main/java/com/laptopshop/dto/CartItemDTO.java`, `CheckoutFormDTO.java`
  - `src/main/java/com/laptopshop/controller/StoreController.java`
  - `src/main/resources/static/css/style.css`
  - `src/main/resources/static/images/laptop-default.svg`
  - `src/main/resources/templates/fragments/header.html`, `footer.html`
  - `src/main/resources/templates/store/` (`index.html`, `detail.html`, `cart.html`, `checkout.html`, `order-success.html`, `my-orders.html`)

## 4. Thành viên 4 – Nguyễn Tuấn Khang (2001240193)
**Nhiệm vụ:** Backend khách hàng, đơn hàng, thanh toán, thống kê doanh thu và xử lý đồng thời (Concurrency).
- Trong `laptop-shop-web/`:
  - `src/main/java/com/laptopshop/entity/Customer.java`, `Order.java`, `OrderDetail.java`, `OrderStatus.java`, `OrderStatusHistory.java`, `Payment.java`, `PaymentMethod.java`, `PaymentStatus.java`
  - `src/main/java/com/laptopshop/dto/CustomerDTO.java`, `OrderItemRequest.java`, `PlaceOrderRequest.java`, `MonthlyRevenueDTO.java`, `OrderStatusCountDTO.java`, `TopSellingLaptopDTO.java`
  - `src/main/java/com/laptopshop/repository/CustomerRepository.java`, `OrderRepository.java`, `OrderDetailRepository.java`, `OrderStatusHistoryRepository.java`, `PaymentRepository.java`
  - `src/main/java/com/laptopshop/service/CustomerService.java`, `OrderService.java`, `StatisticsService.java`
  - `src/main/java/com/laptopshop/service/impl/CustomerServiceImpl.java`, `OrderServiceImpl.java`, `StatisticsServiceImpl.java`
  - `src/main/java/com/laptopshop/controller/CustomerController.java`, `OrderController.java`, `StatisticsController.java`
  - `src/main/resources/templates/admin/customers/` (`list.html`, `form.html`)
  - `src/main/resources/templates/admin/orders/` (`list.html`, `create.html`, `detail.html`)
  - `src/main/resources/templates/admin/statistics/index.html`
  - `src/test/java/com/laptopshop/ConcurrencyOrderTest.java`

## 5. Thành viên 5 – Vũ Gia Bảo (2001240043)
**Nhiệm vụ:** Tài khoản, phân quyền (Spring Security + BCrypt), tích hợp module, xử lý ngoại lệ, kiểm thử tổng thể & README.
- `pom.xml` (root) và `README.md`
- `docs/PHAN_CONG_FILE_THANH_VIEN.md`
- Trong `laptop-shop-web/`:
  - `pom.xml`
  - `src/main/resources/application.properties`
  - `src/main/java/com/laptopshop/LaptopShopApplication.java`
  - `src/main/java/com/laptopshop/config/` (`SecurityConfig.java`, `CustomUserDetails.java`, `CustomUserDetailsService.java`, `DataInitializer.java`)
  - `src/main/java/com/laptopshop/entity/Role.java`, `Account.java`
  - `src/main/java/com/laptopshop/dto/AccountDTO.java`, `RegisterDTO.java`
  - `src/main/java/com/laptopshop/repository/RoleRepository.java`, `AccountRepository.java`
  - `src/main/java/com/laptopshop/service/AccountService.java`, `service/impl/AccountServiceImpl.java`
  - `src/main/java/com/laptopshop/controller/AuthController.java`, `AccountController.java`, `AdminDashboardController.java`
  - `src/main/java/com/laptopshop/exception/` (`BusinessException.java`, `ResourceNotFoundException.java`, `GlobalExceptionHandler.java`)
  - `src/main/resources/templates/fragments/admin-layout.html`
  - `src/main/resources/templates/auth/` (`login.html`, `register.html`, `profile.html`)
  - `src/main/resources/templates/admin/dashboard.html`
  - `src/main/resources/templates/admin/accounts/` (`list.html`, `form.html`)
  - `src/main/resources/templates/error/` (`403.html`, `404.html`, `error.html`)
  - `src/test/resources/application.properties`
  - `src/test/java/com/laptopshop/SystemIntegrationTest.java`
