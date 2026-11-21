package com.laptopshop.repository;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu (tích hợp truy vấn cho TV3 & khóa Pessimistic cho TV4)
import com.laptopshop.entity.Laptop;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuAndIdNot(String sku, Integer id);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Integer id);

    // Lọc danh sách laptop cho trang Quản trị (bao gồm cả đang bán & ẩn)
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("SELECT l FROM Laptop l " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(l.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(l.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "       OR LOWER(l.cpu) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR l.category.id = :categoryId) " +
           "AND (:brandId IS NULL OR l.brand.id = :brandId) " +
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

    // Lọc danh sách laptop đang mở bán (TrangThaiHoatDong = true) cho giao diện Khách hàng (TV3)
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("SELECT l FROM Laptop l " +
           "WHERE l.isActive = true " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(l.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(l.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(l.cpu) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR l.category.id = :categoryId) " +
           "AND (:brandId IS NULL OR l.brand.id = :brandId) " +
           "AND (:minPrice IS NULL OR l.salePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR l.salePrice <= :maxPrice) " +
           "AND (:discountedOnly = false OR l.listPrice > l.salePrice)")
    Page<Laptop> filterActiveLaptops(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("brandId") Integer brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("discountedOnly") boolean discountedOnly,
            Pageable pageable
    );

    @Query("SELECT l FROM Laptop l LEFT JOIN FETCH l.brand LEFT JOIN FETCH l.category " +
           "WHERE l.isActive = true AND l.listPrice > l.salePrice " +
           "ORDER BY (l.listPrice - l.salePrice) DESC")
    List<Laptop> findTopDiscountedLaptops(Pageable pageable);

    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand b JOIN FETCH l.category c " +
           "WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(l.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Laptop> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand JOIN FETCH l.category ORDER BY l.id")
    List<Laptop> findAllWithDetails();

    @Query("SELECT l FROM Laptop l LEFT JOIN FETCH l.category LEFT JOIN FETCH l.brand WHERE l.isActive = true ORDER BY l.id ASC")
    List<Laptop> findAllActiveWithDetails();

    @Query("SELECT l FROM Laptop l JOIN FETCH l.brand JOIN FETCH l.category WHERE l.id = :id")
    Optional<Laptop> findByIdWithDetails(@Param("id") Integer id);

    // Khóa bi quan (PESSIMISTIC_WRITE) dùng cho TV2 (StockService) và TV4 (OrderService)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Laptop l WHERE l.id = :id")
    Optional<Laptop> findByIdWithLock(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Laptop l WHERE l.id = :id")
    Optional<Laptop> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT l FROM Laptop l LEFT JOIN FETCH l.brand LEFT JOIN FETCH l.category " +
           "WHERE l.stockQuantity <= :threshold AND l.isActive = true ORDER BY l.stockQuantity ASC")
    List<Laptop> findLowStock(@Param("threshold") int threshold);

    @Query("SELECT l FROM Laptop l LEFT JOIN FETCH l.brand LEFT JOIN FETCH l.category " +
           "WHERE l.stockQuantity <= :threshold AND l.isActive = true ORDER BY l.stockQuantity ASC")
    List<Laptop> findLowStockLaptops(@Param("threshold") int threshold);
}
