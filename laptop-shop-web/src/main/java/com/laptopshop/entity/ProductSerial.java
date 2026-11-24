package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SoSerialLaptop")
public class ProductSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSerial")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaLaptop", nullable = false)
    private Laptop laptop;

    @Column(name = "SoSerial", nullable = false, unique = true, length = 60)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaChiTietDonHang")
    private OrderDetail orderDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 25)
    private SerialStatus status = SerialStatus.IN_STOCK;

    @Column(name = "BaoHanhDen")
    private LocalDate warrantyEndDate;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductSerial() {}

    public ProductSerial(Laptop laptop, String serialNumber) {
        this.laptop = laptop;
        this.serialNumber = serialNumber;
        this.status = SerialStatus.IN_STOCK;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = SerialStatus.IN_STOCK;
    }

    public boolean isUnderWarranty() {
        return warrantyEndDate != null && !warrantyEndDate.isBefore(LocalDate.now());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public OrderDetail getOrderDetail() { return orderDetail; }
    public void setOrderDetail(OrderDetail orderDetail) { this.orderDetail = orderDetail; }
    public SerialStatus getStatus() { return status; }
    public void setStatus(SerialStatus status) { this.status = status; }
    public LocalDate getWarrantyEndDate() { return warrantyEndDate; }
    public void setWarrantyEndDate(LocalDate warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
