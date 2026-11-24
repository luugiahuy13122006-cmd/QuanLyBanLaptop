package com.laptopshop.repository;

import com.laptopshop.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Integer> {
    List<CartItemEntity> findByAccount_IdOrderByIdAsc(Integer accountId);
    Optional<CartItemEntity> findByAccount_IdAndLaptop_Id(Integer accountId, Integer laptopId);
    void deleteByAccount_Id(Integer accountId);
    void deleteByAccount_IdAndLaptop_Id(Integer accountId, Integer laptopId);
    long countByLaptop_Id(Integer laptopId);
}
