package com.laptopshop.entity;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import jakarta.persistence.*;

@Entity
@Table(name = "ThuongHieu")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThuongHieu")
    private Integer id;

    @Column(name = "TenThuongHieu", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "QuocGia", length = 100)
    private String country;

    @Column(name = "MoTa", length = 500)
    private String description;

    @Column(name = "TrangThaiHoatDong", nullable = false)
    private Boolean isActive = true;

    public Brand() {
    }

    public Brand(String name, String country, String description) {
        this.name = name;
        this.country = country;
        this.description = description;
        this.isActive = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
