package com.laptopshop.repository;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    @Query("SELECT c FROM Category c WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    List<Category> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT COUNT(l) FROM Laptop l WHERE l.category.id = :categoryId")
    long countLaptopsByCategoryId(@Param("categoryId") Integer categoryId);
}
