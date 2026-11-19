package com.laptopshop.repository;

// Thành viên phụ trách: TV4
// Repository thao tác dữ liệu bảng OrderDetails (Chi tiết đơn hàng) trong CSDL QL_Laptop.

import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    // Truy vấn nghiệp vụ Top 10 laptop bán chạy nhất (khớp truy vấn trong QL_LAPTOP.sql)
    @Query("SELECT new com.laptopshop.dto.TopSellingLaptopDTO(" +
           "d.laptop.id, d.skuSnapshot, d.laptopNameSnapshot, d.laptop.brand.name, SUM(d.quantity), SUM(d.lineTotal)) " +
           "FROM OrderDetail d " +
           "WHERE d.order.status <> com.laptopshop.entity.OrderStatus.CANCELLED " +
           "AND d.order.status <> com.laptopshop.entity.OrderStatus.RETURNED " +
           "GROUP BY d.laptop.id, d.skuSnapshot, d.laptopNameSnapshot, d.laptop.brand.name " +
           "ORDER BY SUM(d.quantity) DESC")
    List<TopSellingLaptopDTO> findTopSellingLaptops(Pageable pageable);
}
