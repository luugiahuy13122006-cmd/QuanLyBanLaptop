package com.laptopshop.service;
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
    public BrandService(BrandRepository repo) { this.repo = repo; }

    public List<Brand> getAllBrands() {
        return repo.findByIsActiveTrueOrderByNameAsc();
    }

    public Page<Brand> getBrands(String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.searchByKeyword(keyword == null ? "" : keyword, pageable);
    }

    public Brand getBrandById(Integer id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + id));
    }

    @Transactional
    public Brand createBrand(BrandDTO dto) {
        if (repo.findByName(dto.getName()).isPresent()) {
            throw new BusinessException("Tên thương hiệu '" + dto.getName() + "' đã tồn tại!");
        }
        Brand b = new Brand();
        b.setName(dto.getName());
        b.setCountry(dto.getCountry());
        b.setDescription(dto.getDescription());
        b.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return repo.save(b);
    }

    @Transactional
    public Brand updateBrand(Integer id, BrandDTO dto) {
        Brand b = getBrandById(id);
        if (repo.existsByNameAndIdNot(dto.getName(), id)) {
            throw new BusinessException("Tên thương hiệu '" + dto.getName() + "' đã được sử dụng!");
        }
        b.setName(dto.getName());
        b.setCountry(dto.getCountry());
        b.setDescription(dto.getDescription());
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
