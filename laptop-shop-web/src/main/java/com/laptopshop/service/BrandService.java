package com.laptopshop.service;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.dto.BrandDTO;
import com.laptopshop.entity.Brand;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BrandService {
    private final BrandRepository repo;

    public BrandService(BrandRepository repo) {
        this.repo = repo;
    }

    public List<Brand> getAllBrands() {
        return repo.findByIsActiveTrueOrderByNameAsc();
    }

    public Page<Brand> getBrands(String keyword, int page, int size, String sortBy, String direction) {
        String sortField = switch (sortBy) {
            case "id", "country", "isActive" -> sortBy;
            default -> "name";
        };
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.searchByKeyword(keyword == null ? "" : keyword.trim(), pageable);
    }

    public Brand getBrandById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + id));
    }

    @Transactional
    public Brand createBrand(BrandDTO dto) {
        String name = dto.getName().trim();
        if (repo.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Tên thương hiệu '" + name + "' đã tồn tại!");
        }
        Brand b = new Brand();
        b.setName(name);
        b.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : null);
        b.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        b.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(b);
    }

    @Transactional
    public Brand updateBrand(Integer id, BrandDTO dto) {
        Brand b = getBrandById(id);
        String name = dto.getName().trim();
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Tên thương hiệu '" + name + "' đã được sử dụng!");
        }
        b.setName(name);
        b.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : null);
        b.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        b.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(b);
    }

    @Transactional
    public void deleteBrand(Integer id) {
        Brand b = getBrandById(id);
        long count = repo.countLaptopsByBrandId(id);
        if (count > 0) {
            throw new BusinessException("Không thể xóa! Thương hiệu đang được sử dụng bởi " + count + " laptop.");
        }
        repo.delete(b);
    }
}
