package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThanhToan")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThanhToan")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "PhuongThucThanhToan", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiThanhToan", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "SoTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "MaGiaoDich", length = 100)
    private String transactionCode;

    @Column(name = "ThoiGianThanhToan")
    private LocalDateTime paidAt;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "AnhMinhChung", length = 100)
    private String proofImage;

    @Column(name = "NgayTaiMinhChung")
    private LocalDateTime proofUploadedAt;

    @Column(name = "LyDoTuChoi", length = 500)
    private String rejectReason;

    @Column(name = "NguoiXacNhan")
    private Integer confirmedBy;

    public Payment() {}

    public Payment(Order order, PaymentMethod paymentMethod, PaymentStatus paymentStatus, BigDecimal amount, String transactionCode) {
        this.order = order;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.UNPAID;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.transactionCode = transactionCode;
        this.createdAt = LocalDateTime.now();
        if (this.paymentStatus == PaymentStatus.PAID) {
            this.paidAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.paymentMethod == null) this.paymentMethod = PaymentMethod.BANK_TRANSFER;
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.UNPAID;
        if (this.amount == null) this.amount = BigDecimal.ZERO;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionCode() { return transactionCode; }
    public String getTransactionRef() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProofImage() { return proofImage; }
    public void setProofImage(String proofImage) { this.proofImage = proofImage; }

    public LocalDateTime getProofUploadedAt() { return proofUploadedAt; }
    public void setProofUploadedAt(LocalDateTime proofUploadedAt) { this.proofUploadedAt = proofUploadedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Integer getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Integer confirmedBy) { this.confirmedBy = confirmedBy; }
}
