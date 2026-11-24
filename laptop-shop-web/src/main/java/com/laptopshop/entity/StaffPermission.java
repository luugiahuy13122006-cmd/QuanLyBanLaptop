package com.laptopshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PhanQuyenNhanVien",
       uniqueConstraints = @UniqueConstraint(name = "UX_PhanQuyen_TK_CN", columnNames = {"MaTaiKhoan", "MaChucNang"}))
public class StaffPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhanQuyen")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "MaChucNang", nullable = false, length = 40)
    private AdminModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "QuyenHan", nullable = false, length = 20)
    private PermissionLevel level = PermissionLevel.NONE;

    public StaffPermission() {}

    public StaffPermission(Account account, AdminModule module, PermissionLevel level) {
        this.account = account;
        this.module = module;
        this.level = level != null ? level : PermissionLevel.NONE;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public AdminModule getModule() { return module; }
    public void setModule(AdminModule module) { this.module = module; }
    public PermissionLevel getLevel() { return level; }
    public void setLevel(PermissionLevel level) { this.level = level; }
}
