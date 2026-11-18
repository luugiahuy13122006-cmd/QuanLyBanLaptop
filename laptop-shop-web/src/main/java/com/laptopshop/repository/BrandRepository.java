package com.laptopshop.repository;
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
    boolean existsByNameAndIdNot(String name, Integer id);
    @Query("SELECT b FROM Brand b WHERE (:keyword IS NULL OR :keyword = '' OR b.name LIKE %:keyword%)")
    Page<Brand> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    List<Brand> findByIsActiveTrueOrderByNameAsc();
    @Query("SELECT COUNT(l) FROM Laptop l WHERE l.brand.id = :brandId")
    long countLaptopsByBrandId(@Param("brandId") Integer brandId);
}
