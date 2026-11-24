package com.laptopshop.repository;

import com.laptopshop.entity.ImportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportReceiptDetailRepository extends JpaRepository<ImportReceiptDetail, Integer> {
    boolean existsByLaptopId(Integer laptopId);
}
