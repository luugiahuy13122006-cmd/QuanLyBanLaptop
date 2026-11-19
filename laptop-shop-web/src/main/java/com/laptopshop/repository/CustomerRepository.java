package com.laptopshop.repository;

// Thành viên phụ trách: TV4
// Repository thao tác dữ liệu khách hàng (Customers) trong CSDL QL_Laptop qua Spring Data JPA.

import com.laptopshop.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // Kiểm tra email khách hàng đã tồn tại (khớp truy vấn kiểm tra trùng email trong QL_LAPTOP.sql)
    boolean existsByEmailIgnoreCase(String email);

    // Kiểm tra email khách hàng trùng với bản ghi khác khi sửa
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    // Kiểm tra số điện thoại khách hàng đã tồn tại
    boolean existsByPhone(String phone);

    // Kiểm tra số điện thoại trùng với bản ghi khác khi sửa
    boolean existsByPhoneAndIdNot(String phone, Integer id);

    // Tìm kiếm khách hàng theo tên (CustomerName), email hoặc số điện thoại có phân trang
    @Query("SELECT c FROM Customer c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    // Tìm khách hàng theo email
    Optional<Customer> findByEmailIgnoreCase(String email);
}
