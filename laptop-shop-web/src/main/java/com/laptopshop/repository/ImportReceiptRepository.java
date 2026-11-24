package com.laptopshop.repository;

import com.laptopshop.dto.MonthlyImportCostDTO;
import com.laptopshop.entity.ImportReceipt;
import com.laptopshop.entity.ImportReceiptStatus;
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
public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Integer> {

    boolean existsByReceiptCodeIgnoreCase(String receiptCode);
    boolean existsByCreatedById(Integer accountId);
    boolean existsByApprovedById(Integer accountId);
    long countByStatus(ImportReceiptStatus status);

    @EntityGraph(attributePaths = {"createdBy", "approvedBy"})
    @Query("SELECT r FROM ImportReceipt r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.receiptCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.supplier) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ImportReceipt> filterReceipts(@Param("status") ImportReceiptStatus status,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "approvedBy", "details", "details.laptop"})
    Optional<ImportReceipt> findWithDetailsById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ImportReceipt r WHERE r.id = :id")
    Optional<ImportReceipt> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM ImportReceipt r " +
           "WHERE r.status = com.laptopshop.entity.ImportReceiptStatus.APPROVED")
    BigDecimal calculateTotalImportCost();

    @Query("SELECT new com.laptopshop.dto.MonthlyImportCostDTO(" +
           "MONTH(r.approvedAt), COUNT(r), COALESCE(SUM(r.totalAmount), 0)) " +
           "FROM ImportReceipt r " +
           "WHERE r.status = com.laptopshop.entity.ImportReceiptStatus.APPROVED " +
           "AND YEAR(r.approvedAt) = :year " +
           "GROUP BY MONTH(r.approvedAt) ORDER BY MONTH(r.approvedAt)")
    List<MonthlyImportCostDTO> getMonthlyImportCost(@Param("year") int year);
}
