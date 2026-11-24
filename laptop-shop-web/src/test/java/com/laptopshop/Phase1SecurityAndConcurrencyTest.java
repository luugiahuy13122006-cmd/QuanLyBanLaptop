package com.laptopshop;

import com.laptopshop.config.CustomUserDetails;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase1SecurityAndConcurrencyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private BrandRepository brandRepo;
    @Autowired private LaptopRepository laptopRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private AuditLogRepository auditLogRepo;
    @Autowired private StaffPermissionRepository staffPermRepo;
    @Autowired private OrderService orderService;
    @Autowired private LaptopService laptopService;
    @Autowired private CodeGeneratorService codeGeneratorService;
    @Autowired private AccountService accountService;
    @Autowired private PaymentGatewayService paymentGatewayService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Integer testLaptopId;
    private Integer customerAId;
    private Integer customerBId;
    private CustomUserDetails customerAPrincipal;
    private CustomUserDetails customerBPrincipal;
    private Account staffAccount;

    @BeforeEach
    void setup() {
        staffPermRepo.deleteAll();
        orderRepo.deleteAll();
        customerRepo.deleteAll();
        accountRepo.deleteAll();
        laptopRepo.deleteAll();
        categoryRepo.deleteAll();
        brandRepo.deleteAll();
        auditLogRepo.deleteAll();

        Category cat = categoryRepo.save(new Category("Laptop Test P1", "Mô tả"));
        Brand brand = brandRepo.save(new Brand("ASUS", "Đài Loan", "Hãng"));

        Laptop l = new Laptop();
        l.setSku("P1-TEST-01");
        l.setName("ASUS Vivobook Tiếng Việt");
        l.setCategory(cat);
        l.setBrand(brand);
        l.setCpu("Intel Core i5-13500H");
        l.setRamGb(16);
        l.setStorageGb(512);
        l.setPurchasePrice(new BigDecimal("14000000"));
        l.setListPrice(new BigDecimal("19000000"));
        l.setSalePrice(new BigDecimal("17000000"));
        l.setStockQuantity(10);
        l.setIsActive(true);
        testLaptopId = laptopRepo.save(l).getId();

        Role cusRole = roleRepo.findByRoleNameIgnoreCase("CUSTOMER").orElseThrow();
        Role staffRole = roleRepo.findByRoleNameIgnoreCase("STAFF").orElseThrow();

        staffAccount = new Account();
        staffAccount.setUsername("staff_p1");
        staffAccount.setPasswordHash(passwordEncoder.encode("Pass1234"));
        staffAccount.setFullName("Nhân Viên P1");
        staffAccount.setEmail("staff_p1@nexora.vn");
        staffAccount.setRole(staffRole);
        staffAccount = accountRepo.save(staffAccount);

        Account accA = createCustomerAccount("khach_a", "a@nexora.vn", "0911000001", cusRole);
        Customer cusA = customerRepo.save(createCustomer(accA.getId(), "Khách A", "a@nexora.vn", "0911000001"));
        customerAId = cusA.getId();
        customerAPrincipal = new CustomUserDetails(accA, cusA.getId());

        Account accB = createCustomerAccount("khach_b", "b@nexora.vn", "0911000002", cusRole);
        Customer cusB = customerRepo.save(createCustomer(accB.getId(), "Khách B", "b@nexora.vn", "0911000002"));
        customerBId = cusB.getId();
        customerBPrincipal = new CustomUserDetails(accB, cusB.getId());
    }

    private Account createCustomerAccount(String username, String email, String phone, Role role) {
        Account a = new Account();
        a.setUsername(username);
        a.setPasswordHash(passwordEncoder.encode("Pass1234"));
        a.setFullName(username);
        a.setEmail(email);
        a.setPhone(phone);
        a.setRole(role);
        return accountRepo.save(a);
    }

    private Customer createCustomer(Integer accountId, String name, String email, String phone) {
        Customer c = new Customer(name, email, phone, "TP.HCM");
        c.setAccountId(accountId);
        return c;
    }

    @Test
    @DisplayName("P1-Concurrency: 20 luồng đặt hàng cùng lúc trên tồn kho 10 -> đúng 10 thành công, 10 thất bại, tồn = 0")
    void test20ThreadsOrderConcurrency() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(20);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < 20; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    PlaceOrderRequest req = new PlaceOrderRequest();
                    req.setCustomerId(customerAId);
                    req.setPaymentMethod(PaymentMethod.CASH);
                    req.setItems(List.of(new OrderItemRequest(testLaptopId, 1)));
                    orderService.placeOrder(req, customerAPrincipal.getAccountId());
                    success.incrementAndGet();
                } catch (Exception ex) {
                    fail.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(10, success.get());
        assertEquals(10, fail.get());
        assertEquals(0, laptopRepo.findById(testLaptopId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("P1-CodeGen: 50 luồng sinh mã đơn hàng đồng thời -> 50 mã duy nhất không trùng lặp")
    void test50ThreadsCodeGeneration() throws Exception {
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        Set<String> codes = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    codes.add(codeGeneratorService.nextOrderCode());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(errors.isEmpty(), "Không được lỗi khi sinh mã đa luồng: " + errors);
        assertEquals(n, codes.size());
    }

    @Test
    @DisplayName("P1-Security: Chặn CUSTOMER gọi /orders/place, chặn IDOR đơn hàng, chặn vượt 5% giảm giá STAFF")
    void testSecurityAndIdorMatrix() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(customerAId);
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        req.setItems(List.of(new OrderItemRequest(testLaptopId, 1)));
        Order orderA = orderService.placeOrder(req, customerAPrincipal.getAccountId());

        // Khách B không được xem hoặc hủy đơn của Khách A (IDOR -> 404/error)
        mockMvc.perform(get("/don-hang-cua-toi/" + orderA.getId()).with(user(customerBPrincipal)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/don-hang-cua-toi/" + orderA.getId() + "/huy")
                        .with(user(customerBPrincipal)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(OrderStatus.PENDING, orderRepo.findById(orderA.getId()).orElseThrow().getStatus());

        // Khách hàng bị chặn 403 trên API đặt đơn quầy /orders/place và /admin/**
        mockMvc.perform(post("/orders/place").with(user(customerAPrincipal)).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/dashboard").with(user(customerAPrincipal)))
                .andExpect(status().isForbidden());

        // STAFF giảm quá 5% tổng đơn bị chặn
        PlaceOrderRequest staffOverDiscount = new PlaceOrderRequest();
        staffOverDiscount.setCustomerId(customerBId);
        staffOverDiscount.setPaymentMethod(PaymentMethod.CASH);
        staffOverDiscount.setDiscount(new BigDecimal("2000000")); // > 5% của 17M
        staffOverDiscount.setItems(List.of(new OrderItemRequest(testLaptopId, 1)));
        assertThrows(BusinessException.class, () -> orderService.placeOrder(staffOverDiscount, staffAccount.getId()));
    }

    @Test
    @DisplayName("P1-Webhook: Xác thực HMAC Webhook, sai chữ ký bị 401, gọi lại cùng gatewayRef được idempotent")
    void testPaymentWebhookHmacAndIdempotency() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(customerAId);
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        req.setItems(List.of(new OrderItemRequest(testLaptopId, 1)));
        Order order = orderService.placeOrder(req, customerAPrincipal.getAccountId());
        Payment payment = order.getLatestPayment();

        String body = String.format(
                "{\"orderCode\":\"%s\",\"transactionCode\":\"%s\",\"amount\":%s,\"gatewayRef\":\"GW-REF-999\"}",
                order.getOrderCode(), payment.getTransactionCode(), order.getTotalAmount().toPlainString());

        // Sai chữ ký -> 401
        mockMvc.perform(post("/api/webhooks/payment")
                        .header("X-Signature", "deadbeef")
                        .contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        // Đúng chữ ký -> 200 & PAID + CONFIRMED
        String sig = paymentGatewayService.computeSignature(
                order.getOrderCode(), payment.getTransactionCode(), order.getTotalAmount(), "GW-REF-999");
        mockMvc.perform(post("/api/webhooks/payment")
                        .header("X-Signature", sig)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        Order updated = orderRepo.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, updated.getStatus());
        assertEquals(PaymentStatus.PAID, paymentRepositoryLatest(order.getId()).getPaymentStatus());

        // Gửi lại cùng gatewayRef -> idempotent
        assertFalse(paymentGatewayService.processBankWebhook(
                order.getOrderCode(), payment.getTransactionCode(), order.getTotalAmount(), "GW-REF-999", sig));
    }

    private Payment paymentRepositoryLatest(Integer orderId) {
        return orderService.getPaymentByOrderId(orderId).orElseThrow();
    }

    @Test
    @DisplayName("P1-Auth: Khóa tài khoản sau 5 lần đăng nhập sai & mở khóa bởi Admin")
    void testBruteForceLock() {
        for (int i = 0; i < 5; i++) {
            accountService.recordFailedLogin("khach_a");
        }
        Account locked = accountRepo.findByUsernameIgnoreCase("khach_a").orElseThrow();
        assertEquals(5, locked.getFailedLoginAttempts());
        assertNotNull(locked.getLockedUntil());
        assertFalse(new CustomUserDetails(locked, customerAId).isAccountNonLocked());

        accountService.resetFailedLogin("a@nexora.vn");
        Account unlocked = accountRepo.findByUsernameIgnoreCase("khach_a").orElseThrow();
        assertEquals(0, unlocked.getFailedLoginAttempts());
        assertNull(unlocked.getLockedUntil());
        assertTrue(new CustomUserDetails(unlocked, customerAId).isAccountNonLocked());
    }

    @Test
    @DisplayName("P1-Upload & Update: Chặn file giả đuôi ảnh & cập nhật laptop không ghi đè tồn kho")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testUploadMagicBytesAndStockImmutability() {
        MultipartFile[] spoofed = new MultipartFile[5];
        for (int i = 0; i < 5; i++) {
            spoofed[i] = new MockMultipartFile("imageFiles", (i + 1) + ".jpg", "image/jpeg",
                    "NOT_A_REAL_JPEG_SCRIPT".getBytes(StandardCharsets.UTF_8));
        }
        assertThrows(BusinessException.class, () -> laptopService.saveUploadedLaptopImages("P1-TEST-01", spoofed));

        Laptop existing = laptopRepo.findById(testLaptopId).orElseThrow();
        LaptopDTO updateDto = new LaptopDTO();
        updateDto.setSku(existing.getSku());
        updateDto.setName(existing.getName() + " Pro");
        updateDto.setCategoryId(existing.getCategory().getId());
        updateDto.setBrandId(existing.getBrand().getId());
        updateDto.setCpu(existing.getCpu());
        updateDto.setRamGb(16);
        updateDto.setStorageGb(512);
        updateDto.setPurchasePrice(existing.getPurchasePrice());
        updateDto.setListPrice(existing.getListPrice());
        updateDto.setSalePrice(existing.getSalePrice());
        updateDto.setStockQuantity(9999); // phải bị bỏ qua khi update thông tin
        updateDto.setIsActive(true);

        Laptop updated = laptopService.updateLaptop(testLaptopId, updateDto);
        assertEquals(10, updated.getStockQuantity());
    }

    @Test
    @DisplayName("P1-Socket: ServerSocket đa luồng AUTH + tìm không dấu + 5 client đồng thời + STOCK_ADJUST")
    void testSocketServerAuthAndMultiClient() throws Exception {
        SocketServer server = new SocketServer(0, "SECRET_TEST_TOKEN",
                laptopRepo, categoryRepo, brandRepo, orderRepo, laptopService, auditLogRepoServiceFallback());
        Thread serverThread = new Thread(server, "test-socket-server");
        serverThread.setDaemon(true);
        serverThread.start();

        long deadline = System.currentTimeMillis() + 3000;
        while (server.getPort() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        int port = server.getPort();
        assertTrue(port > 0);

        try {
            // Chưa AUTH phải trả ERROR UNAUTHORIZED
            try (Socket s = new Socket("127.0.0.1", port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)), true)) {
                out.println("LIST_LAPTOPS");
                assertEquals("ERROR", in.readLine());
                assertTrue(in.readLine().contains("UNAUTHORIZED"));
            }

            // 5 client kết nối đồng thời
            ExecutorService clients = Executors.newFixedThreadPool(5);
            CountDownLatch done = new CountDownLatch(5);
            AtomicInteger okCount = new AtomicInteger();
            for (int i = 0; i < 5; i++) {
                final int idx = i;
                clients.submit(() -> {
                    try (Socket s = new Socket("127.0.0.1", port);
                         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)), true)) {
                        out.println("AUTH|SECRET_TEST_TOKEN");
                        assertEquals("OK", in.readLine());
                        while (!"END".equals(in.readLine())) {}
                        if (idx == 0) {
                            out.println("STOCK_ADJUST|P1-TEST-01|2|Kiểm kê");
                        } else {
                            out.println("SEARCH|tieng viet");
                        }
                        if ("OK".equals(in.readLine())) {
                            okCount.incrementAndGet();
                        }
                        while (!"END".equals(in.readLine())) {}
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(done.await(15, TimeUnit.SECONDS));
            clients.shutdown();
            assertEquals(5, okCount.get());
            assertEquals(12, laptopRepo.findById(testLaptopId).orElseThrow().getStockQuantity());
        } finally {
            server.stop();
        }
    }

    @Autowired private AuditLogService auditLogService;
    private AuditLogService auditLogRepoServiceFallback() { return auditLogService; }
}
