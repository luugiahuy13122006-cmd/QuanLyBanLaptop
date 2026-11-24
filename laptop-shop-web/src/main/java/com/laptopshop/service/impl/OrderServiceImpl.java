package com.laptopshop.service.impl;

import com.laptopshop.config.ShopProperties;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.dto.RefundRequestDTO;
import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING);
    private static final BigDecimal STAFF_MAX_DISCOUNT_RATE = new BigDecimal("0.05");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final LaptopRepository laptopRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final AccountRepository accountRepository;
    private final CodeGeneratorService codeGenerator;
    private final FileStorageService fileStorage;
    private final ShopProperties shopProperties;
    private final QrCodeService qrCodeService;
    private final StockMovementService stockMovementService;
    private final PaymentGatewayService paymentGatewayService;
    private final AuditLogService auditLogService;
    private final CouponService couponService;
    private final WarrantyService warrantyService;
    private final EmailService emailService;
    private final NotificationSseService notificationSseService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CustomerRepository customerRepository,
                            LaptopRepository laptopRepository,
                            PaymentRepository paymentRepository,
                            OrderStatusHistoryRepository historyRepository,
                            AccountRepository accountRepository,
                            CodeGeneratorService codeGenerator,
                            FileStorageService fileStorage,
                            ShopProperties shopProperties,
                            QrCodeService qrCodeService,
                            StockMovementService stockMovementService,
                            PaymentGatewayService paymentGatewayService,
                            AuditLogService auditLogService,
                            CouponService couponService,
                            WarrantyService warrantyService,
                            EmailService emailService,
                            NotificationSseService notificationSseService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.laptopRepository = laptopRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.accountRepository = accountRepository;
        this.codeGenerator = codeGenerator;
        this.fileStorage = fileStorage;
        this.shopProperties = shopProperties;
        this.qrCodeService = qrCodeService;
        this.stockMovementService = stockMovementService;
        this.paymentGatewayService = paymentGatewayService;
        this.auditLogService = auditLogService;
        this.couponService = couponService;
        this.warrantyService = warrantyService;
        this.emailService = emailService;
        this.notificationSseService = notificationSseService;
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

        // Gộp trùng laptopId & sắp xếp tăng dần theo laptopId để khóa PESSIMISTIC_WRITE chống deadlock
        Map<Integer, Integer> mergedQtyByLaptop = new TreeMap<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getLaptopId() == null) {
                throw new BusinessException("Thiếu mã sản phẩm trong đơn hàng.");
            }
            Integer qty = itemReq.getQuantity();
            if (qty == null || qty <= 0) {
                throw new BusinessException("Số lượng sản phẩm phải lớn hơn 0.");
            }
            mergedQtyByLaptop.merge(itemReq.getLaptopId(), qty, Integer::sum);
        }
        if (mergedQtyByLaptop.isEmpty()) {
            throw new BusinessException("Đơn hàng phải chứa ít nhất 1 sản phẩm laptop.");
        }

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

        BigDecimal subtotal = BigDecimal.ZERO;
        List<StockMovement> pendingMovements = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : mergedQtyByLaptop.entrySet()) {
            Integer laptopId = entry.getKey();
            int qty = entry.getValue();
            log.info("[ORDER | THREAD: {}] Khóa PESSIMISTIC_WRITE laptop ID={}", threadName, laptopId);
            Laptop laptop = laptopRepository.findByIdForUpdate(laptopId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + laptopId));
            if (!Boolean.TRUE.equals(laptop.getIsActive())) {
                throw new BusinessException("Sản phẩm '" + laptop.getName() + "' hiện đã ngừng kinh doanh.");
            }
            int currentStock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
            if (currentStock < qty) {
                throw new BusinessException(String.format(
                        "Sản phẩm '%s' (SKU: %s) không đủ tồn kho (Yêu cầu: %d, Hiện còn: %d)",
                        laptop.getName(), laptop.getSku(), qty, currentStock));
            }
            int newStock = currentStock - qty;
            laptop.setStockQuantity(newStock);
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);

            pendingMovements.add(new StockMovement(laptop, StockMovementType.SALE, -qty,
                    currentStock, newStock, null, null, createdByAccountId));

            BigDecimal unitPrice = laptop.getSalePrice().setScale(2, RoundingMode.HALF_UP);
            OrderDetail detail = new OrderDetail(order, laptop, qty, unitPrice);
            order.addDetail(detail);
            subtotal = subtotal.add(detail.getLineTotal());
        }

        final BigDecimal cleanSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount;
        String cleanCoupon = (request.getCouponCode() != null && !request.getCouponCode().isBlank())
                ? request.getCouponCode().trim().toUpperCase() : null;
        if (cleanCoupon != null) {
            discount = couponService.consumeCoupon(cleanCoupon, cleanSubtotal);
            order.setCouponCode(cleanCoupon);
        } else {
            discount = request.getDiscount() != null
                    ? request.getDiscount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            if (discount.signum() < 0) {
                throw new BusinessException("Giảm giá không được âm.");
            }
            if (discount.compareTo(cleanSubtotal) > 0) {
                throw new BusinessException("Giảm giá không được vượt quá tạm tính đơn hàng.");
            }
            if (discount.signum() > 0 && createdByAccountId != null) {
                accountRepository.findById(createdByAccountId).ifPresent(acc -> {
                    String role = acc.getRole() != null ? acc.getRole().getRoleName() : "";
                    if ("STAFF".equalsIgnoreCase(role)) {
                        BigDecimal staffLimit = cleanSubtotal.multiply(STAFF_MAX_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
                        if (discount.compareTo(staffLimit) > 0) {
                            throw new BusinessException("Nhân viên chỉ được giảm giá tối đa 5% giá trị đơn hàng.");
                        }
                    }
                });
            }
        }

        BigDecimal shippingFee = request.getShippingFee() != null && request.getShippingFee().signum() > 0
                ? request.getShippingFee().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal taxable = cleanSubtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal vatAmount = taxable.multiply(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(110), 0, RoundingMode.HALF_UP);

        String orderCode = codeGenerator.nextOrderCode();
        order.setOrderCode(orderCode);

        BigDecimal totalAmount = taxable.add(shippingFee).setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(cleanSubtotal);
        order.setDiscount(discount);
        order.setShippingFee(shippingFee);
        order.setVatAmount(vatAmount);
        order.setTotalAmount(totalAmount);

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
        pendingMovements.forEach(m -> stockMovementService.record(
                m.getLaptop(), m.getMovementType(), m.getQuantityDelta(),
                m.getStockBefore(), m.getStockAfter(), orderCode, "Xuất bán đơn hàng " + orderCode, m.getActorAccountId()));

        auditLogService.logAction(createdByAccountId, null, "PLACE_ORDER", "Order", saved.getOrderCode(),
                "Đặt đơn " + saved.getOrderCode() + ", tổng " + totalAmount + " (" + method + ")");
        if (customer.getEmail() != null) {
            emailService.sendEmail(customer.getEmail(),
                    "Xác nhận tiếp nhận đơn hàng " + saved.getOrderCode(),
                    "Cảm ơn bạn đã đặt hàng tại NEXORA LAPTOP. Tổng thanh toán: " + totalAmount + " đ.");
        }
        notificationSseService.broadcast("NEW_ORDER",
                "Đơn hàng mới " + saved.getOrderCode(),
                recipientName + " - " + totalAmount.setScale(0, RoundingMode.HALF_UP) + " đ",
                "/admin/orders/" + saved.getId());
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
            if (newStatus == OrderStatus.RETURNED && current != OrderStatus.SHIPPING) {
                throw new BusinessException("Chỉ đơn hàng đang ở trạng thái 'Đang giao hàng' mới có thể chuyển sang 'Đã trả hàng'. Hãy dùng 'Đã hủy' nếu chưa xuất kho giao.");
            }
            if (newStatus == OrderStatus.CANCELLED && current == OrderStatus.SHIPPING) {
                throw new BusinessException("Đơn đang giao hàng không thể hủy trực tiếp; hãy chuyển sang 'Đã trả hàng' nếu giao không thành công.");
            }
            if (bank && payStatus == PaymentStatus.PAID) {
                if (newStatus == OrderStatus.RETURNED && current == OrderStatus.SHIPPING) {
                    order.setStatus(OrderStatus.REFUND_REQUESTED);
                    order.setRefundReason("Giao không thành công / Trả hàng - chờ hoàn tiền chuyển khoản");
                    order.setRefundRequestedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    addHistory(order, current, OrderStatus.REFUND_REQUESTED,
                            "Khách trả hàng khi đang giao (đã thanh toán CK) -> chuyển chờ hoàn tiền", changedByAccountId);
                    auditLogService.logAction(changedByAccountId, null, "ORDER_RETURN_REFUND", "Order", order.getOrderCode(),
                            "Chuyển trả hàng có hoàn tiền");
                    return orderRepository.save(order);
                }
                throw new BusinessException("Đơn đã thanh toán chuyển khoản. Hãy xử lý qua luồng hoàn tiền.");
            }
            if (bank && payStatus == PaymentStatus.PENDING_CONFIRMATION) {
                throw new BusinessException("Đơn đang chờ xác nhận thanh toán. Hãy xác nhận hoặc từ chối ảnh minh chứng trước.");
            }
            String reason = newStatus == OrderStatus.RETURNED ? "Giao không thành công / Trả hàng" : "Hủy bởi nhân viên";
            return cancelOrReturn(order, newStatus, reason, changedByAccountId,
                    "Cập nhật trạng thái từ " + current.name() + " sang " + newStatus.name());
        }

        if (isBackwardTransition(current, newStatus)) {
            throw new BusinessException("Không thể chuyển ngược trạng thái đơn hàng từ '"
                    + current.getDisplayName() + "' về '" + newStatus.getDisplayName() + "'.");
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
            warrantyService.allocateSerialsForCompletedOrder(order);
        }
        addHistory(order, current, newStatus,
                "Cập nhật trạng thái từ " + current.name() + " sang " + newStatus.name(), changedByAccountId);
        auditLogService.logAction(changedByAccountId, null, "UPDATE_ORDER_STATUS", "Order", order.getOrderCode(),
                current.name() + " -> " + newStatus.name());
        Order saved = orderRepository.save(order);
        if (saved.getCustomer() != null && saved.getCustomer().getEmail() != null) {
            emailService.sendEmail(saved.getCustomer().getEmail(),
                    "Cập nhật đơn hàng " + saved.getOrderCode() + ": " + newStatus.getDisplayName(),
                    "Đơn hàng " + saved.getOrderCode() + " đã chuyển sang trạng thái: " + newStatus.getDisplayName()
                            + (saved.getTrackingCode() != null ? " (Mã vận đơn: " + saved.getTrackingCode() + ")" : ""));
        }
        return saved;
    }

    private static boolean isBackwardTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CONFIRMED -> to == OrderStatus.PENDING;
            case PREPARING -> to == OrderStatus.PENDING || to == OrderStatus.CONFIRMED;
            case SHIPPING -> to == OrderStatus.PENDING || to == OrderStatus.CONFIRMED || to == OrderStatus.PREPARING;
            default -> false;
        };
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
        if (!paymentGatewayService.isDemoMode()) {
            throw new BusinessException("Hệ thống đang chạy chế độ vận hành thật (PRODUCTION): "
                    + "chỉ Webhook ngân hàng hoặc nhân viên kiểm tra minh chứng mới được xác nhận thanh toán.");
        }
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
                "Khách hàng xác nhận đã chuyển khoản thành công qua VietQR (DEMO)", accountId);
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
        if (!qrCodeService.verifyToken(order, payment, token)) {
            throw new BusinessException("Liên kết QR thanh toán không hợp lệ hoặc đã thay đổi.");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment confirmBankTransferByQrToken(Integer orderId, String token) {
        if (!paymentGatewayService.isDemoMode()) {
            throw new BusinessException("Hệ thống đang chạy chế độ vận hành thật (PRODUCTION): "
                    + "vui lòng chờ Webhook ngân hàng tự động hoặc nhân viên duyệt minh chứng.");
        }
        Order order = lockOrder(orderId);
        Payment payment = latestPayment(orderId);
        if (!qrCodeService.verifyToken(order, payment, token)) {
            throw new BusinessException("Mã QR xác nhận không hợp lệ hoặc đã hết hạn.");
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
                "Khách hàng quét mã QR trên điện thoại và xác nhận đã chuyển khoản (DEMO)", customerAccountId);
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

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (oldFile != null && !oldFile.equals(stored)) {
                        fileStorage.deleteQuietly(oldFile);
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        fileStorage.deleteQuietly(stored);
                    }
                }
            });
        }

        payment.setProofImage(stored);
        payment.setProofUploadedAt(LocalDateTime.now());
        payment.setRejectReason(null);
        payment.setPaymentStatus(PaymentStatus.PENDING_CONFIRMATION);
        paymentRepository.save(payment);
        addHistory(order, order.getStatus(), order.getStatus(), "Khách tải ảnh minh chứng chuyển khoản", accountId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorage.deleteQuietly(oldFile);
        }
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
        auditLogService.logAction(staffAccountId, null, "CONFIRM_PAYMENT", "Order", order.getOrderCode(),
                "Xác nhận thanh toán chuyển khoản");
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
        auditLogService.logAction(staffAccountId, null, "REJECT_PAYMENT_PROOF", "Order", order.getOrderCode(),
                cleanReason);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order confirmRefund(Integer orderId, Integer staffAccountId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("Đơn hàng không ở trạng thái 'Yêu cầu hoàn tiền'.");
        }
        restoreStock(order, StockMovementType.RETURN_RESTORE, staffAccountId,
                "Hoàn kho do hoàn tiền đơn " + order.getOrderCode());
        if (order.getCouponCode() != null) {
            couponService.releaseCoupon(order.getCouponCode());
        }
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
        auditLogService.logAction(staffAccountId, null, "CONFIRM_REFUND", "Order", order.getOrderCode(),
                "Xác nhận hoàn tiền và hoàn kho");
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
        StockMovementType movementType = newStatus == OrderStatus.RETURNED
                ? StockMovementType.RETURN_RESTORE : StockMovementType.CANCEL_RESTORE;
        restoreStock(order, movementType, by, reason);
        if (order.getCouponCode() != null) {
            couponService.releaseCoupon(order.getCouponCode());
        }
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
        auditLogService.logAction(by, null, "ORDER_" + newStatus.name(), "Order", order.getOrderCode(), reason);
        return orderRepository.save(order);
    }

    private void restoreStock(Order order, StockMovementType movementType, Integer actorId, String reason) {
        List<OrderDetail> sorted = order.getDetails().stream()
                .filter(d -> d.getLaptop() != null)
                .sorted(Comparator.comparing((OrderDetail d) -> d.getLaptop().getId()))
                .toList();
        for (OrderDetail detail : sorted) {
            Laptop laptop = laptopRepository.findByIdForUpdate(detail.getLaptop().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Laptop ID: " + detail.getLaptop().getId()));
            int oldStock = laptop.getStockQuantity() != null ? laptop.getStockQuantity() : 0;
            int qty = detail.getQuantity();
            int newStock = oldStock + qty;
            laptop.setStockQuantity(newStock);
            laptop.setUpdatedAt(LocalDateTime.now());
            laptopRepository.save(laptop);
            stockMovementService.record(laptop, movementType, qty, oldStock, newStock,
                    order.getOrderCode(), reason, actorId);
        }
    }

    private void addHistory(Order order, OrderStatus from, OrderStatus to, String note, Integer by) {
        OrderStatusHistory h = new OrderStatusHistory(order, from, to, abbreviate(note, 500));
        h.setChangedBy(by);
        historyRepository.save(h);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateShippingInfo(Integer orderId, String carrierName, String trackingCode, Integer actorId) {
        Order order = lockOrder(orderId);
        order.setCarrierName(carrierName != null ? carrierName.trim() : null);
        order.setTrackingCode(trackingCode != null ? trackingCode.trim() : null);
        order.setUpdatedAt(LocalDateTime.now());
        addHistory(order, order.getStatus(), order.getStatus(),
                "Cập nhật vận chuyển: " + abbreviate(carrierName, 50) + " / " + abbreviate(trackingCode, 50), actorId);
        return orderRepository.save(order);
    }

    private static String abbreviate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
