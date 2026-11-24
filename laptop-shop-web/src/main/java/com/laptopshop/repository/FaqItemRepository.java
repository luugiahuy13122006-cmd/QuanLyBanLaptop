package com.laptopshop.repository;

import com.laptopshop.entity.FaqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FaqItemRepository extends JpaRepository<FaqItem, Integer> {
    List<FaqItem> findByPublishedTrueOrderBySortOrderAscIdAsc();
}
