package com.laptopshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "KhoaTacVu")
public class JobLock {

    @Id
    @Column(name = "TenTacVu", nullable = false, length = 80)
    private String jobName;

    @Column(name = "MayChuGiuKhoa", length = 120)
    private String lockedBy;

    @Column(name = "KhoaDenLuc")
    private LocalDateTime lockedUntil;

    @Column(name = "LanChayCuoi")
    private LocalDateTime lastRunAt;

    public JobLock() {}

    public JobLock(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
}
