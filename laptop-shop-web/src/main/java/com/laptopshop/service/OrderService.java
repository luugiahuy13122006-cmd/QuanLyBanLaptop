package com.laptopshop.service;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.OrderStatus;
import com.laptopshop.entity.OrderStatusHistory;
import com.laptopshop.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(PlaceOrderRequest request);
    Order placeOrder(PlaceOrderRequest request, Integer createdByAccountId);
    Order updateOrderStatus(Integer orderId, OrderStatus newStatus);
    Order updateOrderStatus(Integer orderId, OrderStatus newStatus, Integer changedByAccountId);
    Order getOrderById(Integer orderId);
    Page<Order> getOrders(OrderStatus status, Pageable pageable);
    Page<Order> getOrders(OrderStatus status, String keyword, Pageable pageable);
    List<Order> getOrdersByCustomerId(Integer customerId);
    Order cancelOrderByCustomer(Integer orderId, Integer customerId, Integer accountId);
    Optional<Payment> getPaymentByOrderId(Integer orderId);
    List<OrderStatusHistory> getOrderStatusHistory(Integer orderId);
}
