package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GioHangChiTiet", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"MaTaiKhoan", "MaLaptop"})
})
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietGio")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "MaLaptop", nullable = false)
    private Laptop laptop;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity = 1;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CartItemEntity() {}

    public CartItemEntity(Account account, Laptop laptop, Integer quantity) {
        this.account = account;
        this.laptop = laptop;
        this.quantity = quantity != null ? quantity : 1;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
