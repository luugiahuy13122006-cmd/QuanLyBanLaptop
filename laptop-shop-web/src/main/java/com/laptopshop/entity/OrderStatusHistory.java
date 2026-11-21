package com.laptopshop.entity;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LichSuTrangThaiDonHang")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLichSu")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiCu", length = 20)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiMoi", nullable = false, length = 20)
    private OrderStatus newStatus;

    @Column(name = "NguoiThayDoi")
    private Integer changedBy;

    @Column(name = "GhiChuThayDoi", length = 500)
    private String changeNote;

    @Column(name = "ThoiGianThayDoi", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public OrderStatusHistory() {
    }

    public OrderStatusHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changeNote) {
        this.order = order;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changeNote = changeNote;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) this.changedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Integer getHistoryId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public OrderStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(OrderStatus oldStatus) { this.oldStatus = oldStatus; }

    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }

    public Integer getChangedBy() { return changedBy; }
    public void setChangedBy(Integer changedBy) { this.changedBy = changedBy; }

    public String getChangeNote() { return changeNote; }
    public String getNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
