package com.laptopshop.repository;
import com.laptopshop.entity.Laptop;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
@Repository
public interface LaptopRepository extends JpaRepository<Laptop, Integer> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Integer id);

    @Query("SELECT l FROM Laptop l JOIN FETCH l.category c JOIN FETCH l.brand b " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR l.name LIKE %:keyword% OR b.name LIKE %:keyword%) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:brandId IS NULL OR b.id = :brandId) " +
           "AND (:minPrice IS NULL OR l.salePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.salePrice <= :maxPrice)")
    Page<Laptop> filterLaptops(
        @Param("keyword") String keyword,
        @Param("categoryId") Integer categoryId,
        @Param("brandId") Integer brandId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand b JOIN FETCH l.category c " +
           "WHERE l.name LIKE %:keyword% OR b.name LIKE %:keyword%")
    List<Laptop> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand JOIN FETCH l.category ORDER BY l.id")
    List<Laptop> findAllWithDetails();
    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand JOIN FETCH l.category WHERE l.id = :id")
    Optional<Laptop> findByIdWithDetails(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Laptop l WHERE l.id = :id")
    Optional<Laptop> findByIdWithLock(@Param("id") Integer id);

    @Query("SELECT l FROM Laptop l WHERE l.stockQuantity <= :threshold AND l.isActive = true ORDER BY l.stockQuantity ASC")
    List<Laptop> findLowStock(@Param("threshold") int threshold);
}
