package com.laptopshop.repository;

import com.laptopshop.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    List<Supplier> findByIsActiveTrueOrderByNameAsc();
    Page<Supplier> findByNameContainingIgnoreCaseOrderByIdDesc(String name, Pageable pageable);
}
