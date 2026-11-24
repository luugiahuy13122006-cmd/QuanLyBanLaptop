package com.laptopshop.service;

import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.repository.OrderStatusHistoryRepository;
import com.laptopshop.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);

    private final String paymentMode;
    private final String webhookSecret;
    private final Environment environment;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final AuditLogService auditLogService;

    public PaymentGatewayService(
            @Value("${laptop.payment.mode:DEMO}") String paymentMode,
            @Value("${laptop.payment.webhook-secret:}") String webhookSecret,
            Environment environment,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository historyRepository,
            AuditLogService auditLogService) {
        this.paymentMode = paymentMode;
        this.webhookSecret = webhookSecret;
        this.environment = environment;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && (webhookSecret == null || webhookSecret.trim().length() < 16)) {
            throw new IllegalStateException("PAYMENT_WEBHOOK_SECRET bắt buộc >= 16 ký tự trong profile prod.");
        }
    }

    public boolean isDemoMode() {
        return !"PRODUCTION".equalsIgnoreCase(paymentMode);
    }

    public String getPaymentMode() {
        return isDemoMode() ? "DEMO" : "PRODUCTION";
    }

    public String computeSignature(String orderCode, String transactionCode, BigDecimal amount, String gatewayRef) {
        try {
            String key = (webhookSecret != null && !webhookSecret.isBlank())
                    ? webhookSecret : "NEXORA_DEV_WEBHOOK_SECRET_2026";
            String amt = amount != null ? amount.stripTrailingZeros().toPlainString() : "0";
            String payload = orderCode + "|" + (transactionCode != null ? transactionCode : "") + "|" + amt + "|" + gatewayRef;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tính chữ ký Webhook", e);
        }
    }

    public boolean verifySignature(String orderCode, String transactionCode, BigDecimal amount, String gatewayRef, String signature) {
        if (signature == null || signature.isBlank()) return false;
        String expected = computeSignature(orderCode, transactionCode, amount, gatewayRef);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processBankWebhook(String orderCode, String transactionCode, BigDecimal amount,
                                      String gatewayRef, String signature) {
        if (orderCode == null || orderCode.isBlank() || gatewayRef == null || gatewayRef.isBlank() || amount == null) {
            throw new BusinessException("Thiếu thông tin webhook (orderCode, amount, gatewayRef).");
        }
        if (!verifySignature(orderCode.trim(), transactionCode, amount, gatewayRef.trim(), signature)) {
            auditLogService.logSecurityEvent(null, "WEBHOOK", "WEBHOOK_INVALID_SIG", "Payment", orderCode,
                    "Từ chối webhook chữ ký không hợp lệ, gatewayRef=" + gatewayRef);
            throw new SecurityException("Chữ ký Webhook không hợp lệ.");
        }
        if (paymentRepository.existsByGatewayRef(gatewayRef.trim())) {
            log.info("[WEBHOOK] Bỏ qua callback trùng lặp gatewayRef={}", gatewayRef);
            return false;
        }
        Order order = orderRepository.findByOrderCodeIgnoreCase(orderCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderCode));
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + order.getId()));
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(lockedOrder.getId())
                .orElseThrow(() -> new BusinessException("Đơn hàng chưa có bản ghi thanh toán."));

        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Đơn hàng không dùng phương thức chuyển khoản.");
        }
        if (lockedOrder.getTotalAmount().compareTo(amount) != 0) {
            throw new BusinessException("Số tiền chuyển khoản không khớp tổng đơn hàng (" + lockedOrder.getTotalAmount() + ").");
        }
        if (lockedOrder.getStatus().isClosed() || lockedOrder.getStatus() == OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("Đơn hàng ở trạng thái đóng, không thể xác nhận thanh toán.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            if (payment.getGatewayRef() == null) {
                payment.setGatewayRef(gatewayRef.trim());
                paymentRepository.save(payment);
            }
            return false;
        }

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayRef(gatewayRef.trim());
        payment.setRejectReason(null);
        paymentRepository.save(payment);

        OrderStatus prev = lockedOrder.getStatus();
        lockedOrder.setPaymentExpiresAt(null);
        if (prev == OrderStatus.PENDING) {
            lockedOrder.setStatus(OrderStatus.CONFIRMED);
        }
        lockedOrder.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(lockedOrder);

        historyRepository.save(new OrderStatusHistory(lockedOrder, prev, lockedOrder.getStatus(),
                "Webhook ngân hàng xác nhận thanh toán thành công (Ref: " + gatewayRef.trim() + ")"));
        auditLogService.logAction(null, "WEBHOOK", "PAYMENT_WEBHOOK_PAID", "Order",
                lockedOrder.getOrderCode(), "Số tiền " + amount + ", Ref=" + gatewayRef.trim());
        log.info("[WEBHOOK] Đã xác nhận thanh toán đơn {} (gatewayRef={})", lockedOrder.getOrderCode(), gatewayRef);
        return true;
    }
}
