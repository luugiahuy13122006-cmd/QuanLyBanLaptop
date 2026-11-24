package com.laptopshop.repository;

import com.laptopshop.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByAccount_IdOrderByCreatedAtDesc(Integer accountId);
    Optional<WishlistItem> findByAccount_IdAndLaptop_Id(Integer accountId, Integer laptopId);
    boolean existsByAccount_IdAndLaptop_Id(Integer accountId, Integer laptopId);
    void deleteByAccount_IdAndLaptop_Id(Integer accountId, Integer laptopId);
}
