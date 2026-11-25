package com.laptopshop;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.*;
import com.laptopshop.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2FeaturesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private BrandRepository brandRepo;
    @Autowired private LaptopRepository laptopRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private CouponRepository couponRepo;
    @Autowired private ProductSerialRepository serialRepo;
    @Autowired private WarrantyClaimRepository claimRepo;
    @Autowired private ProductReviewRepository reviewRepo;
    @Autowired private WishlistRepository wishlistRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private CustomerAddressRepository addressRepo;
    @Autowired private SupplierRepository supplierRepo;
    @Autowired private ImportReceiptRepository importReceiptRepo;
    @Autowired private StaffPermissionRepository staffPermRepo;
    @Autowired private BannerRepository bannerRepo;
    @Autowired private ChatMessageRepository chatRepo;
    @Autowired private AuditLogRepository auditLogRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private CartService cartService;
    @Autowired private CouponService couponService;
    @Autowired private OrderService orderService;
    @Autowired private WarrantyService warrantyService;
    @Autowired private ReviewService reviewService;
    @Autowired private WishlistAndCompareService wishlistAndCompareService;
    @Autowired private CustomerAddressService addressService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private SupplierService supplierService;
    @Autowired private StaffPermissionService staffPermissionService;
    @Autowired private ImportReceiptService importReceiptService;
    @Autowired private QrCodeService qrCodeService;

    private Integer laptopId;
    private Integer customerId;
    private Account customerAccount;
    private CustomUserDetails customerPrincipal;

    @BeforeEach
    void init() {
        claimRepo.deleteAll();
        serialRepo.deleteAll();
        reviewRepo.deleteAll();
        wishlistRepo.deleteAll();
        cartItemRepo.deleteAll();
        addressRepo.deleteAll();
        importReceiptRepo.deleteAll();
        staffPermRepo.deleteAll();
        bannerRepo.deleteAll();
        chatRepo.deleteAll();
        orderRepo.deleteAll();
        couponRepo.deleteAll();
        supplierRepo.deleteAll();
        customerRepo.deleteAll();
        accountRepo.deleteAll();
        laptopRepo.deleteAll();
        categoryRepo.deleteAll();
        brandRepo.deleteAll();
        auditLogRepo.deleteAll();

        Category cat = categoryRepo.save(new Category("Ultrabook", "Mỏng nhẹ"));
        Brand brand = brandRepo.save(new Brand("ASUS", "Đài Loan", "Hãng"));

        Laptop l = new Laptop();
        l.setSku("P2-LAP-01");
        l.setName("ASUS Zenbook AI P2");
        l.setCategory(cat);
        l.setBrand(brand);
        l.setCpu("Intel Core Ultra 7");
        l.setRamGb(16);
        l.setStorageGb(1024);
        l.setPurchasePrice(new BigDecimal("18000000"));
        l.setListPrice(new BigDecimal("25000000"));
        l.setSalePrice(new BigDecimal("22000000"));
        l.setStockQuantity(10);
        l.setWarrantyMonths(24);
        l.setIsActive(true);
        laptopId = laptopRepo.save(l).getId();

        Role cusRole = roleRepo.findByRoleNameIgnoreCase("CUSTOMER").orElseThrow();
        customerAccount = new Account();
        customerAccount.setUsername("p2_khach");
        customerAccount.setPasswordHash(passwordEncoder.encode("Pass1234"));
        customerAccount.setFullName("Khách P2");
        customerAccount.setEmail("p2@nexora.vn");
        customerAccount.setPhone("0988111222");
        customerAccount.setRole(cusRole);
        customerAccount = accountRepo.save(customerAccount);

        Customer cus = new Customer("Khách P2", "p2@nexora.vn", "0988111222", "Quận 1");
        cus.setAccountId(customerAccount.getId());
        customerId = customerRepo.save(cus).getId();
        customerPrincipal = new CustomUserDetails(customerAccount, customerId);
    }

    @Test
    @DisplayName("P2-Cart & Address: gộp giỏ session vào DB khi đăng nhập + sổ địa chỉ")
    void testDbCartMergeAndAddresses() {
        MockHttpSession session = new MockHttpSession();
        cartService.addItem(laptopId, 2, null, session);
        assertEquals(2, cartService.getCartCount(null, session));

        cartService.mergeSessionCartToDb(customerAccount.getId(), session);
        assertEquals(2, cartService.getCartCount(customerAccount.getId(), new MockHttpSession()));

        addressService.addAddress(customerId, "Khách P2", "0988111222", "TP.HCM", "Q1", "Bến Nghé", "12 Lê Lợi", true);
        assertEquals(1, addressService.getAddressesForCustomer(customerId).size());
    }

    @Test
    @DisplayName("P2-Coupon, Serial, Warranty, Review, Analytics: đủ vòng đời coupon -> serial -> bảo hành -> đánh giá -> lợi nhuận")
    void testCouponSerialWarrantyReviewAnalyticsFlow() throws Exception {
        Coupon coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setDiscountType(DiscountType.PERCENT);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setMaxDiscountAmount(new BigDecimal("2000000"));
        coupon.setMinOrderAmount(new BigDecimal("10000000"));
        coupon.setMaxUsage(5);
        couponService.saveCoupon(coupon);

        // Chưa mua không được đánh giá
        assertThrows(BusinessException.class,
                () -> reviewService.submitReview(laptopId, customerAccount.getId(), 5, "Chưa mua thử gửi", false));

        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(customerId);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setCouponCode("SAVE10");
        req.setShippingFee(BigDecimal.ZERO);
        req.setItems(List.of(new OrderItemRequest(laptopId, 1)));
        Order order = orderService.placeOrder(req, customerAccount.getId());

        // Giảm 10% trên 22M = 2.2M, nhưng cap 2M => còn 20M
        assertEquals(0, new BigDecimal("2000000").compareTo(order.getDiscount()));
        assertEquals(0, new BigDecimal("20000000").compareTo(order.getTotalAmount()));
        assertEquals(1, couponRepo.findByCodeIgnoreCase("SAVE10").orElseThrow().getUsedCount());

        // Hoàn thành đơn -> cấp phát Serial + bảo hành 24 tháng
        orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED, null);
        List<ProductSerial> serials = warrantyService.lookupWarranty(order.getOrderCode());
        assertEquals(1, serials.size());
        String sn = serials.get(0).getSerialNumber();
        assertTrue(serials.get(0).isUnderWarranty());

        // Gửi yêu cầu bảo hành công khai
        WarrantyClaim claim = warrantyService.submitClaim(sn, "Khách P2", "0988111222", "Loa rè nhẹ");
        assertEquals(WarrantyStatus.RECEIVED, claim.getStatus());

        // Đã hoàn thành đơn -> gửi đánh giá ẩn danh thành công -> duyệt -> kiểm tra ẩn tên K****** & chi tiết SP không lộ SKU/Tồn kho
        ProductReview rev = reviewService.submitReview(laptopId, customerAccount.getId(), 5, "Máy rất mượt", true);
        reviewService.moderate(rev.getId(), true, "Cảm ơn bạn!");
        assertEquals(5.0, reviewService.getAverageRating(laptopId));
        assertEquals("K******", rev.getReviewerDisplayName());

        mockMvc.perform(get("/san-pham/" + laptopId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SKU:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tồn kho:"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("K******")));

        // Doanh thu 20M, giá vốn 18M => lợi nhuận gộp = 2M
        assertEquals(0, new BigDecimal("2000000").compareTo(statisticsService.getTotalGrossProfit()));

        // Kiểm tra API lọc không reload & coupon AJAX
        mockMvc.perform(get("/api/store/filter").param("keyword", "zenbook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/coupons/check").with(user(customerPrincipal))
                        .param("code", "SAVE10").param("subtotal", "22000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("P2-Wishlist, Compare, Supplier")
    void testWishlistCompareAndSupplier() {
        assertTrue(wishlistAndCompareService.toggleWishlist(customerAccount.getId(), laptopId));
        assertFalse(wishlistAndCompareService.toggleWishlist(customerAccount.getId(), laptopId));

        MockHttpSession session = new MockHttpSession();
        wishlistAndCompareService.addToCompare(laptopId, session);
        assertEquals(1, wishlistAndCompareService.getCompareLaptops(session).size());

        Supplier sup = new Supplier();
        sup.setName("DGW Partner");
        sup.setPhone("0909000999");
        Supplier savedSup = supplierService.save(sup);
        assertNotNull(savedSup.getId());
    }

    @Test
    @DisplayName("P3-UI & Vendor Offline: kiểm tra toàn bộ template storefront, customer, admin Phase 1-3 và asset cục bộ render 200 OK")
    void testAllUpdatedTemplatesAndOfflineVendorAssetsRenderCleanly() throws Exception {
        Role adminRole = roleRepo.findByRoleNameIgnoreCase("ADMIN").orElseThrow();
        Account adminAcc = new Account();
        adminAcc.setUsername("p3_admin");
        adminAcc.setPasswordHash(passwordEncoder.encode("Admin1234"));
        adminAcc.setFullName("Quản trị P3");
        adminAcc.setEmail("adminp3@nexora.vn");
        adminAcc.setRole(adminRole);
        adminAcc = accountRepo.save(adminAcc);
        CustomUserDetails adminPrincipal = new CustomUserDetails(adminAcc, null);

        // Kiểm tra banner trang chủ + chat chưa đọc tại /admin/content
        Banner b1 = new Banner();
        b1.setTitle("Laptop AI 2026 — Chuẩn Sức Mạnh Mới");
        b1.setSubtitle("Giảm đến 20%");
        b1.setTargetUrl("/?categoryId=10");
        b1.setCtaLabel("Khám phá Laptop AI");
        b1.setSortOrder(1);
        bannerRepo.save(b1);
        Banner b2 = new Banner();
        b2.setTitle("Đấu Trường Gaming RTX 4060");
        b2.setSubtitle("Freeship toàn quốc");
        b2.setTargetUrl("/?categoryId=2");
        b2.setCtaLabel("Xem Laptop Gaming");
        b2.setSortOrder(2);
        bannerRepo.save(b2);
        chatRepo.save(new ChatMessage("ROOM-TEST", "Khách A", "CUSTOMER", "Xin tư vấn"));
        chatRepo.save(new ChatMessage("ROOM-UNREAD", "Khách B", "CUSTOMER", "Còn hàng không shop?"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Laptop AI 2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đấu Trường Gaming RTX 4060")));

        // Public & SEO & Offline Vendor routes
        for (String publicUrl : List.of(
                "/", "/san-pham/" + laptopId, "/bao-hanh", "/ho-tro",
                "/thong-tin/gioi-thieu", "/login", "/register",
                "/robots.txt", "/sitemap.xml", "/manifest.json", "/sw.js",
                "/vendor/bootstrap/bootstrap.min.css", "/vendor/bootstrap/bootstrap.bundle.min.js",
                "/vendor/bootstrap-icons/bootstrap-icons.min.css", "/vendor/chartjs/chart.umd.min.js",
                "/vendor/html2pdf/html2pdf.bundle.min.js", "/vendor/fonts/fonts.css",
                "/css/tokens.css", "/css/base.css", "/css/components.css", "/css/pages/storefront.css",
                "/js/modules/theme.js", "/js/modules/toast.js", "/js/modules/palette.js"
        )) {
            mockMvc.perform(get(publicUrl))
                    .andExpect(status().isOk());
        }

        // Customer routes (add 1 item to DB cart so /dat-hang renders checkout page instead of redirecting to empty cart)
        cartService.addItem(laptopId, 1, customerAccount.getId(), new MockHttpSession());
        for (String cusUrl : List.of("/gio-hang", "/so-sanh", "/dat-hang", "/tai-khoan", "/tai-khoan/dia-chi", "/yeu-thich", "/don-hang-cua-toi")) {
            mockMvc.perform(get(cusUrl).with(user(customerPrincipal)))
                    .andExpect(status().isOk());
        }

        // Admin routes (removed pos, stock-movements, pending-proofs)
        for (String adminUrl : List.of(
                "/admin/dashboard", "/admin/statistics", "/admin/statistics/report",
                "/admin/laptops", "/admin/laptops/add", "/admin/laptops/edit/" + laptopId,
                "/admin/categories", "/admin/categories/add", "/admin/brands", "/admin/brands/add",
                "/admin/import-receipts", "/admin/import-receipts/create",
                "/admin/suppliers", "/admin/orders", "/admin/orders/create",
                "/admin/customers", "/admin/coupons",
                "/admin/warranties", "/admin/reviews", "/admin/content", "/admin/accounts", "/admin/stock/test", "/admin/audit-logs"
        )) {
            mockMvc.perform(get(adminUrl).with(user(adminPrincipal)))
                    .andExpect(status().isOk());
        }

        // Phân quyền STAFF: chặn coupons/suppliers & thực thi ma trận quyền động
        Role staffRole = roleRepo.findByRoleNameIgnoreCase("STAFF").orElseThrow();
        Account staffAcc = new Account();
        staffAcc.setUsername("p3_staff");
        staffAcc.setPasswordHash(passwordEncoder.encode("Staff1234"));
        staffAcc.setFullName("Nhân viên P3");
        staffAcc.setEmail("staffp3@nexora.vn");
        staffAcc.setRole(staffRole);
        staffAcc = accountRepo.save(staffAcc);
        CustomUserDetails staffPrincipal = new CustomUserDetails(staffAcc, null);

        java.util.Map<AdminModule, PermissionLevel> matrix = new java.util.EnumMap<>(AdminModule.class);
        matrix.put(AdminModule.STATISTICS, PermissionLevel.NONE);
        matrix.put(AdminModule.LAPTOPS, PermissionLevel.VIEW);
        matrix.put(AdminModule.ORDERS, PermissionLevel.EDIT);
        matrix.put(AdminModule.IMPORT_RECEIPTS, PermissionLevel.EDIT);
        staffPermissionService.savePermissionsForStaff(staffAcc.getId(), matrix);

        mockMvc.perform(get("/admin/accounts/" + staffAcc.getId() + "/permissions").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("perm_STATISTICS")));

        mockMvc.perform(get("/403")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/stock/test").with(user(staffPrincipal))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/coupons").with(user(staffPrincipal))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/suppliers").with(user(staffPrincipal))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/statistics").with(user(staffPrincipal))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/laptops").with(user(staffPrincipal))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/laptops/add").with(user(staffPrincipal))).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/laptops/delete/" + laptopId).with(user(staffPrincipal)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/orders/create").with(user(staffPrincipal))).andExpect(status().isOk());

        // Nhà cung cấp liên kết Phiếu nhập kho + Toggle & Delete guard
        Supplier sup = new Supplier();
        sup.setName("FPT Synnex Partner");
        sup = supplierService.save(sup);
        com.laptopshop.dto.ImportReceiptForm importForm = new com.laptopshop.dto.ImportReceiptForm();
        importForm.setSupplierId(sup.getId());
        com.laptopshop.dto.ImportItemRequest itemReq = new com.laptopshop.dto.ImportItemRequest();
        itemReq.setLaptopId(laptopId);
        itemReq.setQuantity(3);
        itemReq.setUnitCost(new BigDecimal("18000000"));
        importForm.setItems(List.of(itemReq));
        ImportReceipt createdRcpt = importReceiptService.createReceipt(importForm, staffAcc.getId());
        assertEquals(sup.getName(), createdRcpt.getSupplier());

        Integer supIdToDelete = sup.getId();
        assertThrows(BusinessException.class, () -> supplierService.delete(supIdToDelete));
        Supplier toggled = supplierService.toggleActive(supIdToDelete);
        assertFalse(toggled.getIsActive());

        // Kiểm tra đa luồng trong trang Quản trị (/admin/stock/concurrent-test)
        int stockBefore = laptopRepo.findById(laptopId).orElseThrow().getStockQuantity();
        mockMvc.perform(post("/admin/stock/concurrent-test").with(user(adminPrincipal)).with(csrf())
                        .param("laptopId", String.valueOf(laptopId))
                        .param("threadCount", "5")
                        .param("deltaPerThread", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/stock/test"));
        assertEquals(stockBefore + 10, laptopRepo.findById(laptopId).orElseThrow().getStockQuantity());

        // Kiểm tra luồng quét QR trên điện thoại: xác nhận thanh toán xong vẫn hiển thị trang thành công + nút Quay lại trang chủ
        PlaceOrderRequest qrReq = new PlaceOrderRequest();
        qrReq.setCustomerId(customerId);
        qrReq.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        qrReq.setItems(List.of(new OrderItemRequest(laptopId, 1)));
        Order qrOrder = orderService.placeOrder(qrReq, customerAccount.getId());
        Payment qrPayment = orderService.getPaymentByOrderId(qrOrder.getId()).orElseThrow();
        String qrToken = qrCodeService.createToken(qrOrder.getId(), qrOrder.getOrderCode(),
                qrPayment.getTransactionCode(), qrOrder.getTotalAmount(), qrOrder.getPaymentExpiresAt());

        mockMvc.perform(get("/qr-pay/" + qrOrder.getId() + "/" + qrToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận đã chuyển khoản")));

        mockMvc.perform(post("/qr-pay/" + qrOrder.getId() + "/" + qrToken + "/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/qr-pay/" + qrOrder.getId() + "/" + qrToken));

        // Sau khi thanh toán xong (paymentExpiresAt = null), redirect GET phải hiển thị thành công & nút Quay lại trang chủ, KHÔNG báo lỗi
        mockMvc.perform(get("/qr-pay/" + qrOrder.getId() + "/" + qrToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã chuyển khoản thành công!")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quay lại trang chủ")));
    }
}
