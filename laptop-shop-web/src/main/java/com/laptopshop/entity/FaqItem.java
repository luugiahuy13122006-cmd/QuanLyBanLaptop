package com.laptopshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "CauHoiThuongGap")
public class FaqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCauHoi")
    private Integer id;

    @Column(name = "ChuDe", nullable = false, length = 80)
    private String topic;

    @Column(name = "CauHoi", nullable = false, length = 300)
    private String question;

    @Column(name = "CauTraLoi", nullable = false, length = 2000)
    private String answer;

    @Column(name = "ThuTu", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "HienThi", nullable = false)
    private Boolean published = true;

    public FaqItem() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
}
