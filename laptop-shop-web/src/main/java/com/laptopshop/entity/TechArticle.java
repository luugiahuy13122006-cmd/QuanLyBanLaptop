package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BaiVietCongNghe")
public class TechArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBaiViet")
    private Integer id;

    @Column(name = "TieuDe", nullable = false, length = 200)
    private String title;

    @Column(name = "DuongDan", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "TomTat", length = 500)
    private String summary;

    @Column(name = "NoiDung", nullable = false, length = 4000)
    private String content;

    @Column(name = "AnhBia", length = 300)
    private String coverImage;

    @Column(name = "TacGia", length = 100)
    private String author = "NEXORA Editorial";

    @Column(name = "HienThi", nullable = false)
    private Boolean published = true;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "NgayCapNhat")
    private LocalDateTime updatedAt;

    public TechArticle() {}

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.published == null) this.published = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
