package com.laptopshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietPhieuNhap")
public class ImportReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietPhieuNhap")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaPhieuNhap", nullable = false)
    private ImportReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaLaptop", nullable = false)
    private Laptop laptop;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "DonGiaNhap", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "ThanhTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    public ImportReceiptDetail() {}

    public ImportReceiptDetail(Laptop laptop, Integer quantity, BigDecimal unitCost) {
        this.laptop = laptop;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineTotal = unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ImportReceipt getReceipt() { return receipt; }
    public void setReceipt(ImportReceipt receipt) { this.receipt = receipt; }
    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
