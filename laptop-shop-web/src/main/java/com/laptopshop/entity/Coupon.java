package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaGiamGia")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGiamGiaId")
    private Integer id;

    @Column(name = "MaCode", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "MoTa", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiGiam", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "GiaTriGiam", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "GiamToiDa", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "DonToiThieu", nullable = false, precision = 18, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "SoLuotToiDa", nullable = false)
    private Integer maxUsage = 100;

    @Column(name = "DaSuDung", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "BatDauLuc")
    private LocalDateTime startsAt;

    @Column(name = "KetThucLuc")
    private LocalDateTime expiresAt;

    @Column(name = "TrangThaiHoatDong", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(name = "PhienBan", nullable = false)
    private Integer version = 0;

    public Coupon() {}

    @PrePersist
    protected void onCreate() {
        if (this.discountType == null) this.discountType = DiscountType.PERCENT;
        if (this.discountValue == null) this.discountValue = BigDecimal.ZERO;
        if (this.minOrderAmount == null) this.minOrderAmount = BigDecimal.ZERO;
        if (this.maxUsage == null) this.maxUsage = 100;
        if (this.usedCount == null) this.usedCount = 0;
        if (this.isActive == null) this.isActive = true;
        if (this.version == null) this.version = 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public Integer getMaxUsage() { return maxUsage; }
    public void setMaxUsage(Integer maxUsage) { this.maxUsage = maxUsage; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
