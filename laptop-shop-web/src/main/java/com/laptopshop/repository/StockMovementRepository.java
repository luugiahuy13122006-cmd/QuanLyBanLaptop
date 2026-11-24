package com.laptopshop.repository;

import com.laptopshop.entity.StockMovement;
import com.laptopshop.entity.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
    Page<StockMovement> findByLaptop_IdOrderByCreatedAtDesc(Integer laptopId, Pageable pageable);
    Page<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovementType movementType, Pageable pageable);
    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByLaptop_Id(Integer laptopId);
}
