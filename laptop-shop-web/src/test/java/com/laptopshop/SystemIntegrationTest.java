package com.laptopshop;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Kiểm thử tổng thể toàn hệ thống: Phân quyền (Spring Security), Tài khoản (BCrypt), Ràng buộc dữ liệu (CRUD),
// Đa luồng tồn kho (TV2) và Giao tiếp Socket Server với 3 Client đồng thời (TV2).

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.WebConfig;
import com.laptopshop.dto.*;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.*;
import com.laptopshop.service.*;
import com.laptopshop.socket.SocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SystemIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountService accountService;
    @Autowired private CategoryService categoryService;
    @Autowired private BrandService brandService;
    @Autowired private LaptopService laptopService;
    @Autowired private CustomerService customerService;
    @Autowired private OrderService orderService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private RoleRepository roleRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private LaptopRepository laptopRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private ImportReceiptRepository importReceiptRepository;
    @Autowired private ImportReceiptService importReceiptService;
    @Autowired private QrCodeService qrCodeService;

    private Integer categoryId;
    private Integer brandId;
    private Integer laptopId;
    private Integer customerId;

    @BeforeEach
    void initData() {
        historyRepository.deleteAll();
        paymentRepository.deleteAll();
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
        importReceiptRepository.deleteAll();
        customerRepository.deleteAll();
        accountRepository.deleteAll();
        laptopRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();

        CategoryDTO catDto = new CategoryDTO();
        catDto.setName("Laptop Văn Phòng");
        catDto.setDescription("Nhẹ, pin lâu");
        categoryId = categoryService.createCategory(catDto).getId();

        BrandDTO brandDto = new BrandDTO();
        brandDto.setName("Dell");
        brandDto.setCountry("Mỹ");
        brandId = brandService.createBrand(brandDto).getId();

        LaptopDTO laptopDto = new LaptopDTO();
        laptopDto.setSku("DELL-TEST-01");
        laptopDto.setName("Dell XPS 13");
        laptopDto.setCategoryId(categoryId);
        laptopDto.setBrandId(brandId);
        laptopDto.setCpu("Intel Core i7");
        laptopDto.setRamGb(16);
        laptopDto.setStorageGb(512);
        laptopDto.setPurchasePrice(new BigDecimal("25000000"));
        laptopDto.setSalePrice(new BigDecimal("30000000"));
        laptopDto.setStockQuantity(10);
        laptopId = laptopService.createLaptop(laptopDto).getId();

        CustomerDTO cusDto = new CustomerDTO(null, "Trần Văn Test", "test@gmail.com", "0909123456", "TP.HCM");
        customerId = customerService.createCustomer(cusDto).getId();
    }

    @Test
    @DisplayName("TV5: Kiểm thử đăng ký khách hàng mới, mã hóa BCrypt và kiểm tra trùng username")
    void testRegisterAndAccountSecurity() {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("khach_test");
        reg.setPassword("123456");
        reg.setConfirmPassword("123456");
        reg.setFullName("Khách Hàng Test");
        reg.setEmail("khachtest@gmail.com");
        reg.setPhone("0988777666");
        reg.setAddress("Quận 1, TP.HCM");

        Account created = accountService.registerCustomer(reg);
        assertNotNull(created.getId());
        assertEquals("CUSTOMER", created.getRole().getRoleName());
        assertTrue(passwordEncoder.matches("123456", created.getPasswordHash()));
        assertTrue(customerRepository.findByAccountId(created.getId()).isPresent());

        assertThrows(BusinessException.class, () -> accountService.registerCustomer(reg),
                "Không được phép đăng ký trùng username");
    }

    @Test
    @DisplayName("TV5: Kiểm thử phân quyền URL theo vai trò (ADMIN, STAFF, CUSTOMER, Anonymous)")
    @WithMockUser(username = "staff01", roles = {"STAFF"})
    void testStaffAccessControl() throws Exception {
        mockMvc.perform(get("/admin/laptops")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/laptops/add")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/laptops/edit/" + laptopId)).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/laptops/delete/" + laptopId).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/categories/add")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/brands/add")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/import-receipts/1/approve").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/accounts")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TV5: Kiểm thử quyền ADMIN truy cập được mọi trang quản trị kể cả Tài khoản")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminAccessControl() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/accounts")).andExpect(status().isOk());
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TV2 & TV4: Kiểm thử ràng buộc dữ liệu (trùng SKU, cấm xóa bản ghi đang được tham chiếu)")
    void testReferentialIntegrityConstraints() {
        LaptopDTO dupSku = new LaptopDTO();
        dupSku.setSku("DELL-TEST-01");
        dupSku.setName("Dell Duplicate");
        dupSku.setCategoryId(categoryId);
        dupSku.setBrandId(brandId);
        dupSku.setSalePrice(new BigDecimal("15000000"));
        dupSku.setStockQuantity(5);
        assertThrows(BusinessException.class, () -> laptopService.createLaptop(dupSku));

        assertThrows(BusinessException.class, () -> categoryService.deleteCategory(categoryId));
        assertThrows(BusinessException.class, () -> brandService.deleteBrand(brandId));

        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(customerId);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setDeliveryAddress("TP.HCM");
        req.setNote("Test FK");
        req.setItems(List.of(new OrderItemRequest(laptopId, 1)));
        orderService.placeOrder(req);

        List<Order> customerOrders = orderService.getOrdersByCustomerId(customerId);
        assertFalse(customerOrders.isEmpty());
        assertFalse(customerOrders.get(0).getDetails().isEmpty());
        assertFalse(customerOrders.get(0).getPayments().isEmpty());

        assertThrows(BusinessException.class, () -> laptopService.deleteLaptop(laptopId),
                "Không được xóa Laptop đã nằm trong OrderDetails");
        assertThrows(BusinessException.class, () -> customerService.deleteCustomer(customerId),
                "Không được xóa Customer đã có đơn hàng");
    }

    @Test
    @DisplayName("TV2: Kiểm thử đa luồng cập nhật tồn kho (10 luồng cộng cùng lúc)")
    void testConcurrentStockUpdate() throws InterruptedException {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    laptopService.updateStockSafe(laptopId, 2);
                    ok.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();

        assertEquals(threads, ok.get());
        assertEquals(30, laptopRepository.findById(laptopId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("TV2: Kiểm thử thông số phần cứng thực tế (từ chối RAM 9GB, SSD 513GB, Giá bán > Giá niêm yết)")
    void testRejectUnrealHardwareSpecsAndPrice() {
        LaptopDTO badRam = new LaptopDTO();
        badRam.setSku("TEST-SPEC-1");
        badRam.setName("Laptop Invalid RAM");
        badRam.setCategoryId(categoryId);
        badRam.setBrandId(brandId);
        badRam.setRamGb(9);
        badRam.setStorageGb(512);
        badRam.setSalePrice(new BigDecimal("15000000"));
        assertThrows(BusinessException.class, () -> laptopService.createLaptop(badRam));

        LaptopDTO badSsd = new LaptopDTO();
        badSsd.setSku("TEST-SPEC-2");
        badSsd.setName("Laptop Invalid SSD");
        badSsd.setCategoryId(categoryId);
        badSsd.setBrandId(brandId);
        badSsd.setRamGb(16);
        badSsd.setStorageGb(513);
        badSsd.setSalePrice(new BigDecimal("15000000"));
        assertThrows(BusinessException.class, () -> laptopService.createLaptop(badSsd));

        LaptopDTO badPrice = new LaptopDTO();
        badPrice.setSku("TEST-SPEC-3");
        badPrice.setName("Laptop Invalid Discount Price");
        badPrice.setCategoryId(categoryId);
        badPrice.setBrandId(brandId);
        badPrice.setRamGb(16);
        badPrice.setStorageGb(512);
        badPrice.setListPrice(new BigDecimal("14000000"));
        badPrice.setSalePrice(new BigDecimal("15000000"));
        assertThrows(BusinessException.class, () -> laptopService.createLaptop(badPrice));
    }

    @Test
    @DisplayName("TV2: Kiểm thử Socket Server phục vụ đồng thời 3 Client qua TCP Socket")
    void testSocketServerWithThreeConcurrentClients() throws Exception {
        int freePort;
        try (ServerSocket tmp = new ServerSocket(0)) {
            freePort = tmp.getLocalPort();
        }
        SocketServer server = new SocketServer(laptopRepository, categoryRepository, brandRepository);
        var portField = SocketServer.class.getDeclaredField("port");
        portField.setAccessible(true);
        portField.setInt(server, freePort);

        Thread serverThread = new Thread(server, "Test-SocketServer");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);
        AtomicInteger successClients = new AtomicInteger(0);

        String[] cmds = {"PING", "SEARCH|Dell", "LIST_CATEGORIES"};
        for (int i = 0; i < 3; i++) {
            final String cmd = cmds[i];
            pool.submit(() -> {
                try {
                    start.await();
                    try (Socket s = new Socket("localhost", freePort);
                         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)), true)) {
                        s.setSoTimeout(5000);
                        out.println(cmd);
                        String statusLine = in.readLine();
                        assertEquals("OK", statusLine);
                        String line;
                        while ((line = in.readLine()) != null) {
                            if ("END".equals(line)) break;
                        }
                        out.println("QUIT");
                        successClients.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();
        server.stop();

        assertEquals(3, successClients.get(), "Cả 3 Socket Client đồng thời phải giao tiếp thành công với Server");
    }

    @Test
    @DisplayName("TV2: Kiểm thử upload bắt buộc 5 ảnh khi thêm mới Laptop và ẩn URL / giữ ảnh khi cập nhật")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUploadFiveLaptopImagesAndKeepImagesOnEdit() throws Exception {
        String testSku = "TEST-UP-999";
        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        BufferedImage dummyImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(dummyImg, "jpg", imgBaos);
        byte[] jpgBytes = imgBaos.toByteArray();

        MockMultipartFile[] onlyFour = new MockMultipartFile[4];
        for (int i = 0; i < 4; i++) {
            onlyFour[i] = new MockMultipartFile("imageFiles", "pic" + (i + 1) + ".jpg", "image/jpeg", jpgBytes);
        }
        LaptopDTO invalidDto = new LaptopDTO();
        invalidDto.setSku(testSku);
        invalidDto.setName("Laptop Thieu Anh");
        invalidDto.setCategoryId(categoryId);
        invalidDto.setBrandId(brandId);
        invalidDto.setSalePrice(new BigDecimal("18000000"));
        assertThrows(BusinessException.class, () -> laptopService.createLaptopWithImages(invalidDto, onlyFour));

        MockMultipartFile badExt = new MockMultipartFile("imageFiles", "virus.txt", "text/plain", "bad".getBytes());
        assertThrows(BusinessException.class, () -> laptopService.saveUploadedLaptopImages(testSku, new MockMultipartFile[]{
                onlyFour[0], onlyFour[1], onlyFour[2], onlyFour[3], badExt
        }));

        Path srcUploadDir = WebConfig.resolveLaptopImagesBaseDir().resolve(testSku);
        Path targetBase = WebConfig.resolveTargetLaptopImagesBaseDir();
        Path targetUploadDir = targetBase != null ? targetBase.resolve(testSku) : null;
        try {
            mockMvc.perform(multipart("/admin/laptops/add")
                            .file(new MockMultipartFile("imageFiles", "1.jpg", "image/jpeg", jpgBytes))
                            .file(new MockMultipartFile("imageFiles", "2.jpg", "image/jpeg", jpgBytes))
                            .file(new MockMultipartFile("imageFiles", "3.jpg", "image/jpeg", jpgBytes))
                            .file(new MockMultipartFile("imageFiles", "4.jpg", "image/jpeg", jpgBytes))
                            .file(new MockMultipartFile("imageFiles", "5.png", "image/png", jpgBytes))
                            .param("sku", testSku)
                            .param("name", "Dell Upload 5 Images")
                            .param("categoryId", String.valueOf(categoryId))
                            .param("brandId", String.valueOf(brandId))
                            .param("ramGb", "16")
                            .param("storageGb", "512")
                            .param("screenSize", "15.6")
                            .param("purchasePrice", "15000000")
                            .param("listPrice", "20000000")
                            .param("salePrice", "18500000")
                            .param("stockQuantity", "5")
                            .param("isActive", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/laptops"));

            Laptop saved = laptopRepository.findAllWithDetails().stream()
                    .filter(l -> testSku.equals(l.getSku()))
                    .findFirst()
                    .orElseThrow();

            String expectedCsv = "/images/laptops/" + testSku + "/1.jpg,/images/laptops/" + testSku + "/2.jpg,/images/laptops/"
                    + testSku + "/3.jpg,/images/laptops/" + testSku + "/4.jpg,/images/laptops/" + testSku + "/5.jpg";
            assertEquals(expectedCsv, saved.getImageUrl());
            assertEquals(5, laptopService.getGalleryImages(saved).size());

            String editHtml = mockMvc.perform(get("/admin/laptops/edit/" + saved.getId()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(editHtml.contains("name=\"imageUrl\""));

            LaptopDTO updateDto = new LaptopDTO();
            updateDto.setSku(saved.getSku());
            updateDto.setName("Dell Upload 5 Images Updated");
            updateDto.setCategoryId(categoryId);
            updateDto.setBrandId(brandId);
            updateDto.setRamGb(16);
            updateDto.setStorageGb(512);
            updateDto.setScreenSize(new BigDecimal("15.6"));
            updateDto.setPurchasePrice(new BigDecimal("15000000"));
            updateDto.setListPrice(new BigDecimal("20000000"));
            updateDto.setSalePrice(new BigDecimal("17900000"));
            updateDto.setStockQuantity(5);
            updateDto.setIsActive(true);
            updateDto.setImageUrl(null);
            Laptop updated = laptopService.updateLaptop(saved.getId(), updateDto);
            assertEquals(expectedCsv, updated.getImageUrl());
        } finally {
            deleteDirectoryQuietly(srcUploadDir);
            deleteDirectoryQuietly(targetUploadDir);
        }
    }

    @Test
    @DisplayName("Phase 2: Kiểm thử Phiếu nhập kho (Pessimistic Lock cộng tồn) và luồng Chuyển khoản -> Hoàn tiền")
    void testImportReceiptAndBankTransferRefundFlow() {
        Role adminRole = roleRepository.findByRoleNameIgnoreCase("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN", "Admin")));
        Account adminAcc = new Account();
        adminAcc.setUsername("admin_test_p2");
        adminAcc.setPasswordHash(passwordEncoder.encode("123456"));
        adminAcc.setFullName("Admin Test");
        adminAcc.setEmail("adminp2@test.com");
        adminAcc.setRole(adminRole);
        adminAcc = accountRepository.save(adminAcc);

        ImportItemRequest item = new ImportItemRequest();
        item.setLaptopId(laptopId);
        item.setQuantity(4);
        item.setUnitCost(new BigDecimal("20000000"));
        ImportReceiptForm form = new ImportReceiptForm();
        form.setSupplier("Dell Vietnam");
        form.setNote("Test nhập kho");
        form.setItems(List.of(item));

        ImportReceipt created = importReceiptService.createReceipt(form, adminAcc.getId());
        assertEquals(ImportReceiptStatus.PENDING, created.getStatus());
        ImportReceipt approved = importReceiptService.approveReceipt(created.getId(), adminAcc.getId());
        assertEquals(ImportReceiptStatus.APPROVED, approved.getStatus());
        assertEquals(14, laptopRepository.findById(laptopId).orElseThrow().getStockQuantity());
        assertThrows(BusinessException.class, () -> laptopService.deleteLaptop(laptopId));

        Customer cus = customerRepository.findById(customerId).orElseThrow();
        cus.setAccountId(adminAcc.getId());
        customerRepository.save(cus);

        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(customerId);
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        req.setDeliveryAddress("TP.HCM");
        req.setItems(List.of(new OrderItemRequest(laptopId, 2)));
        Order order = orderService.placeOrder(req, adminAcc.getId());
        assertEquals(12, laptopRepository.findById(laptopId).orElseThrow().getStockQuantity());

        assertThrows(BusinessException.class, () -> orderService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED));
        Payment pCreated = orderService.getPaymentByOrderId(order.getId()).orElseThrow();
        String qrToken = qrCodeService.createToken(order.getId(), order.getOrderCode(), pCreated.getTransactionCode());
        orderService.confirmBankTransferByQrToken(order.getId(), qrToken);
        Order confirmed = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(PaymentStatus.PAID, orderService.getPaymentByOrderId(order.getId()).orElseThrow().getPaymentStatus());
        RefundRequestDTO refundDto = new RefundRequestDTO();
        refundDto.setReason("Đổi ý");
        refundDto.setBankName("MB Bank");
        refundDto.setAccountNumber("0123456789");
        refundDto.setAccountHolder("ADMIN TEST");
        orderService.requestRefund(order.getId(), adminAcc.getId(), refundDto);
        Order refunded = orderService.confirmRefund(order.getId(), adminAcc.getId());
        assertEquals(OrderStatus.REFUNDED, refunded.getStatus());
        assertEquals(14, laptopRepository.findById(laptopId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("Hồ sơ cá nhân đồng bộ Customer và chặn thêm khách hàng/tài khoản CUSTOMER ở quản trị")
    void testProfileUpdateAndCustomerCreationGuard() throws Exception {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("khach_profile_test");
        reg.setPassword("123456");
        reg.setConfirmPassword("123456");
        reg.setFullName("Nguyễn Minh Khôi");
        reg.setEmail("minhkhoi@test.com");
        reg.setPhone("0977111222");
        reg.setAddress("Quận 3, TP.HCM");
        Account acc = accountService.registerCustomer(reg);
        Customer cus = customerRepository.findByAccountId(acc.getId()).orElseThrow();
        CustomUserDetails principal = new CustomUserDetails(acc, cus.getId());

        mockMvc.perform(get("/tai-khoan").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh Khôi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quận 3, TP.HCM")));

        // Sai mật khẩu hiện tại: giữ dữ liệu trên form + báo lỗi rõ ràng
        mockMvc.perform(post("/tai-khoan").with(user(principal)).with(csrf())
                        .param("fullName", "Nguyễn Minh Khôi Mới")
                        .param("email", "minhkhoi@test.com")
                        .param("phone", "0977111222")
                        .param("address", "Quận 10, TP.HCM")
                        .param("currentPassword", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quận 10, TP.HCM")));

        // Đúng mật khẩu: cập nhật cả Account lẫn Customer
        mockMvc.perform(post("/tai-khoan").with(user(principal)).with(csrf())
                        .param("fullName", "Nguyễn Minh Khôi Updated")
                        .param("email", "minhkhoi@test.com")
                        .param("phone", "0977111222")
                        .param("address", "Quận 7, TP.HCM")
                        .param("currentPassword", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tai-khoan"));

        Customer refreshedCus = customerRepository.findByAccountId(acc.getId()).orElseThrow();
        assertEquals("Nguyễn Minh Khôi Updated", refreshedCus.getCustomerName());
        assertEquals("Quận 7, TP.HCM", refreshedCus.getAddress());

        // Chặn tạo tài khoản CUSTOMER trong AccountService
        Role customerRole = roleRepository.findByRoleNameIgnoreCase("CUSTOMER").orElseThrow();
        AccountDTO badAcc = new AccountDTO();
        badAcc.setUsername("admin_create_cus");
        badAcc.setPassword("123456");
        badAcc.setFullName("Khách Admin Tạo");
        badAcc.setRoleId(customerRole.getId());
        assertThrows(BusinessException.class, () -> accountService.createAccount(badAcc));
    }

    @Test
    @DisplayName("Chặn thêm khách hàng trên route quản trị, xuất báo cáo doanh thu & trang thông tin công khai")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCustomerGuardReportAndInfoPages() throws Exception {
        mockMvc.perform(get("/admin/customers/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/customers"));

        mockMvc.perform(post("/admin/customers/save").with(csrf())
                        .param("customerName", "Khách Tạo Lậu")
                        .param("phone", "0909888777")
                        .param("address", "TP.HCM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/customers"));

        mockMvc.perform(get("/admin/statistics/report").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NEX")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BC-DT-2026-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Người lập báo cáo")));

        mockMvc.perform(get("/admin/statistics/report.csv").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BAO CAO DOANH THU NAM 2026")));

        mockMvc.perform(get("/thong-tin/gioi-thieu")).andExpect(status().isOk());
        mockMvc.perform(get("/thong-tin/huong-dan-thanh-toan")).andExpect(status().isOk());
        mockMvc.perform(get("/thong-tin/chinh-sach-bao-hanh")).andExpect(status().isOk());
    }

    private void deleteDirectoryQuietly(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }
}
