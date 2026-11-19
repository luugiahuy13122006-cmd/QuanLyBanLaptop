package com.laptopshop.entity;

// Thành viên phụ trách: TV4
// Thực thể đại diện cho bảng Orders trong cơ sở dữ liệu QL_Laptop (SQL Server - TV1).

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer id;

    @Column(name = "OrderCode", nullable = false, unique = true, length = 30)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID")
    private Customer customer;

    @Column(name = "CreatedBy")
    private Integer createdBy;

    @Column(name = "RecipientName", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "RecipientPhone", nullable = false, length = 15)
    private String recipientPhone;

    @Column(name = "DeliveryAddress", nullable = false, length = 300)
    private String deliveryAddress;

    @Column(name = "OrderDate", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "OrderStatus", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "Subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "Discount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "ShippingFee", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "TotalAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "Note", length = 500)
    private String note;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.subtotal = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.shippingFee = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }
        if (this.discount == null) {
            this.discount = BigDecimal.ZERO;
        }
        if (this.shippingFee == null) {
            this.shippingFee = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
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

    // Lấy bản ghi thanh toán mới nhất của đơn hàng từ bảng Payments
    public Payment getLatestPayment() {
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.get(payments.size() - 1);
    }

    public PaymentMethod getPaymentMethod() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getPaymentMethod() : PaymentMethod.CASH;
    }

    public PaymentStatus getPaymentStatus() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getPaymentStatus() : PaymentStatus.UNPAID;
    }

    public String getTransactionCode() {
        Payment latest = getLatestPayment();
        return latest != null ? latest.getTransactionCode() : "";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return id;
    }

    public void setOrderId(Integer orderId) {
        this.id = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    // Alias tương thích shippingAddress -> deliveryAddress
    public String getShippingAddress() {
        return deliveryAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.deliveryAddress = shippingAddress;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getOrderStatus() {
        return status;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.status = orderStatus;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderDetail> getDetails() {
        return details;
    }

    public List<OrderDetail> getOrderDetails() {
        return details;
    }

    public void setDetails(List<OrderDetail> details) {
        this.details = details;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<OrderStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public void setStatusHistories(List<OrderStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }
}
