package com.laptopshop.dto;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LaptopDTO {
    private Integer id;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 30, message = "SKU tối đa 30 ký tự")
    private String sku;

    @NotBlank(message = "Tên laptop không được để trống")
    @Size(max = 200, message = "Tên laptop tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    @NotNull(message = "Vui lòng chọn thương hiệu")
    private Integer brandId;

    @Size(max = 150)
    private String cpu;

    @Positive(message = "RAM phải lớn hơn 0")
    private Integer ramGb;

    @Positive(message = "Dung lượng lưu trữ phải lớn hơn 0")
    private Integer storageGb;

    @Size(max = 150)
    private String gpu;

    @DecimalMin(value = "10.0", message = "Kích thước màn hình tối thiểu 10 inch")
    private BigDecimal screenSize;

    @Size(max = 100)
    private String operatingSystem;

    @Size(max = 50)
    private String color;

    @NotNull(message = "Giá nhập không được để trống")
    @DecimalMin(value = "0.0", message = "Giá nhập không được âm")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @NotNull(message = "Giá niêm yết không được để trống")
    @DecimalMin(value = "0.0", message = "Giá niêm yết không được âm")
    private BigDecimal listPrice = BigDecimal.ZERO;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được âm")
    private BigDecimal salePrice;

    @NotNull(message = "Tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer stockQuantity = 0;

    @Size(max = 500)
    private String imageUrl;

    private String description;
    private Boolean isActive = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getBrandId() { return brandId; }
    public void setBrandId(Integer brandId) { this.brandId = brandId; }
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
}
