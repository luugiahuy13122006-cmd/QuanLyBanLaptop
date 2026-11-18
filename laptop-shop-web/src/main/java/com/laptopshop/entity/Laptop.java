package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Laptops")
public class Laptop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LaptopID")
    private Integer id;

    @Column(name = "SKU", nullable = false, unique = true, length = 30)
    private String sku;

    @Column(name = "LaptopName", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BrandID", nullable = false)
    private Brand brand;

    @Column(name = "CPU", length = 150)
    private String cpu;

    @Column(name = "RAM_GB")
    private Integer ramGb;

    @Column(name = "Storage_GB")
    private Integer storageGb;

    @Column(name = "GPU", length = 150)
    private String gpu;

    @Column(name = "ScreenSize", precision = 4, scale = 1)
    private BigDecimal screenSize;

    @Column(name = "OperatingSystem", length = 100)
    private String operatingSystem;

    @Column(name = "Color", length = 50)
    private String color;

    @Column(name = "PurchasePrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "SalePrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "ImageURL", length = 500)
    private String imageUrl;

    // NVARCHAR(MAX) cho SQL Server (không dùng TEXT)
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    public Laptop() {}

    // ======== Getters & Setters ========
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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

    // Helpers cho Socket protocol
    public String getBrandName() { return brand == null ? "" : brand.getName(); }
    public String getCategoryName() { return category == null ? "" : category.getName(); }
    public BigDecimal getPrice() { return salePrice; }
    public Integer getStock() { return stockQuantity; }
    public String getRam() { return ramGb == null ? "" : ramGb + " GB"; }
    public String getStorage() { return storageGb == null ? "" : storageGb + " GB"; }
}
