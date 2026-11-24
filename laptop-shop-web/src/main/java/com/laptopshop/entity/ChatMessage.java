package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TinNhanHoTro")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTinNhan")
    private Long id;

    @Column(name = "MaPhienChat", nullable = false, length = 80)
    private String roomKey;

    @Column(name = "TenNguoiGui", nullable = false, length = 100)
    private String senderName;

    @Column(name = "VaiTroNguoiGui", nullable = false, length = 20)
    private String senderRole = "CUSTOMER";

    @Column(name = "NoiDung", nullable = false, length = 1000)
    private String content;

    @Column(name = "DaDoc", nullable = false)
    private Boolean isRead = false;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(String roomKey, String senderName, String senderRole, String content) {
        this.roomKey = roomKey;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.content = content;
        this.isRead = false;
        this.sentAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) this.sentAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomKey() { return roomKey; }
    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
