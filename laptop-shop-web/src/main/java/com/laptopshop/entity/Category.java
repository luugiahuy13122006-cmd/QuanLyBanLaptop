package com.laptopshop.entity;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DanhMucLaptop")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhMuc")
    private Integer id;

    @Column(name = "TenDanhMuc", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "MoTa", length = 500)
    private String description;

    @Column(name = "TrangThaiHoatDong", nullable = false)
    private Boolean isActive = true;

    @Column(name = "NgayTao", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Category() {
    }

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
        this.isActive = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
