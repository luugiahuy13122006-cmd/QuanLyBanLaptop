package com.laptopshop.dto;

import java.math.BigDecimal;

public class MonthlyImportCostDTO {

    private Integer month;
    private Long receiptCount;
    private BigDecimal cost;

    public MonthlyImportCostDTO() {}

    public MonthlyImportCostDTO(Integer month, Long receiptCount, BigDecimal cost) {
        this.month = month;
        this.receiptCount = receiptCount;
        this.cost = cost;
    }

    public Integer getMonth() { return month; }
    public Long getReceiptCount() { return receiptCount; }
    public BigDecimal getCost() { return cost; }
}
