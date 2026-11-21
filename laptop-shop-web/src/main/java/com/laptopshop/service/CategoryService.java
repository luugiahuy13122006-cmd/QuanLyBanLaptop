package com.laptopshop.service;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.dto.CategoryDTO;
import com.laptopshop.entity.Category;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<Category> getAllCategories() {
        return repo.findByIsActiveTrueOrderByNameAsc();
    }

    public Page<Category> getCategories(String keyword, int page, int size, String sortBy, String direction) {
        String sortField = switch (sortBy) {
            case "id", "isActive" -> sortBy;
            default -> "name";
        };
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.searchByKeyword(keyword == null ? "" : keyword.trim(), pageable);
    }

    public Category getCategoryById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
    }

    @Transactional
    public Category createCategory(CategoryDTO dto) {
        String name = dto.getName().trim();
        if (repo.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Tên danh mục '" + name + "' đã tồn tại!");
        }
        Category c = new Category();
        c.setName(name);
        c.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        c.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(c);
    }

    @Transactional
    public Category updateCategory(Integer id, CategoryDTO dto) {
        Category c = getCategoryById(id);
        String name = dto.getName().trim();
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Tên danh mục '" + name + "' đã được sử dụng!");
        }
        c.setName(name);
        c.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        c.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(c);
    }

    @Transactional
    public void deleteCategory(Integer id) {
        Category c = getCategoryById(id);
        long laptopCount = repo.countLaptopsByCategoryId(id);
        if (laptopCount > 0) {
            throw new BusinessException("Không thể xóa! Danh mục đang được sử dụng bởi " + laptopCount + " laptop.");
        }
        repo.delete(c);
    }
}
