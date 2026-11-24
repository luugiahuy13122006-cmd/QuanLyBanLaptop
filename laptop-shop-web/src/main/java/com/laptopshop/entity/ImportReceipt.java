package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PhieuNhap")
public class ImportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuNhap")
    private Integer id;

    @Column(name = "MaCodePhieuNhap", nullable = false, unique = true, length = 30)
    private String receiptCode;

    @Column(name = "NhaCungCap", nullable = false, length = 150)
    private String supplier;

    @Column(name = "GhiChu", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 20)
    private ImportReceiptStatus status = ImportReceiptStatus.PENDING;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NguoiTao", nullable = false)
    private Account createdBy;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiDuyet")
    private Account approvedBy;

    @Column(name = "NgayDuyet")
    private LocalDateTime approvedAt;

    @Column(name = "LyDoTuChoi", length = 500)
    private String rejectReason;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportReceiptDetail> details = new ArrayList<>();

    public ImportReceipt() {}

    public void addDetail(ImportReceiptDetail detail) {
        details.add(detail);
        detail.setReceipt(this);
    }

    public void recalculateTotal() {
        this.totalAmount = details.stream()
                .map(ImportReceiptDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public ImportReceiptStatus getStatus() { return status; }
    public void setStatus(ImportReceiptStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Account getCreatedBy() { return createdBy; }
    public void setCreatedBy(Account createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Account getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Account approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public List<ImportReceiptDetail> getDetails() { return details; }
    public void setDetails(List<ImportReceiptDetail> details) { this.details = details; }
}
