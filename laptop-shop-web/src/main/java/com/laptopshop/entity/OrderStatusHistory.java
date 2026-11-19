package com.laptopshop.entity;

// Thành viên phụ trách: TV4
// Thực thể đại diện cho bảng OrderStatusHistory (Lịch sử thay đổi trạng thái đơn hàng) trong CSDL QL_Laptop (SQL Server - TV1).

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OrderStatusHistory")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "OldStatus", length = 20)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "NewStatus", nullable = false, length = 20)
    private OrderStatus newStatus;

    @Column(name = "ChangedBy")
    private Integer changedBy;

    @Column(name = "ChangeNote", length = 500)
    private String changeNote;

    @Column(name = "ChangedAt", nullable = false)
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
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public Integer getHistoryId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(OrderStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public String getNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
