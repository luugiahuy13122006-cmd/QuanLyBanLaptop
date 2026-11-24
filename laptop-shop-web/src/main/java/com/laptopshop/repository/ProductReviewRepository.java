package com.laptopshop.repository;

import com.laptopshop.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Integer> {
    @EntityGraph(attributePaths = {"customer"})
    List<ProductReview> findByLaptop_IdAndApprovedTrueOrderByCreatedAtDesc(Integer laptopId);

    @EntityGraph(attributePaths = {"laptop", "customer"})
    Page<ProductReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByLaptop_IdAndCustomer_Id(Integer laptopId, Integer customerId);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS DOUBLE)), 0) FROM ProductReview r WHERE r.laptop.id = :laptopId AND r.approved = true")
    Double getAverageRating(@Param("laptopId") Integer laptopId);

    long countByLaptop_IdAndApprovedTrue(Integer laptopId);
}
