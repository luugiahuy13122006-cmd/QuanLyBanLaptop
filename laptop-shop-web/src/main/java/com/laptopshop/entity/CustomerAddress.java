package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DiaChiKhachHang")
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDiaChi")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKhachHang", nullable = false)
    private Customer customer;

    @Column(name = "TenNguoiNhan", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String phone;

    @Column(name = "TinhThanh", length = 100)
    private String province;

    @Column(name = "QuanHuyen", length = 100)
    private String district;

    @Column(name = "PhuongXa", length = 100)
    private String ward;

    @Column(name = "DiaChiChiTiet", nullable = false, length = 300)
    private String addressLine;

    @Column(name = "MacDinh", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomerAddress() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.isDefault == null) this.isDefault = false;
    }

    public String getFullAddressText() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.isBlank()) sb.append(addressLine.trim());
        if (ward != null && !ward.isBlank()) sb.append(", ").append(ward.trim());
        if (district != null && !district.isBlank()) sb.append(", ").append(district.trim());
        if (province != null && !province.isBlank()) sb.append(", ").append(province.trim());
        return sb.toString();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
