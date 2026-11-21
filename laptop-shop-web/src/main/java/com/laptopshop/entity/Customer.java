package com.laptopshop.entity;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "KhachHang")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKhachHang")
    private Integer id;

    @Column(name = "MaTaiKhoan")
    private Integer accountId;

    @Column(name = "TenKhachHang", nullable = false, length = 100)
    private String customerName;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String phone;

    @Column(name = "DiaChi", length = 300)
    private String address;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Customer() {
    }

    public Customer(String customerName, String email, String phone, String address) {
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.createdAt = LocalDateTime.now();
    }

    public Customer(Integer id, String customerName, String email, String phone, String address) {
        this.id = id;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCustomerId() { return id; }
    public void setCustomerId(Integer customerId) { this.id = customerId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getFullName() { return customerName; }
    public void setFullName(String fullName) { this.customerName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
