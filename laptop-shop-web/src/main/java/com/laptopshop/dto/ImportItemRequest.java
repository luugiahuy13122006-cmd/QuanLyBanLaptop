package com.laptopshop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ImportItemRequest {

    @NotNull(message = "Vui lòng chọn laptop")
    private Integer laptopId;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 1, message = "Số lượng nhập tối thiểu là 1")
    @Max(value = 10000, message = "Số lượng nhập tối đa là 10.000")
    private Integer quantity;

    @NotNull(message = "Vui lòng nhập đơn giá nhập")
    @DecimalMin(value = "0.0", message = "Đơn giá nhập không được âm")
    @Digits(integer = 16, fraction = 2, message = "Đơn giá nhập không hợp lệ")
    private BigDecimal unitCost;

    public Integer getLaptopId() { return laptopId; }
    public void setLaptopId(Integer laptopId) { this.laptopId = laptopId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
