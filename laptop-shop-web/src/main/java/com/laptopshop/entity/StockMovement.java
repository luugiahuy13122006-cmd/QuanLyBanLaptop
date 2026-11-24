package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BienDongKho")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBienDong")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaLaptop", nullable = false)
    private Laptop laptop;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiBienDong", nullable = false, length = 30)
    private StockMovementType movementType;

    @Column(name = "SoLuongThayDoi", nullable = false)
    private Integer quantityDelta;

    @Column(name = "TonTruoc", nullable = false)
    private Integer stockBefore;

    @Column(name = "TonSau", nullable = false)
    private Integer stockAfter;

    @Column(name = "MaThamChieu", length = 60)
    private String referenceCode;

    @Column(name = "LyDo", length = 500)
    private String reason;

    @Column(name = "NguoiThucHien")
    private Integer actorAccountId;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StockMovement() {}

    public StockMovement(Laptop laptop, StockMovementType movementType, Integer quantityDelta,
                         Integer stockBefore, Integer stockAfter, String referenceCode,
                         String reason, Integer actorAccountId) {
        this.laptop = laptop;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.referenceCode = referenceCode;
        this.reason = reason;
        this.actorAccountId = actorAccountId;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }
    public Integer getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(Integer quantityDelta) { this.quantityDelta = quantityDelta; }
    public Integer getStockBefore() { return stockBefore; }
    public void setStockBefore(Integer stockBefore) { this.stockBefore = stockBefore; }
    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer stockAfter) { this.stockAfter = stockAfter; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getActorAccountId() { return actorAccountId; }
    public void setActorAccountId(Integer actorAccountId) { this.actorAccountId = actorAccountId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
