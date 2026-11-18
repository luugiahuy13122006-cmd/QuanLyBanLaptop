package com.laptopshop.service;
import com.laptopshop.dto.LaptopDTO;
import com.laptopshop.entity.Brand;
import com.laptopshop.entity.Category;
import com.laptopshop.entity.Laptop;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.BrandRepository;
import com.laptopshop.repository.CategoryRepository;
import com.laptopshop.repository.LaptopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LaptopService {
    private static final Logger log = LoggerFactory.getLogger(LaptopService.class);

    private final LaptopRepository laptopRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;

    public LaptopService(LaptopRepository laptopRepo, CategoryRepository categoryRepo, BrandRepository brandRepo) {
        this.laptopRepo = laptopRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
    }

    public Page<Laptop> filterLaptops(String keyword, Integer categoryId, Integer brandId,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       int page, int size, String sortBy, String direction) {
        // Map sortBy field name
        String sortField = switch (sortBy) {
            case "name" -> "name";
            case "salePrice" -> "salePrice";
            case "stockQuantity" -> "stockQuantity";
            default -> "id";
        };
        Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortField).descending()
            : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return laptopRepo.filterLaptops(kw, categoryId, brandId, minPrice, maxPrice, pageable);
    }

    public Laptop getLaptopById(Integer id) {
        return laptopRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop ID: " + id));
    }

    public List<Laptop> getAllLaptops() {
        return laptopRepo.findAllWithDetails();
    }

    public List<Laptop> searchLaptops(String keyword) {
        return laptopRepo.searchByKeyword(keyword);
    }

    @Transactional
    public Laptop createLaptop(LaptopDTO dto) {
        if (laptopRepo.existsBySku(dto.getSku())) {
            throw new BusinessException("SKU '" + dto.getSku() + "' đã tồn tại!");
        }
        Category cat = categoryRepo.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        Brand brand = brandRepo.findById(dto.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu!"));
        Laptop l = buildLaptop(new Laptop(), dto, cat, brand);
        return laptopRepo.save(l);
    }

    @Transactional
    public Laptop updateLaptop(Integer id, LaptopDTO dto) {
        Laptop l = getLaptopById(id);
        if (laptopRepo.existsBySkuAndIdNot(dto.getSku(), id)) {
            throw new BusinessException("SKU '" + dto.getSku() + "' đã được dùng bởi laptop khác!");
        }
        Category cat = categoryRepo.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        Brand brand = brandRepo.findById(dto.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu!"));
        buildLaptop(l, dto, cat, brand);
        l.setUpdatedAt(LocalDateTime.now());
        return laptopRepo.save(l);
    }

    @Transactional
    public void deleteLaptop(Integer id) {
        Laptop l = getLaptopById(id);
        laptopRepo.delete(l);
    }

    // Cập nhật tồn kho an toàn với khóa PESSIMISTIC (chống race condition)
    @Transactional
    public void updateStockSafe(Integer laptopId, int delta) {
        String threadName = Thread.currentThread().getName();
        log.info("[THREAD: {}] Bắt đầu cập nhật tồn kho laptop ID={}, delta={}", threadName, laptopId, delta);
        Laptop l = laptopRepo.findByIdWithLock(laptopId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop ID: " + laptopId));
        int newStock = l.getStockQuantity() + delta;
        if (newStock < 0) {
            throw new BusinessException("Tồn kho không đủ! Hiện còn: " + l.getStockQuantity());
        }
        l.setStockQuantity(newStock);
        l.setUpdatedAt(LocalDateTime.now());
        laptopRepo.save(l);
        log.info("[THREAD: {}] Cập nhật thành công: laptop ID={}, tồn kho mới={}", threadName, laptopId, newStock);
    }

    // Tác vụ định kỳ mỗi 60 giây: log cảnh báo tồn kho thấp
    @Scheduled(fixedDelay = 60000)
    public void checkLowStockAlert() {
        String threadName = Thread.currentThread().getName();
        log.info("[SCHEDULED | THREAD: {}] Kiểm tra tồn kho thấp...", threadName);
        List<Laptop> lowStockList = laptopRepo.findLowStock(5);
        if (lowStockList.isEmpty()) {
            log.info("[SCHEDULED | THREAD: {}] Tất cả sản phẩm có tồn kho ổn định.", threadName);
        } else {
            log.warn("[SCHEDULED | THREAD: {}] Cảnh báo {} sản phẩm tồn kho thấp (<= 5):", threadName, lowStockList.size());
            for (Laptop lp : lowStockList) {
                log.warn("  >> ID={} | {} | Tồn kho: {}", lp.getId(), lp.getName(), lp.getStockQuantity());
            }
        }
    }

    private Laptop buildLaptop(Laptop l, LaptopDTO dto, Category cat, Brand brand) {
        l.setSku(dto.getSku());
        l.setName(dto.getName());
        l.setCategory(cat);
        l.setBrand(brand);
        l.setCpu(dto.getCpu());
        l.setRamGb(dto.getRamGb());
        l.setStorageGb(dto.getStorageGb());
        l.setGpu(dto.getGpu());
        l.setScreenSize(dto.getScreenSize());
        l.setOperatingSystem(dto.getOperatingSystem());
        l.setColor(dto.getColor());
        l.setPurchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO);
        l.setSalePrice(dto.getSalePrice());
        l.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        l.setImageUrl(dto.getImageUrl());
        l.setDescription(dto.getDescription());
        l.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return l;
    }
}
