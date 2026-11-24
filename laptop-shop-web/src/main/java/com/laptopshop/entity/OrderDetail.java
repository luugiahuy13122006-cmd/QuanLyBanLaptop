package com.laptopshop.entity;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietDonHang")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietDonHang")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaLaptop")
    private Laptop laptop;

    @Column(name = "MaSKULuuTru", nullable = false, length = 30)
    private String skuSnapshot;

    @Column(name = "TenLaptopLuuTru", nullable = false, length = 200)
    private String laptopNameSnapshot;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "GiaVonLuuTru", nullable = false, precision = 18, scale = 2)
    private BigDecimal costPriceSnapshot = BigDecimal.ZERO;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "ThanhTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    public OrderDetail() {
    }

    public OrderDetail(Order order, Laptop laptop, Integer quantity, BigDecimal unitPrice) {
        this.order = order;
        this.laptop = laptop;
        this.skuSnapshot = (laptop != null && laptop.getSku() != null) ? laptop.getSku() : "N/A";
        this.laptopNameSnapshot = (laptop != null && laptop.getName() != null) ? laptop.getName() : "N/A";
        this.costPriceSnapshot = (laptop != null && laptop.getPurchasePrice() != null) ? laptop.getPurchasePrice() : BigDecimal.ZERO;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) {
        this.laptop = laptop;
        if (laptop != null) {
            if (this.skuSnapshot == null) this.skuSnapshot = laptop.getSku();
            if (this.laptopNameSnapshot == null) this.laptopNameSnapshot = laptop.getName();
        }
    }

    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String skuSnapshot) { this.skuSnapshot = skuSnapshot; }

    public String getLaptopNameSnapshot() { return laptopNameSnapshot; }
    public void setLaptopNameSnapshot(String laptopNameSnapshot) { this.laptopNameSnapshot = laptopNameSnapshot; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        if (unitPrice != null && this.quantity != null) {
            this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.unitPrice != null && quantity != null) {
            this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public BigDecimal getSubtotal() { return lineTotal; }
    public void setSubtotal(BigDecimal subtotal) { this.lineTotal = subtotal; }

    public BigDecimal getCostPriceSnapshot() { return costPriceSnapshot; }
    public void setCostPriceSnapshot(BigDecimal costPriceSnapshot) { this.costPriceSnapshot = costPriceSnapshot; }
}
