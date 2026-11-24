package com.laptopshop.repository;

import com.laptopshop.entity.WarrantyClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Integer> {
    List<WarrantyClaim> findBySerialNumberIgnoreCaseOrderByCreatedAtDesc(String serialNumber);
    Page<WarrantyClaim> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
