package com.laptopshop;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
// Kiểm thử đa luồng đặt hàng (10 luồng tranh 5 máy) chống Overselling bằng PESSIMISTIC_WRITE Lock & kiểm thử vòng đời đơn hàng.

import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.*;
import com.laptopshop.repository.*;
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

    @Autowired private OrderService orderService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private LaptopRepository laptopRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private Integer testLaptopId;
    private Integer testCustomerId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        historyRepository.deleteAll();
        paymentRepository.deleteAll();
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        laptopRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();

        Category cat = categoryRepository.save(new Category("Gaming", "Dòng laptop gaming hiệu năng cao"));
        Brand brand = brandRepository.save(new Brand("ASUS", "Đài Loan", "Thương hiệu laptop ASUS"));

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
    @DisplayName("Kiểm thử 10 luồng đặt hàng đồng thời khi kho chỉ còn 5 chiếc (Chống Overselling)")
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
                    startLatch.await();
                    PlaceOrderRequest req = new PlaceOrderRequest();
                    req.setCustomerId(testCustomerId);
                    req.setPaymentMethod(PaymentMethod.CASH);
                    req.setDeliveryAddress("140 Lê Trọng Tấn, Tân Phú, TP.HCM");
                    req.setNote("Đặt mua đồng thời");
                    req.setItems(List.of(new OrderItemRequest(testLaptopId, 1)));
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
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        Laptop updatedLaptop = laptopRepository.findById(testLaptopId).orElseThrow();

        assertEquals(5, successCount.get(), "Phải có đúng 5 đơn hàng đặt thành công");
        assertEquals(5, failCount.get(), "Phải có đúng 5 đơn hàng bị từ chối do hết kho");
        assertEquals(0, updatedLaptop.getStockQuantity(), "Tồn kho cuối cùng phải bằng 0, không được âm");
        assertEquals(5, orderRepository.count());
        assertEquals(5, paymentRepository.count());
        assertEquals(5, historyRepository.count());
    }

    @Test
    @DisplayName("Kiểm thử hủy đơn hàng hoàn kho, lịch sử trạng thái và báo cáo thống kê")
    void testOrderLifecycleAndStatistics() {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setCustomerId(testCustomerId);
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setDeliveryAddress("140 Lê Trọng Tấn, Tân Phú, TP.HCM");
        req.setNote("Đơn hàng test thống kê");
        req.setItems(List.of(new OrderItemRequest(testLaptopId, 2)));
        req.setDiscount(new BigDecimal("1000000"));

        Order order = orderService.placeOrder(req);
        assertNotNull(order.getOrderCode());
        assertEquals(0, new BigDecimal("50000000").compareTo(order.getSubtotal()));
        assertEquals(0, new BigDecimal("49000000").compareTo(order.getTotalAmount()));
        assertEquals(3, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity());

        orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        Payment payment = orderService.getPaymentByOrderId(order.getId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(0, new BigDecimal("49000000").compareTo(statisticsService.getTotalRevenue()));
        assertFalse(statisticsService.getMonthlyRevenue(LocalDate.now().getYear()).isEmpty());
        assertFalse(statisticsService.getTopSellingLaptops(5).isEmpty());

        PlaceOrderRequest req2 = new PlaceOrderRequest();
        req2.setCustomerId(testCustomerId);
        req2.setPaymentMethod(PaymentMethod.CASH);
        req2.setDeliveryAddress("140 Lê Trọng Tấn, Tân Phú, TP.HCM");
        req2.setNote("Đơn hàng test hủy hoàn kho");
        req2.setItems(List.of(new OrderItemRequest(testLaptopId, 2)));
        Order order2 = orderService.placeOrder(req2);
        assertEquals(1, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity());

        orderService.updateOrderStatus(order2.getId(), OrderStatus.CANCELLED);
        assertEquals(3, laptopRepository.findById(testLaptopId).orElseThrow().getStockQuantity());
        assertEquals(2, orderService.getOrderStatusHistory(order2.getId()).size());
    }
}
