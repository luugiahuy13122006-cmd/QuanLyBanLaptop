package com.laptopshop.repository;

import com.laptopshop.entity.Payment;
import com.laptopshop.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByOrderIdOrderByIdAsc(Integer orderId);

    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Integer orderId);

    Optional<Payment> findByProofImage(String proofImage);

    boolean existsByGatewayRef(String gatewayRef);

    Optional<Payment> findFirstByOrder_OrderCodeOrderByIdDesc(String orderCode);

    @EntityGraph(attributePaths = {"order"})
    List<Payment> findByPaymentStatusOrderByCreatedAtAsc(PaymentStatus status);

    long countByPaymentStatus(PaymentStatus status);
}
