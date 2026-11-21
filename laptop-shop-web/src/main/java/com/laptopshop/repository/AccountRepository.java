package com.laptopshop.repository;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import com.laptopshop.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Integer id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    @EntityGraph(attributePaths = {"role"})
    @Query("SELECT a FROM Account a JOIN a.role r " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:roleId IS NULL OR r.id = :roleId)")
    Page<Account> searchAccounts(@Param("keyword") String keyword,
                                 @Param("roleId") Integer roleId,
                                 Pageable pageable);
}
