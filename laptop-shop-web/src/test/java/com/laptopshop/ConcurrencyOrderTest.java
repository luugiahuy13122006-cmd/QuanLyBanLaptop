package com.laptopshop;

import com.laptopshop.dto.*;
import com.laptopshop.entity.*;
import com.laptopshop.repository.*;
import com.laptopshop.service.CustomerService;
import com.laptopshop.service.OrderService;
import com.laptopshop.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrencyOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private LaptopRepository laptopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    private Integer testLaptopId;
    private Integer testCustomerId;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        paymentRepository.deleteAll();
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        laptopRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();

        Category cat = new Category("Gaming", "Dòng laptop gaming hiệu năng cao");
        cat = categoryRepository.save(cat);

        Brand brand = new Brand("ASUS", "Đài Loan", "Thương hiệu laptop ASUS");
        brand = brandRepository.save(brand);

        // Khởi tạo 1 Laptop chỉ còn đúng 5 chiếc trong kho (StockQuantity = 5)
        Laptop laptop = new Laptop();
        laptop.setSku("LAP-001");
        laptop.setName("ASUS TUF Gaming F15");
        laptop.setCategory(cat);
        laptop.setBrand(brand);
        laptop.setCpu("Intel Core i7-12700H");
        laptop.setRamGb(16);
        laptop.setStorageGb(512);
        laptop.setGpu("NVIDIA RTX 4050 6GB");
        laptop.setScreenSize(new BigDecimal("15.6"));
        laptop.setOperatingSystem("Windows 11 Home");
        laptop.setColor("Gray");
        laptop.setPurchasePrice(new BigDecimal("21000000"));
        laptop.setSalePrice(new BigDecimal("25000000"));
        laptop.setStockQuantity(5);
        laptop.setImageUrl("asus-tuf-f15.jpg");
        laptop.setDescription("Laptop gaming bền bỉ chuẩn quân đội");
        laptop.setIsActive(true);
        testLaptopId = laptopRepository.save(laptop).getId();

        Customer customer = new Customer(
                "Nguyễn Tuấn Khang",
                "khang.tv4@gmail.com",
                "0912345678",
                "140 Lê Trọng Tấn, Tân Phú, TP.HCM"
        );
        testCustomerId = customerRepository.save(customer).getId();
    }

    @Test
    @DisplayName("Kiểm thử 10 luồng đặt hàng đồng thời khi kho chỉ còn 5 chiếc (Chống Overselling trên QL_Laptop)")
    void testConcurrentOrderPlacement_ShouldPreventOverselling() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Đợi tín hiệu xuất phát cùng lúc

                    PlaceOrderRequest req = new PlaceOrderRequest(
                            testCustomerId,
                            PaymentMethod.BANK_TRANSFER,
                            "140 Lê Trọng Tấn, Tân Phú, TP.HCM",
                            "Đặt mua đồng thời",
                            List.of(new OrderItemRequest(testLaptopId, 1))
                    );
                    orderService.placeOrder(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add(e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Cho phép 10 luồng bắn yêu cầu đặt hàng cùng 1 thời điểm
        doneLatch.await();
        executorService.shutdown();

        Laptop updatedLaptop = laptopRepository.findById(testLaptopId).orElseThrow();

        // Khẳng định: Đúng 5 đơn thành công, 5 đơn thất bại do hết hàng, tồn kho cuối = 0 (không bị âm)
        assertEquals(5, successCount.get(), "Phải có đúng 5 đơn hàng đặt thành công");
        assertEquals(5, failCount.get(), "Phải có đúng 5 đơn hàng bị từ chối do hết kho");
        assertEquals(0, updatedLaptop.getStockQuantity(), "Tồn kho cuối cùng phải bằng 0, tuyệt đối không âm");
        assertEquals(5, orderRepository.count(), "Bảng Orders phải có đúng 5 bản ghi");
        assertEquals(5, paymentRepository.count(), "Bảng Payments phải có đúng 5 bản ghi");
        assertEquals(5, historyRepository.count(), "Bảng OrderStatusHistory phải có đúng 5 bản ghi khởi tạo");
    }

    @Test
    @DisplayName("Kiểm thử hủy đơn hàng hoàn kho, lịch sử trạng thái và báo cáo thống kê QL_Laptop")
    void testOrderLifecycleAndStatistics() {
        PlaceOrderRequest req = new PlaceOrderRequest(
                testCustomerId,
                PaymentMethod.E_WALLET,
                "140 Lê Trọng Tấn, Tân Phú, TP.HCM",
                "Đơn hàng test thống kê",
                List.of(new OrderItemRequest(testLaptopId, 2))
        );
        req.setDiscount(new BigDecimal("1000000"));
        req.setShippingFee(new BigDecimal("50000"));

        Order order = orderService.placeOrder(req);
        assertNotNull(order.getOrderCode());
        assertEquals(0, new BigDecimal("50000000").compareTo(order.getSubtotal()));
        assertEquals(0, new BigDecimal("49050000").compareTo(order.getTotalAmount()));
        assertEquals(3, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity());

        // Chuyển sang COMPLETED -> kiểm tra doanh thu và trạng thái Payment = PAID
        orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        Payment payment = orderService.getPaymentByOrderId(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(0, new BigDecimal("49050000").compareTo(statisticsService.getTotalRevenue()));
        assertFalse(statisticsService.getMonthlyRevenue(LocalDate.now().getYear()).isEmpty());
        assertFalse(statisticsService.getTopSellingLaptops(5).isEmpty());

        // Tạo đơn thứ 2 rồi HỦY (CANCELLED) -> kiểm tra hoàn kho và lịch sử trạng thái
        PlaceOrderRequest req2 = new PlaceOrderRequest(
                testCustomerId,
                PaymentMethod.CASH,
                "140 Lê Trọng Tấn, Tân Phú, TP.HCM",
                "Đơn hàng test hủy hoàn kho",
                List.of(new OrderItemRequest(testLaptopId, 2))
        );
        Order order2 = orderService.placeOrder(req2);
        assertEquals(1, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity());

        orderService.updateOrderStatus(order2.getId(), OrderStatus.CANCELLED);
        assertEquals(3, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity(),
                "Sau khi hủy đơn 2 chiếc, tồn kho phải được hoàn lại từ 1 lên 3");
        assertEquals(2, orderService.getOrderStatusHistory(order2.getId()).size(),
                "Đơn hàng 2 phải có 2 bản ghi lịch sử (PENDING -> CANCELLED)");
    }
}
