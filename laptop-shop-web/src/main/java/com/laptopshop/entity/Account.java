package com.laptopshop.entity;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoan")
    private Integer id;

    @Column(name = "TenDangNhap", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "MatKhauMaHoa", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String fullName;

    @Column(name = "Email", unique = true, length = 100)
    private String email;

    @Column(name = "SoDienThoai", length = 15)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "MaVaiTro", nullable = false)
    private Role role;

    @Column(name = "TrangThaiHoatDong", nullable = false)
    private Boolean isActive = true;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "SoLanDangNhapSai", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "KhoaDenLuc")
    private LocalDateTime lockedUntil;

    @Column(name = "BatBuocDoiMatKhau", nullable = false)
    private Boolean mustChangePassword = false;

    @Version
    @Column(name = "PhienBan", nullable = false)
    private Integer version = 0;

    public Account() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.failedLoginAttempts == null) this.failedLoginAttempts = 0;
        if (this.mustChangePassword == null) this.mustChangePassword = false;
        if (this.version == null) this.version = 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
