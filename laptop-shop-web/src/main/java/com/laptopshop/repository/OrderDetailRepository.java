package com.laptopshop.repository;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    long countByLaptopId(Integer laptopId);

    @Query("SELECT new com.laptopshop.dto.TopSellingLaptopDTO(" +
           "d.laptop.id, d.skuSnapshot, d.laptopNameSnapshot, d.laptop.brand.name, SUM(d.quantity), SUM(d.lineTotal)) " +
           "FROM OrderDetail d " +
           "WHERE d.order.status <> com.laptopshop.entity.OrderStatus.CANCELLED " +
           "AND d.order.status <> com.laptopshop.entity.OrderStatus.RETURNED " +
           "GROUP BY d.laptop.id, d.skuSnapshot, d.laptopNameSnapshot, d.laptop.brand.name " +
           "ORDER BY SUM(d.quantity) DESC")
    List<TopSellingLaptopDTO> findTopSellingLaptops(Pageable pageable);
}
