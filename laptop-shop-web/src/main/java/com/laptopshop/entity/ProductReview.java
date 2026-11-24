package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DanhGiaSanPham")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaLaptop", nullable = false)
    private Laptop laptop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKhachHang", nullable = false)
    private Customer customer;

    @Column(name = "SoSao", nullable = false)
    private Integer rating = 5;

    @Column(name = "TieuDe", length = 150)
    private String title;

    @Column(name = "NoiDung", nullable = false, length = 1000)
    private String content;

    @Column(name = "DaMuaHang", nullable = false)
    private Boolean verifiedPurchase = true;

    @Column(name = "DaDuyet", nullable = false)
    private Boolean approved = false;

    @Column(name = "PhanHoiShop", length = 500)
    private String shopReply;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductReview() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.rating == null) this.rating = 5;
        if (this.verifiedPurchase == null) this.verifiedPurchase = true;
        if (this.approved == null) this.approved = false;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getVerifiedPurchase() { return verifiedPurchase; }
    public void setVerifiedPurchase(Boolean verifiedPurchase) { this.verifiedPurchase = verifiedPurchase; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getShopReply() { return shopReply; }
    public void setShopReply(String shopReply) { this.shopReply = shopReply; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
