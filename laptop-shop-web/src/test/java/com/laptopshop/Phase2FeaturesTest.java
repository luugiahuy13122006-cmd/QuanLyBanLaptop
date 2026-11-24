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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @Autowired private StockMovementRepository stockMovementRepo;
    @Autowired private SupplierRepository supplierRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;
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
        stockMovementRepo.deleteAll();
        tokenRepo.deleteAll();
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
                () -> reviewService.submitReview(laptopId, customerAccount.getId(), 5, "Tốt", "Chưa mua thử gửi"));

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

        // Đã hoàn thành đơn -> gửi đánh giá thành công -> duyệt -> hiển thị avg rating 5.0
        ProductReview rev = reviewService.submitReview(laptopId, customerAccount.getId(), 5, "Xuất sắc", "Máy rất mượt");
        reviewService.moderate(rev.getId(), true, "Cảm ơn bạn!");
        assertEquals(5.0, reviewService.getAverageRating(laptopId));

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

        // Public & SEO & Offline Vendor routes
        for (String publicUrl : List.of(
                "/", "/san-pham/" + laptopId, "/bao-hanh", "/tin-tuc", "/ho-tro",
                "/thong-tin/gioi-thieu", "/login", "/register", "/forgot-password",
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

        // Admin & Phase 2-3 routes
        for (String adminUrl : List.of(
                "/admin/dashboard", "/admin/pos", "/admin/statistics", "/admin/statistics/report",
                "/admin/laptops", "/admin/laptops/add", "/admin/laptops/edit/" + laptopId,
                "/admin/categories", "/admin/categories/add", "/admin/brands", "/admin/brands/add",
                "/admin/import-receipts", "/admin/import-receipts/create",
                "/admin/suppliers", "/admin/stock-movements", "/admin/orders", "/admin/orders/create",
                "/admin/orders/pending-proofs", "/admin/customers", "/admin/coupons",
                "/admin/warranties", "/admin/reviews", "/admin/content", "/admin/accounts", "/admin/audit-logs"
        )) {
            mockMvc.perform(get(adminUrl).with(user(adminPrincipal)))
                    .andExpect(status().isOk());
        }
    }
}
