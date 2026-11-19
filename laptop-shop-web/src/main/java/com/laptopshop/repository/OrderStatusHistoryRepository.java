package com.laptopshop.repository;

// Thành viên phụ trách: TV4
// Repository thao tác dữ liệu bảng OrderStatusHistory (Lịch sử trạng thái đơn hàng) trong CSDL QL_Laptop.

import com.laptopshop.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(Integer orderId);
}
