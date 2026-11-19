package com.laptopshop.service;

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
    Order updateOrderStatus(Integer orderId, OrderStatus newStatus);
    Order getOrderById(Integer orderId);
    Page<Order> getOrders(OrderStatus status, Pageable pageable);
    Optional<Payment> getPaymentByOrderId(Integer orderId);
    List<OrderStatusHistory> getOrderStatusHistory(Integer orderId);
}
