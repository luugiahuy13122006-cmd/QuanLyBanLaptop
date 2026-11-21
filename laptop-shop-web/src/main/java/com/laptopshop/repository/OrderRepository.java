package com.laptopshop.repository;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
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

    long countByCustomerId(Integer customerId);
    long countByCreatedBy(Integer createdBy);
    boolean existsByOrderCodeIgnoreCase(String orderCode);

    @EntityGraph(attributePaths = {"customer", "payments"})
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " o.recipientPhone LIKE CONCAT('%', :keyword, '%'))")
    Page<Order> filterOrders(@Param("status") OrderStatus status,
                             @Param("customerId") Integer customerId,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "details"})
    List<Order> findByCustomerIdOrderByOrderDateDesc(Integer customerId);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Order> findWithCustomerById(Integer id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE (o.status = com.laptopshop.entity.OrderStatus.COMPLETED " +
           "   OR EXISTS (SELECT p FROM Payment p WHERE p.order = o AND p.paymentStatus = com.laptopshop.entity.PaymentStatus.PAID)) " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.CANCELLED " +
           "AND o.status <> com.laptopshop.entity.OrderStatus.RETURNED")
    BigDecimal calculateTotalRevenue();

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

    @Query("SELECT new com.laptopshop.dto.OrderStatusCountDTO(o.status, COUNT(o)) " +
           "FROM Order o GROUP BY o.status")
    List<OrderStatusCountDTO> countOrdersByStatus();
}
