package com.laptopshop.repository;

import com.laptopshop.entity.ProductSerial;
import com.laptopshop.entity.SerialStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSerialRepository extends JpaRepository<ProductSerial, Integer> {
    @EntityGraph(attributePaths = {"laptop", "orderDetail", "orderDetail.order", "orderDetail.order.customer"})
    Optional<ProductSerial> findBySerialNumberIgnoreCase(String serialNumber);

    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    List<ProductSerial> findByLaptop_IdAndStatusOrderByIdAsc(Integer laptopId, SerialStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"laptop", "orderDetail", "orderDetail.order"})
    List<ProductSerial> findByOrderDetail_Order_OrderCodeIgnoreCase(String orderCode);

    @EntityGraph(attributePaths = {"laptop"})
    Page<ProductSerial> findAllByOrderByIdDesc(Pageable pageable);
}
