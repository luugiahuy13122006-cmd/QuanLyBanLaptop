package com.laptopshop.service.impl;

import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderServiceImpl implements OrderService {

    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(1000);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final LaptopRepository laptopRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            LaptopRepository laptopRepository,
                            PaymentRepository paymentRepository,
                            OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.laptopRepository = laptopRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Đặt hàng có kiểm soát đồng thời (Concurrency Control) bằng Pessimistic Write Lock
     * và quản lý giao dịch (@Transactional) trên CSDL QL_Laptop.
     * Nếu bất kỳ mặt hàng nào không đủ tồn kho (StockQuantity), toàn bộ giao dịch sẽ Rollback.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(PlaceOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + request.getCustomerId()));

        Order order = new Order();
        order.setOrderCode(generateUniqueOrderCode());
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        String recipientName = (request.getRecipientName() != null && !request.getRecipientName().isBlank())
                ? request.getRecipientName().trim() : customer.getCustomerName();
        String recipientPhone = (request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank())
                ? request.getRecipientPhone().trim() : customer.getPhone();
        String deliveryAddress = (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank())
                ? request.getDeliveryAddress().trim() : customer.getAddress();

        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setDeliveryAddress(deliveryAddress != null ? deliveryAddress : "");
        order.setNote(request.getNote());

        // Sắp xếp danh sách item theo laptopId tăng dần để tránh Deadlock khi khóa nhiều dòng
        List<OrderItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getLaptopId))
                .toList();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : sortedItems) {
            // Thu thập khóa PESSIMISTIC_WRITE trên dòng dữ liệu Laptops
            Laptop laptop = laptopRepository.findByIdForUpdate(itemReq.getLaptopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + itemReq.getLaptopId()));

            if (!Boolean.TRUE.equals(laptop.getIsActive())) {
                throw new BusinessException("Sản phẩm '" + laptop.getName() + "' hiện đã ngừng kinh doanh.");
            }

            if (laptop.getStockQuantity() < itemReq.getQuantity()) {
                throw new BusinessException(String.format(
                        "Sản phẩm '%s' (SKU: %s) không đủ tồn kho (Yêu cầu: %d, Hiện còn: %d)",
                        laptop.getName(), laptop.getSku(), itemReq.getQuantity(), laptop.getStockQuantity()));
            }

            // Trừ tồn kho an toàn trong vùng khóa
            laptop.setStockQuantity(laptop.getStockQuantity() - itemReq.getQuantity());
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);

            // Tạo chi tiết đơn hàng (OrderDetails) với thông tin Snapshot theo thiết kế của TV1
            OrderDetail detail = new OrderDetail(
                    order,
                    laptop,
                    itemReq.getQuantity(),
                    laptop.getSalePrice()
            );
            order.addDetail(detail);

            subtotal = subtotal.add(detail.getLineTotal());
        }

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);

        // Tạo bản ghi thanh toán (Payments)
        Payment payment = new Payment(
                order,
                request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH,
                PaymentStatus.UNPAID,
                totalAmount,
                "TXN-" + order.getOrderCode()
        );
        order.addPayment(payment);

        // Ghi lịch sử trạng thái đơn hàng (OrderStatusHistory)
        OrderStatusHistory history = new OrderStatusHistory(
                order,
                null,
                OrderStatus.PENDING,
                "Khởi tạo đơn hàng mới " + order.getOrderCode()
        );
        order.addStatusHistory(history);

        return orderRepository.save(order);
    }

    /**
     * Cập nhật trạng thái đơn hàng.
     * Nếu chuyển sang CANCELLED hoặc RETURNED, tự động hoàn trả lại số lượng tồn kho (StockQuantity)
     * và ghi nhận lịch sử vào bảng OrderStatusHistory.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findWithCustomerById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == newStatus) {
            return order;
        }

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.RETURNED) {
            throw new BusinessException("Đơn hàng đã hủy hoặc hoàn trả không thể thay đổi trạng thái.");
        }
        if (currentStatus == OrderStatus.COMPLETED) {
            throw new BusinessException("Đơn hàng đã hoàn thành không thể thay đổi trạng thái.");
        }

        // Hoàn lại tồn kho nếu hủy đơn hoặc hoàn trả
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED) {
            List<OrderDetail> sortedDetails = order.getDetails().stream()
                    .sorted(Comparator.comparing(d -> d.getLaptop().getId()))
                    .toList();
            for (OrderDetail detail : sortedDetails) {
                Laptop laptop = laptopRepository.findByIdForUpdate(detail.getLaptop().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + detail.getLaptop().getId()));
                laptop.setStockQuantity(laptop.getStockQuantity() + detail.getQuantity());
                laptop.setUpdatedAt(LocalDateTime.now());
                laptopRepository.save(laptop);
            }
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Đồng bộ trạng thái bảng Payments
        List<Payment> payments = paymentRepository.findByOrderIdOrderByIdAsc(orderId);
        for (Payment payment : payments) {
            if (newStatus == OrderStatus.COMPLETED) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
            } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED) {
                if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                    payment.setPaymentStatus(PaymentStatus.REFUNDED);
                } else {
                    payment.setPaymentStatus(PaymentStatus.FAILED);
                }
            }
            paymentRepository.save(payment);
        }

        // Lưu vết vào bảng OrderStatusHistory
        OrderStatusHistory history = new OrderStatusHistory(
                order,
                currentStatus,
                newStatus,
                "Cập nhật trạng thái từ " + currentStatus.name() + " sang " + newStatus.name()
        );
        historyRepository.save(history);

        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Integer orderId) {
        Order order = orderRepository.findWithCustomerById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        order.getDetails().size();
        order.getPayments().size();
        order.getStatusHistories().size();
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrders(OrderStatus status, Pageable pageable) {
        return orderRepository.filterOrders(status, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Integer orderId) {
        List<Payment> list = paymentRepository.findByOrderIdOrderByIdAsc(orderId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderStatusHistory(Integer orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }

    private String generateUniqueOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int seq = ORDER_SEQ.incrementAndGet() % 10000;
        String code = String.format("ORD-%s-%04d", datePart, seq);
        while (orderRepository.existsByOrderCodeIgnoreCase(code)) {
            seq = ORDER_SEQ.incrementAndGet() % 10000;
            code = String.format("ORD-%s-%04d", datePart, seq);
        }
        return code;
    }
}
