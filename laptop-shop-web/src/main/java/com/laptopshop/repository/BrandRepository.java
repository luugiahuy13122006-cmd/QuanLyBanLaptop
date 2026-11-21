package com.laptopshop.repository;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    Optional<Brand> findByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    @Query("SELECT b FROM Brand b WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.country) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Brand> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    List<Brand> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT COUNT(l) FROM Laptop l WHERE l.brand.id = :brandId")
    long countLaptopsByBrandId(@Param("brandId") Integer brandId);
}
