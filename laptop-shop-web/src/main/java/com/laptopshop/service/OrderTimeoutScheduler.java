package com.laptopshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);
    private final OrderService orderService;
    private final JobLockService jobLockService;

    public OrderTimeoutScheduler(OrderService orderService, JobLockService jobLockService) {
        this.orderService = orderService;
        this.jobLockService = jobLockService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredBankTransferOrders() {
        if (!jobLockService.tryAcquire("AUTO_CANCEL_EXPIRED_ORDERS", 50)) {
            return;
        }
        try {
            String threadName = Thread.currentThread().getName();
            List<Integer> expiredIds = orderService.findExpiredPendingPaymentOrderIds();
            if (expiredIds.isEmpty()) {
                log.info("[SCHEDULED | THREAD: {}] Không có đơn chuyển khoản nào quá hạn thanh toán.", threadName);
                return;
            }
            log.warn("[SCHEDULED | THREAD: {}] Phát hiện {} đơn chuyển khoản quá hạn, bắt đầu tự hủy...", threadName, expiredIds.size());
            int cancelled = 0;
            for (Integer id : expiredIds) {
                try {
                    if (orderService.autoCancelExpiredOrder(id)) cancelled++;
                } catch (Exception ex) {
                    log.error("[SCHEDULED | THREAD: {}] Lỗi khi tự hủy đơn ID={}: {}", threadName, id, ex.getMessage());
                }
            }
            log.warn("[SCHEDULED | THREAD: {}] Hoàn tất: đã tự hủy {}/{} đơn.", threadName, cancelled, expiredIds.size());
        } finally {
            jobLockService.release("AUTO_CANCEL_EXPIRED_ORDERS");
        }
    }
}
