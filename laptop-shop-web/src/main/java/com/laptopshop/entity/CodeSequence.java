package com.laptopshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "BoDemMa")
public class CodeSequence {

    @Id
    @Column(name = "TenBoDem", length = 30)
    private String name;

    @Column(name = "GiaTriHienTai", nullable = false)
    private Integer currentValue = 0;

    public CodeSequence() {}

    public CodeSequence(String name, Integer currentValue) {
        this.name = name;
        this.currentValue = currentValue;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCurrentValue() { return currentValue; }
    public void setCurrentValue(Integer currentValue) { this.currentValue = currentValue; }
}
