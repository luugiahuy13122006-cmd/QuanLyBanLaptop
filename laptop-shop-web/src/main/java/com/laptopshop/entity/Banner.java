package com.laptopshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "BannerQuangCao")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBanner")
    private Integer id;

    @Column(name = "TieuDe", nullable = false, length = 150)
    private String title;

    @Column(name = "TieuDePhu", length = 300)
    private String subtitle;

    @Column(name = "DuongDanAnh", length = 300)
    private String imageUrl;

    @Column(name = "LienKet", length = 300)
    private String targetUrl = "/";

    @Column(name = "NhanNut", length = 60)
    private String ctaLabel = "Khám phá ngay";

    @Column(name = "ThuTu", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "HienThi", nullable = false)
    private Boolean active = true;

    public Banner() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getCtaLabel() { return ctaLabel; }
    public void setCtaLabel(String ctaLabel) { this.ctaLabel = ctaLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
