package com.laptopshop.repository;

// Thành viên phụ trách: TV4
// Repository thao tác dữ liệu bảng Orders và thực hiện các truy vấn thống kê doanh thu bằng JPQL khớp QL_LAPTOP.sql.

import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Đếm số đơn hàng của một khách hàng (kiểm tra ràng buộc FK_Orders_Customers trước khi xóa khách hàng)
    long countByCustomerId(Integer customerId);

    boolean existsByOrderCodeIgnoreCase(String orderCode);

    // Lọc danh sách đơn hàng theo trạng thái (OrderStatus) và khách hàng (CustomerID) kèm phân trang
    @EntityGraph(attributePaths = {"customer", "payments"})
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:customerId IS NULL OR o.customer.id = :customerId)")
    Page<Order> filterOrders(@Param("status") OrderStatus status,
                             @Param("customerId") Integer customerId,
                             Pageable pageable);

    // Lấy thông tin đơn hàng kèm khách hàng
    @EntityGraph(attributePaths = {"customer"})
    Optional<Order> findWithCustomerById(Integer id);

    // 1. Thống kê tổng doanh thu: các đơn hàng đã hoàn thành (COMPLETED) hoặc đã thanh toán (PAID) và không bị hủy/trả
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE (o.status = com.laptopshop.entity.OrderStatus.COMPLETED " +
           "   OR EXISTS (SELECT p FROM Payment p WHERE p.order = o AND p.paymentStatus = com.laptopshop.entity.PaymentStatus.PAID)) " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.CANCELLED " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.RETURNED")
    BigDecimal calculateTotalRevenue();

    // 2. Thống kê doanh thu theo từng tháng của năm (khớp truy vấn Thống kê doanh thu theo tháng trong QL_LAPTOP.sql)
    @Query("SELECT new com.laptopshop.dto.MonthlyRevenueDTO(" +
           "MONTH(o.orderDate), COUNT(o), COALESCE(SUM(o.totalAmount), 0)) " +
           "FROM Order o WHERE YEAR(o.orderDate) = :year " +
           "AND (o.status = com.laptopshop.entity.OrderStatus.COMPLETED " +
           "   OR EXISTS (SELECT p FROM Payment p WHERE p.order = o AND p.paymentStatus = com.laptopshop.entity.PaymentStatus.PAID)) " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.CANCELLED " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.RETURNED " +
           "GROUP BY MONTH(o.orderDate) " +
           "ORDER BY MONTH(o.orderDate)")
    List<MonthlyRevenueDTO> getMonthlyRevenueByYear(@Param("year") int year);

    // 3. Thống kê số lượng đơn hàng theo từng trạng thái đơn hàng
    @Query("SELECT new com.laptopshop.dto.OrderStatusCountDTO(o.status, COUNT(o)) " +
           "FROM Order o GROUP BY o.status")
    List<OrderStatusCountDTO> countOrdersByStatus();
}
