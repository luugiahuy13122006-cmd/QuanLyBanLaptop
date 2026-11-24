package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DonHang")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDonHang")
    private Integer id;

    @Column(name = "MaCodeDonHang", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKhachHang")
    private Customer customer;

    @Column(name = "NguoiTao")
    private Integer createdBy;

    @Column(name = "TenNguoiNhan", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "SoDienThoaiNhan", nullable = false, length = 15)
    private String recipientPhone;

    @Column(name = "DiaChiGiaoHang", nullable = false, length = 300)
    private String deliveryAddress;

    @Column(name = "NgayDatHang", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiDonHang", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "TamTinh", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "GiamGia", nullable = false, precision = 18, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "PhiVanChuyen", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "ThueVAT", nullable = false, precision = 18, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "MaGiamGia", length = 30)
    private String couponCode;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Version
    @Column(name = "PhienBan", nullable = false)
    private Integer version = 0;

    @Column(name = "GhiChu", length = 500)
    private String note;

    @Column(name = "NgayCapNhat")
    private LocalDateTime updatedAt;

    @Column(name = "HanThanhToan")
    private LocalDateTime paymentExpiresAt;

    @Column(name = "LyDoHuy", length = 500)
    private String cancelReason;

    @Column(name = "LyDoHoanTien", length = 500)
    private String refundReason;

    @Column(name = "NganHangHoan", length = 100)
    private String refundBankName;

    @Column(name = "SoTaiKhoanHoan", length = 30)
    private String refundAccountNumber;

    @Column(name = "TenChuTaiKhoanHoan", length = 100)
    private String refundAccountHolder;

    @Column(name = "NgayYeuCauHoan")
    private LocalDateTime refundRequestedAt;

    @Column(name = "NgayHoanTien")
    private LocalDateTime refundedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    public Order() {}

    @PrePersist
    protected void onCreate() {
        if (this.orderDate == null) this.orderDate = LocalDateTime.now();
        if (this.status == null) this.status = OrderStatus.PENDING;
        if (this.subtotal == null) this.subtotal = BigDecimal.ZERO;
        if (this.discount == null) this.discount = BigDecimal.ZERO;
        if (this.shippingFee == null) this.shippingFee = BigDecimal.ZERO;
        if (this.vatAmount == null) this.vatAmount = BigDecimal.ZERO;
        if (this.totalAmount == null) this.totalAmount = BigDecimal.ZERO;
        if (this.version == null) this.version = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addDetail(OrderDetail detail) {
        details.add(detail);
        detail.setOrder(this);
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistory history) {
        statusHistories.add(history);
        history.setOrder(this);
    }

    public Payment getLatestPayment() {
        if (payments == null || payments.isEmpty()) return null;
        return payments.get(payments.size() - 1);
    }

    public PaymentMethod getPaymentMethod() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getPaymentMethod() : PaymentMethod.BANK_TRANSFER;
    }

    public PaymentStatus getPaymentStatus() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getPaymentStatus() : PaymentStatus.UNPAID;
    }

    public String getTransactionCode() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getTransactionCode() : "";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrderId() { return id; }
    public void setOrderId(Integer orderId) { this.id = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getShippingAddress() { return deliveryAddress; }
    public void setShippingAddress(String shippingAddress) { this.deliveryAddress = shippingAddress; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public OrderStatus getOrderStatus() { return status; }
    public void setOrderStatus(OrderStatus orderStatus) { this.status = orderStatus; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getPaymentExpiresAt() { return paymentExpiresAt; }
    public void setPaymentExpiresAt(LocalDateTime paymentExpiresAt) { this.paymentExpiresAt = paymentExpiresAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundBankName() { return refundBankName; }
    public void setRefundBankName(String refundBankName) { this.refundBankName = refundBankName; }

    public String getRefundAccountNumber() { return refundAccountNumber; }
    public void setRefundAccountNumber(String refundAccountNumber) { this.refundAccountNumber = refundAccountNumber; }

    public String getRefundAccountHolder() { return refundAccountHolder; }
    public void setRefundAccountHolder(String refundAccountHolder) { this.refundAccountHolder = refundAccountHolder; }

    public LocalDateTime getRefundRequestedAt() { return refundRequestedAt; }
    public void setRefundRequestedAt(LocalDateTime refundRequestedAt) { this.refundRequestedAt = refundRequestedAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public List<OrderDetail> getDetails() { return details; }
    public List<OrderDetail> getOrderDetails() { return details; }
    public void setDetails(List<OrderDetail> details) { this.details = details; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public List<OrderStatusHistory> getStatusHistories() { return statusHistories; }
    public void setStatusHistories(List<OrderStatusHistory> statusHistories) { this.statusHistories = statusHistories; }
}
