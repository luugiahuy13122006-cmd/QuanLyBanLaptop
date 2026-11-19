package com.laptopshop.repository;

// Thành viên phụ trách: TV4
// Repository thao tác dữ liệu bảng Payments (Thanh toán) trong CSDL QL_Laptop.

import com.laptopshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByOrderIdOrderByIdAsc(Integer orderId);
}
