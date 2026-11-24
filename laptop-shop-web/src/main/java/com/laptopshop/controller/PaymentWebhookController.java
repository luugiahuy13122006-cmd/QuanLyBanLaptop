package com.laptopshop.controller;

import com.laptopshop.service.PaymentGatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentGatewayService paymentGatewayService;

    public PaymentWebhookController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    public static class PaymentWebhookPayload {
        private String orderCode;
        private String transactionCode;
        private BigDecimal amount;
        private String gatewayRef;

        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
        public String getTransactionCode() { return transactionCode; }
        public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getGatewayRef() { return gatewayRef; }
        public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }
    }

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> receiveBankWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody PaymentWebhookPayload payload) {
        try {
            boolean processed = paymentGatewayService.processBankWebhook(
                    payload.getOrderCode(),
                    payload.getTransactionCode(),
                    payload.getAmount(),
                    payload.getGatewayRef(),
                    signature
            );
            return ResponseEntity.ok(Map.of(
                    "status", processed ? "PROCESSED" : "ALREADY_PROCESSED",
                    "orderCode", payload.getOrderCode(),
                    "gatewayRef", payload.getGatewayRef()
            ));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "UNAUTHORIZED",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "message", ex.getMessage()
            ));
        }
    }
}
