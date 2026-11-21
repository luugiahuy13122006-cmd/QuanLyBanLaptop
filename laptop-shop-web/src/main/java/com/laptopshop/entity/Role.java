package com.laptopshop.entity;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import jakarta.persistence.*;

@Entity
@Table(name = "VaiTro")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaVaiTro")
    private Integer id;

    @Column(name = "TenVaiTro", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "MoTa", length = 255)
    private String description;

    public Role() {
    }

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
