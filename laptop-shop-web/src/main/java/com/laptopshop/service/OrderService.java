package com.laptopshop.service;

import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.dto.RefundRequestDTO;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.OrderStatus;
import com.laptopshop.entity.OrderStatusHistory;
import com.laptopshop.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(PlaceOrderRequest request);
    Order placeOrder(PlaceOrderRequest request, Integer createdByAccountId);

    Order updateOrderStatus(Integer orderId, OrderStatus newStatus);
    Order updateOrderStatus(Integer orderId, OrderStatus newStatus, Integer changedByAccountId);

    Order getOrderById(Integer orderId);
    Order getOrderForCustomer(Integer orderId, Integer accountId);

    Page<Order> getOrders(OrderStatus status, Pageable pageable);
    Page<Order> getOrders(OrderStatus status, String keyword, Pageable pageable);
    List<Order> getOrdersByCustomerId(Integer customerId);

    // Khách hàng
    Order cancelOrderByCustomer(Integer orderId, Integer accountId, String reason);
    Order requestRefund(Integer orderId, Integer accountId, RefundRequestDTO dto);
    Payment confirmCustomerBankTransfer(Integer orderId, Integer accountId);
    Order getOrderForQrToken(Integer orderId, String token);
    Payment confirmBankTransferByQrToken(Integer orderId, String token);
    Payment uploadPaymentProof(Integer orderId, Integer accountId, MultipartFile file);
    boolean isPaymentProofOwnedBy(String filename, Integer accountId);

    // Admin / Staff
    Payment confirmPayment(Integer orderId, Integer staffAccountId);
    Payment rejectPaymentProof(Integer orderId, Integer staffAccountId, String reason);
    Order confirmRefund(Integer orderId, Integer staffAccountId);
    List<Payment> getPaymentsAwaitingConfirmation();
    boolean paymentProofExists(String filename);

    // Scheduler
    List<Integer> findExpiredPendingPaymentOrderIds();
    boolean autoCancelExpiredOrder(Integer orderId);

    Optional<Payment> getPaymentByOrderId(Integer orderId);
    List<OrderStatusHistory> getOrderStatusHistory(Integer orderId);

    Order updateShippingInfo(Integer orderId, String carrierName, String trackingCode, Integer actorId);
}
