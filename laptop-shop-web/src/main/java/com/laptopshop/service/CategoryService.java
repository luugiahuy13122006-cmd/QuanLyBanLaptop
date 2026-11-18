package com.laptopshop.service;
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
    public CategoryService(CategoryRepository repo) { this.repo = repo; }

    public List<Category> getAllCategories() {
        return repo.findByIsActiveTrueOrderByNameAsc();
    }

    public Page<Category> getCategories(String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.searchByKeyword(keyword == null ? "" : keyword, pageable);
    }

    public Category getCategoryById(Integer id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
    }

    @Transactional
    public Category createCategory(CategoryDTO dto) {
        if (repo.findByName(dto.getName()).isPresent()) {
            throw new BusinessException("Tên danh mục '" + dto.getName() + "' đã tồn tại!");
        }
        Category c = new Category();
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
        c.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(c);
    }

    @Transactional
    public Category updateCategory(Integer id, CategoryDTO dto) {
        Category c = getCategoryById(id);
        if (repo.existsByNameAndIdNot(dto.getName(), id)) {
            throw new BusinessException("Tên danh mục '" + dto.getName() + "' đã được sử dụng!");
        }
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
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
