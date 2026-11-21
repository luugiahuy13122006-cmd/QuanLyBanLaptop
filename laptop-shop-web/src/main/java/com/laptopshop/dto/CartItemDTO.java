package com.laptopshop.dto;

// Thành viên phụ trách: TV3 - Trương Xuân Đạt
// DTO biểu diễn một dòng sản phẩm trong giỏ hàng Session của khách hàng.

import com.laptopshop.entity.Laptop;
import java.math.BigDecimal;

public class CartItemDTO {
    private Laptop laptop;
    private Integer quantity;
    private BigDecimal subtotal;

    public CartItemDTO(Laptop laptop, Integer quantity) {
        this.laptop = laptop;
        this.quantity = quantity;
        this.subtotal = laptop.getSalePrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Laptop getLaptop() { return laptop; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
