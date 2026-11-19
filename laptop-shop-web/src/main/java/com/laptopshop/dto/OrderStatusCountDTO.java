package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO chứa số lượng đơn hàng theo từng trạng thái (OrderStatus).

import com.laptopshop.entity.OrderStatus;

public class OrderStatusCountDTO {

    private OrderStatus status;
    private Long count;

    public OrderStatusCountDTO() {
    }

    public OrderStatusCountDTO(OrderStatus status, Long count) {
        this.status = status;
        this.count = count != null ? count : 0L;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
