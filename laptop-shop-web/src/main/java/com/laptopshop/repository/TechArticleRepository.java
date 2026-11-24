package com.laptopshop.repository;

import com.laptopshop.entity.TechArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechArticleRepository extends JpaRepository<TechArticle, Integer> {
    Page<TechArticle> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    Optional<TechArticle> findBySlug(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
