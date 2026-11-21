package com.laptopshop.entity;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
// Thực thể ánh xạ bảng Laptop trong CSDL QL_Laptop (cột tiếng Việt, giữ chữ Laptop).

import jakarta.persistence.*;
import org.springframework.core.io.ClassPathResource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "Laptop")
public class Laptop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLaptop")
    private Integer id;

    @Column(name = "MaSKU", nullable = false, unique = true, length = 30)
    private String sku;

    @Column(name = "TenLaptop", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaDanhMuc", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaThuongHieu", nullable = false)
    private Brand brand;

    @Column(name = "CPU", length = 150)
    private String cpu;

    @Column(name = "RAM_GB")
    private Integer ramGb;

    @Column(name = "DungLuongSSD_GB")
    private Integer storageGb;

    @Column(name = "CardDoHoa", length = 150)
    private String gpu;

    @Column(name = "KichThuocManHinh", precision = 4, scale = 1)
    private BigDecimal screenSize;

    @Column(name = "HeDieuHanh", length = 100)
    private String operatingSystem;

    @Column(name = "MauSac", length = 50)
    private String color;

    @Column(name = "GiaNhap", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "GiaNiemYet", nullable = false, precision = 18, scale = 2)
    private BigDecimal listPrice = BigDecimal.ZERO;

    @Column(name = "GiaBan", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "SoLuongTon", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "DuongDanAnh", length = 500)
    private String imageUrl;

    @Column(name = "MoTa", length = 4000)
    private String description;

    @Column(name = "TrangThaiHoatDong", nullable = false)
    private Boolean isActive = true;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "NgayCapNhat")
    private LocalDateTime updatedAt;

    public Laptop() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.isActive == null) this.isActive = true;
        if (this.purchasePrice == null) this.purchasePrice = BigDecimal.ZERO;
        if (this.listPrice == null || this.listPrice.compareTo(BigDecimal.ZERO) <= 0) {
            this.listPrice = this.salePrice != null ? this.salePrice : BigDecimal.ZERO;
        }
        if (this.stockQuantity == null) this.stockQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getLaptopId() { return id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLaptopName() { return name; }
    public void setLaptopName(String laptopName) { this.name = laptopName; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    public String getCpu() { return cpu; }
    public void setCpu(String cpu) { this.cpu = cpu; }

    public Integer getRamGb() { return ramGb; }
    public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }

    public Integer getStorageGb() { return storageGb; }
    public void setStorageGb(Integer storageGb) { this.storageGb = storageGb; }

    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }

    public BigDecimal getScreenSize() { return screenSize; }
    public void setScreenSize(BigDecimal screenSize) { this.screenSize = screenSize; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helpers cho Socket protocol & giao diện Storefront / Phong Vũ
    public String getBrandName() { return brand == null ? "" : brand.getName(); }
    public String getCategoryName() { return category == null ? "" : category.getName(); }
    public BigDecimal getPrice() { return salePrice; }
    public Integer getStock() { return stockQuantity; }
    public String getRam() { return ramGb == null ? "" : ramGb + " GB"; }
    public String getStorage() { return storageGb == null ? "" : (storageGb >= 1024 ? (storageGb / 1024) + " TB" : storageGb + " GB"); }

    public boolean isDiscounted() {
        return listPrice != null && salePrice != null && listPrice.compareTo(salePrice) > 0;
    }

    public BigDecimal getDiscountAmount() {
        if (!isDiscounted()) return BigDecimal.ZERO;
        return listPrice.subtract(salePrice);
    }

    public int getDiscountPercent() {
        if (!isDiscounted() || listPrice.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return listPrice.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(listPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public String getDisplayImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        if (sku != null && !sku.isBlank()
                && new ClassPathResource("static/images/laptops/" + sku.trim() + "/1.jpg").exists()) {
            return "/images/laptops/" + sku.trim() + "/1.jpg";
        }
        return "/images/laptop-default.svg";
    }
}
