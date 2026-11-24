package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NhatKyHeThong")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNhatKy")
    private Long id;

    @Column(name = "MaTaiKhoan")
    private Integer actorAccountId;

    @Column(name = "TenDangNhap", length = 50)
    private String actorUsername;

    @Column(name = "HanhDong", nullable = false, length = 60)
    private String action;

    @Column(name = "DoiTuong", nullable = false, length = 60)
    private String entityType;

    @Column(name = "MaDoiTuong", length = 60)
    private String entityId;

    @Column(name = "MoTa", length = 1000)
    private String details;

    @Column(name = "DiaChiIP", length = 64)
    private String ipAddress;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public AuditLog(Integer actorAccountId, String actorUsername, String action,
                    String entityType, String entityId, String details, String ipAddress) {
        this.actorAccountId = actorAccountId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getActorAccountId() { return actorAccountId; }
    public void setActorAccountId(Integer actorAccountId) { this.actorAccountId = actorAccountId; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
