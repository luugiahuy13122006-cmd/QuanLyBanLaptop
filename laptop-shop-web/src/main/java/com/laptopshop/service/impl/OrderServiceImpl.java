package com.laptopshop.service.impl;

import com.laptopshop.config.ShopProperties;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.dto.RefundRequestDTO;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.CodeGeneratorService;
import com.laptopshop.service.FileStorageService;
import com.laptopshop.service.OrderService;
import com.laptopshop.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final LaptopRepository laptopRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CodeGeneratorService codeGenerator;
    private final FileStorageService fileStorage;
    private final ShopProperties shopProperties;
    private final QrCodeService qrCodeService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            LaptopRepository laptopRepository,
                            PaymentRepository paymentRepository,
                            OrderStatusHistoryRepository historyRepository,
                            CodeGeneratorService codeGenerator,
                            FileStorageService fileStorage,
                            ShopProperties shopProperties,
                            QrCodeService qrCodeService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.laptopRepository = laptopRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.codeGenerator = codeGenerator;
        this.fileStorage = fileStorage;
        this.shopProperties = shopProperties;
        this.qrCodeService = qrCodeService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(PlaceOrderRequest request) {
        return placeOrder(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(PlaceOrderRequest request, Integer createdByAccountId) {
        String threadName = Thread.currentThread().getName();
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setCreatedBy(createdByAccountId);
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

        List<OrderItemRequest> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getLaptopId))
                .toList();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : sortedItems) {
            Integer qty = itemReq.getQuantity();
            if (qty == null || qty <= 0) {
                throw new BusinessException("Số lượng sản phẩm phải lớn hơn 0.");
            }
            log.info("[ORDER | THREAD: {}] Khóa PESSIMISTIC_WRITE laptop ID={}", threadName, itemReq.getLaptopId());
            Laptop laptop = laptopRepository.findByIdForUpdate(itemReq.getLaptopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + itemReq.getLaptopId()));
            if (!Boolean.TRUE.equals(laptop.getIsActive())) {
                throw new BusinessException("Sản phẩm '" + laptop.getName() + "' hiện đã ngừng kinh doanh.");
            }
            if (laptop.getStockQuantity() < qty) {
                throw new BusinessException(String.format(
                        "Sản phẩm '%s' (SKU: %s) không đủ tồn kho (Yêu cầu: %d, Hiện còn: %d)",
                        laptop.getName(), laptop.getSku(), qty, laptop.getStockQuantity()));
            }
            laptop.setStockQuantity(laptop.getStockQuantity() - qty);
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);

            OrderDetail detail = new OrderDetail(order, laptop, qty, laptop.getSalePrice());
            order.addDetail(detail);
            subtotal = subtotal.add(detail.getLineTotal());
        }

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTotalAmount(totalAmount);

        String orderCode = codeGenerator.nextOrderCode();
        order.setOrderCode(orderCode);

        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.BANK_TRANSFER;
        if (method == PaymentMethod.BANK_TRANSFER) {
            order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(shopProperties.getPayment().getExpireMinutes()));
        }
        order.addPayment(new Payment(order, method, PaymentStatus.UNPAID, totalAmount, "TXN-" + orderCode));

        OrderStatusHistory history = new OrderStatusHistory(order, null, OrderStatus.PENDING,
                "Khởi tạo đơn hàng mới " + orderCode);
        history.setChangedBy(createdByAccountId);
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);
        log.info("[ORDER | THREAD: {}] Đặt hàng thành công: {} ({})", threadName, saved.getOrderCode(), method);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        return updateOrderStatus(orderId, newStatus, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Integer orderId, OrderStatus newStatus, Integer changedByAccountId) {
        Order order = lockOrder(orderId);
        OrderStatus current = order.getStatus();
        if (current == newStatus) return order;
        if (current.isClosed()) {
            throw new BusinessException("Đơn hàng đã '" + current.getDisplayName() + "' nên không thể thay đổi trạng thái.");
        }
        if (current == OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("Đơn đang chờ hoàn tiền. Hãy dùng chức năng 'Xác nhận đã hoàn tiền'.");
        }
        if (newStatus == OrderStatus.REFUND_REQUESTED || newStatus == OrderStatus.REFUNDED) {
            throw new BusinessException("Trạng thái này chỉ được cập nhật qua luồng hoàn tiền.");
        }

        Payment latest = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).orElse(null);
        boolean bank = latest != null && latest.getPaymentMethod() == PaymentMethod.BANK_TRANSFER;
        PaymentStatus payStatus = latest != null ? latest.getPaymentStatus() : PaymentStatus.UNPAID;

        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED) {
            if (bank && payStatus == PaymentStatus.PAID) {
                throw new BusinessException("Đơn đã thanh toán chuyển khoản. Hãy xử lý qua luồng hoàn tiền.");
            }
            if (bank && payStatus == PaymentStatus.PENDING_CONFIRMATION) {
                throw new BusinessException("Đơn đang chờ xác nhận thanh toán. Hãy xác nhận hoặc từ chối ảnh minh chứng trước.");
            }
            return cancelOrReturn(order, newStatus, "Hủy bởi nhân viên", changedByAccountId,
                    "Cập nhật trạng thái từ " + current.name() + " sang " + newStatus.name());
        }

        if (bank && payStatus != PaymentStatus.PAID) {
            throw new BusinessException("Đơn chuyển khoản chưa được xác nhận thanh toán, chưa thể chuyển sang '"
                    + newStatus.getDisplayName() + "'.");
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        if (newStatus == OrderStatus.COMPLETED) {
            for (Payment p : paymentRepository.findByOrderIdOrderByIdAsc(orderId)) {
                if (p.getPaymentStatus() != PaymentStatus.PAID) {
                    p.setPaymentStatus(PaymentStatus.PAID);
                    p.setPaidAt(LocalDateTime.now());
                    p.setConfirmedBy(changedByAccountId);
                    paymentRepository.save(p);
                }
            }
        }
        addHistory(order, current, newStatus,
                "Cập nhật trạng thái từ " + current.name() + " sang " + newStatus.name(), changedByAccountId);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order cancelOrderByCustomer(Integer orderId, Integer accountId, String reason) {
        Order order = lockOrder(orderId);
        assertOwner(order, accountId);
        if (!CUSTOMER_CANCELLABLE.contains(order.getStatus())) {
            throw new BusinessException("Chỉ có thể hủy đơn khi chưa giao hàng.");
        }
        Payment p = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).orElse(null);
        if (p != null && p.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                throw new BusinessException("Đơn đã thanh toán. Vui lòng gửi yêu cầu hoàn tiền thay vì hủy đơn.");
            }
            if (p.getPaymentStatus() == PaymentStatus.PENDING_CONFIRMATION) {
                throw new BusinessException("Ảnh minh chứng của bạn đang chờ cửa hàng xác nhận.");
            }
        }
        String cleanReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Khách hàng hủy đơn";
        return cancelOrReturn(order, OrderStatus.CANCELLED, abbreviate(cleanReason, 500), accountId,
                "Khách hàng hủy đơn: " + abbreviate(cleanReason, 400));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order requestRefund(Integer orderId, Integer accountId, RefundRequestDTO dto) {
        Order order = lockOrder(orderId);
        assertOwner(order, accountId);
        if (!CUSTOMER_CANCELLABLE.contains(order.getStatus())) {
            throw new BusinessException("Chỉ có thể yêu cầu hoàn tiền khi đơn chưa giao hàng.");
        }
        Payment p = latestPayment(orderId);
        if (p.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || p.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Chỉ đơn đã thanh toán chuyển khoản thành công mới có thể yêu cầu hoàn tiền.");
        }
        OrderStatus old = order.getStatus();
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        order.setRefundReason(dto.getReason().trim());
        order.setRefundBankName(dto.getBankName().trim());
        order.setRefundAccountNumber(dto.getAccountNumber().trim());
        order.setRefundAccountHolder(dto.getAccountHolder().trim());
        order.setRefundRequestedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        addHistory(order, old, OrderStatus.REFUND_REQUESTED,
                "Khách yêu cầu hoàn tiền: " + abbreviate(dto.getReason().trim(), 400), accountId);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment confirmCustomerBankTransfer(Integer orderId, Integer accountId) {
        Order order = lockOrder(orderId);
        assertOwner(order, accountId);
        Payment payment = latestPayment(orderId);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Đơn hàng này không thanh toán bằng chuyển khoản.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return payment;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Đơn hàng không còn ở trạng thái chờ thanh toán.");
        }
        if (order.getPaymentExpiresAt() != null && LocalDateTime.now().isAfter(order.getPaymentExpiresAt())) {
            throw new BusinessException("Đơn hàng đã quá hạn thanh toán.");
        }
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(accountId);
        payment.setRejectReason(null);
        paymentRepository.save(payment);

        OrderStatus oldStatus = order.getStatus();
        order.setPaymentExpiresAt(null);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        addHistory(order, oldStatus, OrderStatus.CONFIRMED,
                "Khách hàng xác nhận đã chuyển khoản thành công qua VietQR", accountId);
        orderRepository.save(order);
        log.info("[PAYMENT-QR] Khách hàng xác nhận chuyển khoản thành công cho đơn {}", order.getOrderCode());
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderForQrToken(Integer orderId, String token) {
        Order order = orderRepository.findWithCustomerById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        order.getDetails().size();
        order.getPayments().size();
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy giao dịch thanh toán cho đơn hàng này."));
        if (!qrCodeService.verifyToken(order.getId(), order.getOrderCode(), payment.getTransactionCode(), token)) {
            throw new BusinessException("Liên kết QR thanh toán không hợp lệ.");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment confirmBankTransferByQrToken(Integer orderId, String token) {
        Order order = lockOrder(orderId);
        Payment payment = latestPayment(orderId);
        if (!qrCodeService.verifyToken(order.getId(), order.getOrderCode(), payment.getTransactionCode(), token)) {
            throw new BusinessException("Mã QR xác nhận không hợp lệ.");
        }
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Đơn hàng này không thanh toán bằng chuyển khoản.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return payment;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Đơn hàng không còn ở trạng thái chờ thanh toán.");
        }
        if (order.getPaymentExpiresAt() != null && LocalDateTime.now().isAfter(order.getPaymentExpiresAt())) {
            throw new BusinessException("Đơn hàng đã quá hạn thanh toán.");
        }
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Integer customerAccountId = order.getCustomer() != null ? order.getCustomer().getAccountId() : null;
        payment.setConfirmedBy(customerAccountId);
        payment.setRejectReason(null);
        paymentRepository.save(payment);

        OrderStatus oldStatus = order.getStatus();
        order.setPaymentExpiresAt(null);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        addHistory(order, oldStatus, OrderStatus.CONFIRMED,
                "Khách hàng quét mã QR trên điện thoại và xác nhận đã chuyển khoản", customerAccountId);
        orderRepository.save(order);
        log.info("[QR-PAY-MOBILE] Đã xác nhận thanh toán đơn {} từ trang quét QR", order.getOrderCode());
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment uploadPaymentProof(Integer orderId, Integer accountId, MultipartFile file) {
        Order order = lockOrder(orderId);
        assertOwner(order, accountId);
        Payment payment = latestPayment(orderId);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Đơn hàng này không thanh toán bằng chuyển khoản.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Đơn hàng không còn ở trạng thái chờ thanh toán.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng đã được xác nhận thanh toán.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.UNPAID && order.getPaymentExpiresAt() != null
                && LocalDateTime.now().isAfter(order.getPaymentExpiresAt())) {
            throw new BusinessException("Đơn hàng đã quá hạn thanh toán và sắp bị hủy tự động.");
        }
        String stored = fileStorage.storePaymentProof(file);
        String oldFile = payment.getProofImage();
        payment.setProofImage(stored);
        payment.setProofUploadedAt(LocalDateTime.now());
        payment.setRejectReason(null);
        payment.setPaymentStatus(PaymentStatus.PENDING_CONFIRMATION);
        paymentRepository.save(payment);
        addHistory(order, order.getStatus(), order.getStatus(), "Khách tải ảnh minh chứng chuyển khoản", accountId);
        fileStorage.deleteQuietly(oldFile);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaymentProofOwnedBy(String filename, Integer accountId) {
        return paymentRepository.findByProofImage(filename)
                .map(p -> p.getOrder().getCustomer() != null
                        && accountId != null
                        && accountId.equals(p.getOrder().getCustomer().getAccountId()))
                .orElse(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment confirmPayment(Integer orderId, Integer staffAccountId) {
        Order order = lockOrder(orderId);
        if (order.getStatus().isClosed() || order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("Đơn hàng không còn ở trạng thái có thể xác nhận thanh toán.");
        }
        Payment payment = latestPayment(orderId);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException("Chỉ đơn chuyển khoản mới cần xác nhận thanh toán.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng đã được xác nhận thanh toán trước đó.");
        }
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedBy(staffAccountId);
        payment.setRejectReason(null);
        paymentRepository.save(payment);
        order.setPaymentExpiresAt(null);
        orderRepository.save(order);
        addHistory(order, order.getStatus(), order.getStatus(), "Xác nhận thanh toán chuyển khoản thành công", staffAccountId);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment rejectPaymentProof(Integer orderId, Integer staffAccountId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do từ chối ảnh minh chứng.");
        }
        Order order = lockOrder(orderId);
        Payment payment = latestPayment(orderId);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER
                || payment.getPaymentStatus() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Đơn hàng không có ảnh minh chứng nào đang chờ xác nhận.");
        }
        String cleanReason = abbreviate(reason.trim(), 500);
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setRejectReason(cleanReason);
        payment.setConfirmedBy(staffAccountId);
        paymentRepository.save(payment);
        order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(shopProperties.getPayment().getExpireMinutes()));
        orderRepository.save(order);
        addHistory(order, order.getStatus(), order.getStatus(),
                "Từ chối ảnh minh chứng: " + abbreviate(cleanReason, 400), staffAccountId);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order confirmRefund(Integer orderId, Integer staffAccountId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("Đơn hàng không ở trạng thái 'Yêu cầu hoàn tiền'.");
        }
        restoreStock(order);
        for (Payment p : paymentRepository.findByOrderIdOrderByIdAsc(orderId)) {
            if (p.getPaymentStatus() == PaymentStatus.PAID) {
                p.setPaymentStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
            }
        }
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        addHistory(order, OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED,
                "Đã hoàn tiền cho khách và hoàn lại tồn kho", staffAccountId);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsAwaitingConfirmation() {
        List<Payment> list = paymentRepository.findByPaymentStatusOrderByCreatedAtAsc(PaymentStatus.PENDING_CONFIRMATION);
        list.forEach(p -> p.getOrder().getOrderCode());
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean paymentProofExists(String filename) {
        return paymentRepository.findByProofImage(filename).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findExpiredPendingPaymentOrderIds() {
        return orderRepository.findExpiredBankTransferOrderIds(LocalDateTime.now(), EnumSet.of(PaymentStatus.UNPAID));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoCancelExpiredOrder(Integer orderId) {
        String threadName = Thread.currentThread().getName();
        Order order = lockOrder(orderId);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId).orElse(null);
        boolean stillExpired = order.getStatus() == OrderStatus.PENDING
                && order.getPaymentExpiresAt() != null
                && order.getPaymentExpiresAt().isBefore(LocalDateTime.now())
                && payment != null
                && payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && payment.getPaymentStatus() == PaymentStatus.UNPAID;
        if (!stillExpired) {
            log.info("[SCHEDULED | THREAD: {}] Bỏ qua đơn {}: đã được xử lý trước khi tự hủy", threadName, order.getOrderCode());
            return false;
        }
        String reason = "Quá hạn " + shopProperties.getPayment().getExpireMinutes() + " phút chưa thanh toán chuyển khoản";
        cancelOrReturn(order, OrderStatus.CANCELLED, reason, null, "Hệ thống tự động hủy: " + reason);
        log.warn("[SCHEDULED | THREAD: {}] Đã tự động hủy đơn {} và hoàn tồn kho ({})", threadName, order.getOrderCode(), reason);
        return true;
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
    public Order getOrderForCustomer(Integer orderId, Integer accountId) {
        Order order = orderRepository.findByIdAndCustomer_AccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        order.getDetails().size();
        order.getPayments().size();
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrders(OrderStatus status, Pageable pageable) {
        return getOrders(status, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrders(OrderStatus status, String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return orderRepository.filterOrders(status, null, kw, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerId(Integer customerId) {
        List<Order> orders = orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        orders.forEach(o -> o.getPayments().size());
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Integer orderId) {
        return paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderStatusHistory(Integer orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }

    private Order lockOrder(Integer orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
    }

    private Payment latestPayment(Integer orderId) {
        return paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new BusinessException("Đơn hàng chưa có thông tin thanh toán."));
    }

    private void assertOwner(Order order, Integer accountId) {
        Customer c = order.getCustomer();
        if (c == null || accountId == null || !accountId.equals(c.getAccountId())) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + order.getId());
        }
    }

    private Order cancelOrReturn(Order order, OrderStatus newStatus, String reason, Integer by, String note) {
        OrderStatus old = order.getStatus();
        restoreStock(order);
        for (Payment p : paymentRepository.findByOrderIdOrderByIdAsc(order.getId())) {
            if (p.getPaymentStatus() != PaymentStatus.PAID && p.getPaymentStatus() != PaymentStatus.REFUNDED) {
                p.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(p);
            }
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        if (newStatus == OrderStatus.CANCELLED && order.getCancelReason() == null) {
            order.setCancelReason(reason);
        }
        addHistory(order, old, newStatus, note, by);
        return orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        List<OrderDetail> sorted = order.getDetails().stream()
                .filter(d -> d.getLaptop() != null)
                .sorted(Comparator.comparing((OrderDetail d) -> d.getLaptop().getId()))
                .toList();
        for (OrderDetail detail : sorted) {
            Laptop laptop = laptopRepository.findByIdForUpdate(detail.getLaptop().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + detail.getLaptop().getId()));
            laptop.setStockQuantity(laptop.getStockQuantity() + detail.getQuantity());
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);
        }
    }

    private void addHistory(Order order, OrderStatus from, OrderStatus to, String note, Integer by) {
        OrderStatusHistory h = new OrderStatusHistory(order, from, to, abbreviate(note, 500));
        h.setChangedBy(by);
        historyRepository.save(h);
    }

    private static String abbreviate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
