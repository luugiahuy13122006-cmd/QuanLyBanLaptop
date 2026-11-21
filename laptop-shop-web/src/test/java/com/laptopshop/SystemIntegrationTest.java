package com.laptopshop;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Kiểm thử tổng thể toàn hệ thống: Phân quyền (Spring Security), Tài khoản (BCrypt), Ràng buộc dữ liệu (CRUD),
// Đa luồng tồn kho (TV2) và Giao tiếp Socket Server với 3 Client đồng thời (TV2).

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        PlaceOrderRequest req = new PlaceOrderRequest(
                customerId, PaymentMethod.CASH, "TP.HCM", "Test FK",
                List.of(new OrderItemRequest(laptopId, 1))
        );
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
}
