# Bảng Ánh Xạ Thư Mục & File Theo Phân Công Thành Viên (Dùng Để Commit GitHub)

Dự án `QuanLyBanLaptop` là **một repository chung** trên GitHub cho cả 5 thành viên. Dưới đây là danh sách các thư mục/file tương ứng phần phụ trách chính sau đợt nâng cấp **NEXORA LAPTOP v2**:

## 1. Thành viên 1 – Lư Gia Huy (2001240171 – Nhóm trưởng)
**Nhiệm vụ:** Cơ sở dữ liệu (29 bảng), script nâng cấp idempotent, sơ đồ ERD và tài liệu thiết kế CSDL.
- `.gitignore`
- `database/QL_LAPTOP.sql`
- `database/QL_LAPTOP_upgrade.sql`
- `docs/So_do_ERD_QL_LAPTOP.png`
- `docs/Chuong1_2_ThietKeCSDL.docx`
- `docs/KIEN_TRUC_VA_SO_DO.md`

## 2. Thành viên 2 – Phạm Hữu Hiệu (2001240138)
**Nhiệm vụ:** Quản lý Laptop, Danh mục, Thương hiệu, Nhà cung cấp, Nhập kho, Serial/Bảo hành & Lập trình mạng Socket Server/Client.
- Toàn bộ module `laptop-socket-client/`:
  - `laptop-socket-client/pom.xml`
  - `laptop-socket-client/src/main/java/com/laptopshop/client/{LaptopClientApp,StockMonitorFrame,MultiClientDemo}.java`
- Trong `laptop-shop-web/`:
  - `entity/{Category,Brand,Laptop,Supplier,ImportReceipt,ImportReceiptDetail,ProductSerial,WarrantyClaim,SerialStatus,WarrantyStatus,StockMovement,StockMovementType}.java`
  - `util/VietnameseTextNormalizer.java`
  - `repository/{Category,Brand,Laptop,Supplier,ImportReceipt,ProductSerial,WarrantyClaim,StockMovement}Repository.java`
  - `service/{Category,Brand,Laptop,Supplier,ImportReceipt,Warranty,StockMovement}Service*.java`
  - `controller/{Category,Brand,Laptop,ImportReceipt,Warranty}Controller.java`
  - `socket/SocketServer.java`
  - `templates/admin/{laptops,categories,brands,import-receipts,suppliers,warranties,stock-movements}/`
  - `docs/GIAO_THUC_SOCKET.md`

## 3. Thành viên 3 – Trương Xuân Đạt (2001240077)
**Nhiệm vụ:** Hệ thống thiết kế Light/Dark UI, thư viện cục bộ offline (`static/vendor/`), storefront, giỏ hàng, so sánh, tin tức, chat.
- `static/vendor/**` (Bootstrap, Icons, Chart.js, html2pdf, Google Fonts WOFF2)
- `static/css/{tokens.css,base.css,components.css,pages/storefront.css}`
- `static/js/modules/{theme.js,toast.js,palette.js,effects.js,store-filter.js,sse-admin.js}`
- `static/{manifest.json,sw.js}`
- `entity/{CartItemEntity,WishlistItem,TechArticle,FaqItem,Banner,ChatMessage,ProductReview}.java`
- `service/{Cart,WishlistAndCompare,Review,ContentAndChat}Service.java`
- `controller/{Store,ContentAndSeo,InfoPage}Controller.java`
- `templates/fragments/{head,scripts,ui,header,footer,brand}.html`
- `templates/store/**` & `templates/customer/wishlist.html`

## 4. Thành viên 4 – Nguyễn Tuấn Khang (2001240193)
**Nhiệm vụ:** Khách hàng, Đơn hàng, Checkout (VietQR/COD/Webhook), Mã giảm giá, Vận đơn, Hóa đơn A4 PDF, POS, Thống kê & Xuất báo cáo, Xử lý đồng thời.
- `entity/{Customer,CustomerAddress,Order,OrderDetail,OrderStatus,OrderStatusHistory,Payment,PaymentMethod,PaymentStatus,Coupon,DiscountType,CodeSequence,JobLock}.java`
- `service/{Customer,CustomerAddress,Order,Coupon,QrCode,PaymentGateway,CodeGenerator,JobLock,Statistics}Service*.java`
- `scheduler/OrderTimeoutScheduler.java`
- `controller/{Customer,Order,QrPayment,PaymentWebhook,Statistics,CouponAndSupplier,EngageAndPos}Controller.java`
- `templates/admin/{customers,orders,coupons,pos,statistics,reviews,content}/` & `templates/customer/{my-orders,order-detail}.html`
- `src/test/java/com/laptopshop/ConcurrencyOrderTest.java`

## 5. Thành viên 5 – Vũ Gia Bảo (2001240043)
**Nhiệm vụ:** Bảo mật Spring Security + BCrypt + Lockout + Reset MK, Quản lý tài khoản, Audit Log, Tích hợp cấu hình môi trường, Kiểm thử toàn hệ thống & Tài liệu bảo vệ.
- `pom.xml`, `README.md`, `docs/{AUDIT_REPORT,UPGRADE_LOG,CHECKLIST_RUBRIC,KIEM_THU_THU_CONG,DEMO_15_PHUT,BAO_CAO_KHUNG_5_CHUONG,PHAN_CONG_FILE_THANH_VIEN}.md`
- `laptop-shop-web/{pom.xml,src/main/resources/application*.properties*}`
- `config/{SecurityConfig,CustomUserDetails,CustomUserDetailsService,DataInitializer,ShopProperties,I18nConfig}.java`
- `entity/{Role,Account,PasswordResetToken,AuditLog}.java`
- `service/{Account,AuditLog,Email,NotificationSse}Service*.java`, `util/PaginationUtils.java`
- `controller/{AuthController,AccountController,AdminDashboardController}.java`
- `exception/{BusinessException,ResourceNotFoundException,GlobalExceptionHandler}.java`
- `templates/{fragments/admin-layout,admin/dashboard,admin/accounts/*,admin/audit-logs/*,auth/*,error/*}.html`
- `src/test/java/com/laptopshop/{SystemIntegrationTest,Phase1SecurityAndConcurrencyTest,Phase2FeaturesTest}.java`
