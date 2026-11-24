package com.laptopshop.service;

import com.laptopshop.config.WebConfig;
import com.laptopshop.dto.LaptopDTO;
import com.laptopshop.entity.Brand;
import com.laptopshop.entity.Category;
import com.laptopshop.entity.Laptop;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.BrandRepository;
import com.laptopshop.repository.CategoryRepository;
import com.laptopshop.repository.ImportReceiptDetailRepository;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.OrderDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LaptopService {
    private static final Logger log = LoggerFactory.getLogger(LaptopService.class);

    public static final List<Integer> VALID_RAM_OPTIONS = List.of(4, 8, 12, 16, 18, 24, 32, 36, 48, 64, 128);
    public static final List<Integer> VALID_STORAGE_OPTIONS = List.of(128, 256, 512, 1024, 2048, 4096);
    public static final List<BigDecimal> VALID_SCREEN_OPTIONS = List.of(
            new BigDecimal("13.0"), new BigDecimal("13.3"), new BigDecimal("13.4"), new BigDecimal("13.6"),
            new BigDecimal("14.0"), new BigDecimal("14.2"), new BigDecimal("14.5"), new BigDecimal("15.3"),
            new BigDecimal("15.6"), new BigDecimal("16.0"), new BigDecimal("16.1"), new BigDecimal("17.0"),
            new BigDecimal("17.3"), new BigDecimal("18.0")
    );
    private static final Set<Integer> ALLOWED_RAM_SET = Set.copyOf(VALID_RAM_OPTIONS);
    private static final Set<Integer> ALLOWED_STORAGE_SET = Set.copyOf(VALID_STORAGE_OPTIONS);

    private final LaptopRepository laptopRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final OrderDetailRepository orderDetailRepo;
    private final ImportReceiptDetailRepository importReceiptDetailRepo;

    public LaptopService(LaptopRepository laptopRepo,
                         CategoryRepository categoryRepo,
                         BrandRepository brandRepo,
                         OrderDetailRepository orderDetailRepo,
                         ImportReceiptDetailRepository importReceiptDetailRepo) {
        this.laptopRepo = laptopRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.orderDetailRepo = orderDetailRepo;
        this.importReceiptDetailRepo = importReceiptDetailRepo;
    }

    public Page<Laptop> filterLaptops(String keyword, Integer categoryId, Integer brandId,
                                      BigDecimal minPrice, BigDecimal maxPrice,
                                      int page, int size, String sortBy, String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return laptopRepo.filterLaptops(kw, categoryId, brandId, minPrice, maxPrice, pageable);
    }

    public Page<Laptop> filterActiveLaptops(String keyword, Integer categoryId, Integer brandId,
                                            BigDecimal minPrice, BigDecimal maxPrice,
                                            boolean discountedOnly,
                                            int page, int size, String sortBy, String direction) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return laptopRepo.filterActiveLaptops(kw, categoryId, brandId, minPrice, maxPrice, discountedOnly, pageable);
    }

    public List<Laptop> getTopDiscountedLaptops(int limit) {
        return laptopRepo.findTopDiscountedLaptops(PageRequest.of(0, limit));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        String sortField = switch (sortBy) {
            case "name" -> "name";
            case "salePrice" -> "salePrice";
            case "stockQuantity" -> "stockQuantity";
            default -> "id";
        };
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        return PageRequest.of(page, size, sort);
    }

    public Laptop getLaptopById(Integer id) {
        return laptopRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop ID: " + id));
    }

    public List<Laptop> getAllLaptops() {
        return laptopRepo.findAllWithDetails();
    }

    public List<Laptop> searchLaptops(String keyword) {
        return laptopRepo.searchByKeyword(keyword == null ? "" : keyword.trim());
    }

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    public List<String> getGalleryImages(Laptop laptop) {
        LinkedHashSet<String> images = new LinkedHashSet<>();
        if (laptop.getImageUrl() != null && !laptop.getImageUrl().isBlank()) {
            for (String part : laptop.getImageUrl().split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    images.add(trimmed);
                }
            }
        }
        String sku = laptop.getSku() != null ? laptop.getSku().trim() : "";
        if (!sku.isEmpty()) {
            Path diskDir = WebConfig.resolveLaptopImagesBaseDir().resolve(sku);
            for (int i = 1; i <= 5; i++) {
                String url = "/images/laptops/" + sku + "/" + i + ".jpg";
                String relPath = "static/images/laptops/" + sku + "/" + i + ".jpg";
                if (Files.exists(diskDir.resolve(i + ".jpg")) || new ClassPathResource(relPath).exists()) {
                    images.add(url);
                }
            }
        }
        if (images.isEmpty()) {
            images.add("/images/laptop-default.svg");
        }
        return new ArrayList<>(images);
    }

    public List<Laptop> getRelatedLaptops(Laptop current, int limit) {
        List<Laptop> all = laptopRepo.findAllActiveWithDetails();
        List<Laptop> related = new ArrayList<>();
        for (Laptop l : all) {
            if (l.getId().equals(current.getId())) continue;
            boolean sameCategory = l.getCategory() != null && current.getCategory() != null
                    && l.getCategory().getId().equals(current.getCategory().getId());
            boolean sameBrand = l.getBrand() != null && current.getBrand() != null
                    && l.getBrand().getId().equals(current.getBrand().getId());
            if (sameCategory || sameBrand) {
                related.add(l);
                if (related.size() >= limit) return related;
            }
        }
        for (Laptop l : all) {
            if (!l.getId().equals(current.getId()) && !related.contains(l)) {
                related.add(l);
                if (related.size() >= limit) break;
            }
        }
        return related;
    }

    @Transactional
    public Laptop createLaptop(LaptopDTO dto) {
        validateRealHardwareSpecs(dto);
        String sku = dto.getSku().trim();
        if (laptopRepo.existsBySkuIgnoreCase(sku)) {
            throw new BusinessException("SKU '" + sku + "' đã tồn tại!");
        }
        Category cat = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        Brand brand = brandRepo.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu!"));
        Laptop l = buildLaptop(new Laptop(), dto, cat, brand, false);
        l.setSku(sku);
        return laptopRepo.save(l);
    }

    @Transactional(rollbackFor = Exception.class)
    public Laptop createLaptopWithImages(LaptopDTO dto, MultipartFile[] imageFiles) {
        validateRealHardwareSpecs(dto);
        String sku = dto.getSku().trim();
        if (laptopRepo.existsBySkuIgnoreCase(sku)) {
            throw new BusinessException("SKU '" + sku + "' đã tồn tại!");
        }
        Category cat = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        Brand brand = brandRepo.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu!"));

        String savedPathsCsv = saveUploadedLaptopImages(sku, imageFiles);
        dto.setImageUrl(savedPathsCsv);

        Laptop l = buildLaptop(new Laptop(), dto, cat, brand, false);
        l.setSku(sku);
        return laptopRepo.save(l);
    }

    public String saveUploadedLaptopImages(String rawSku, MultipartFile[] imageFiles) {
        String safeSku = rawSku == null ? "" : rawSku.trim();
        if (safeSku.isEmpty() || !safeSku.matches("[A-Za-z0-9_-]+")) {
            throw new BusinessException("Mã SKU chỉ được chứa chữ cái, chữ số, dấu gạch ngang (-) hoặc gạch dưới (_).");
        }

        List<MultipartFile> validFiles = new ArrayList<>();
        if (imageFiles != null) {
            for (MultipartFile f : imageFiles) {
                if (f != null && !f.isEmpty()) {
                    validFiles.add(f);
                }
            }
        }
        if (validFiles.size() != 5) {
            throw new BusinessException("Bắt buộc chọn đủ 5 ảnh sản phẩm (hiện có " + validFiles.size() + "/5 ảnh).");
        }

        for (int i = 0; i < validFiles.size(); i++) {
            MultipartFile file = validFiles.get(i);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "";
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                throw new BusinessException("Ảnh số " + (i + 1) + " ('" + originalName + "') không đúng định dạng! Chỉ chấp nhận .jpg, .png, .webp.");
            }
            String contentType = file.getContentType();
            if (contentType != null && !contentType.isBlank()
                    && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")
                    && !contentType.equalsIgnoreCase("application/octet-stream")) {
                throw new BusinessException("Tệp ảnh số " + (i + 1) + " không phải là tệp hình ảnh hợp lệ.");
            }
        }

        Path staticDir = WebConfig.resolveLaptopImagesBaseDir().resolve(safeSku);
        Path targetBase = WebConfig.resolveTargetLaptopImagesBaseDir();
        Path targetDir = targetBase != null ? targetBase.resolve(safeSku) : null;

        List<String> relativeUrls = new ArrayList<>();
        try {
            Files.createDirectories(staticDir);
            if (targetDir != null) {
                Files.createDirectories(targetDir);
            }
            for (int i = 0; i < 5; i++) {
                MultipartFile file = validFiles.get(i);
                String fileName = (i + 1) + ".jpg";
                Path destFile = staticDir.resolve(fileName);
                writeAsJpgOrRaw(file, destFile);
                if (targetDir != null) {
                    Files.copy(destFile, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
                relativeUrls.add("/images/laptops/" + safeSku + "/" + fileName);
            }
        } catch (IOException e) {
            log.error("[UPLOAD] Lỗi lưu ảnh laptop SKU={}: {}", safeSku, e.getMessage());
            throw new BusinessException("Không thể lưu tệp ảnh sản phẩm lên máy chủ: " + e.getMessage());
        }

        return String.join(",", relativeUrls);
    }

    private void writeAsJpgOrRaw(MultipartFile file, Path destFile) throws IOException {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }
        try (InputStream in = file.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img != null) {
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.drawImage(img, 0, 0, null);
                g.dispose();
                ImageIO.write(rgb, "jpg", destFile.toFile());
                return;
            }
        } catch (Exception ignored) {
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Transactional
    public Laptop updateLaptop(Integer id, LaptopDTO dto) {
        validateRealHardwareSpecs(dto);
        Laptop l = laptopRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop ID: " + id));
        String sku = dto.getSku().trim();
        if (laptopRepo.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new BusinessException("SKU '" + sku + "' đã được dùng bởi laptop khác!");
        }
        Category cat = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục!"));
        Brand brand = brandRepo.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu!"));
        buildLaptop(l, dto, cat, brand, true);
        l.setSku(sku);
        l.setUpdatedAt(LocalDateTime.now());
        return laptopRepo.save(l);
    }

    private void validateRealHardwareSpecs(LaptopDTO dto) {
        if (dto.getRamGb() != null && !ALLOWED_RAM_SET.contains(dto.getRamGb())) {
            throw new BusinessException("Dung lượng RAM không hợp lệ! Chỉ chấp nhận chuẩn thực tế: 4, 8, 12, 16, 18, 24, 32, 36, 48, 64, 128 GB.");
        }
        if (dto.getStorageGb() != null && !ALLOWED_STORAGE_SET.contains(dto.getStorageGb())) {
            throw new BusinessException("Dung lượng SSD không hợp lệ! Chỉ chấp nhận chuẩn thực tế: 128, 256, 512, 1024, 2048, 4096 GB.");
        }
        if (dto.getScreenSize() != null && VALID_SCREEN_OPTIONS.stream().noneMatch(s -> s.compareTo(dto.getScreenSize()) == 0)) {
            throw new BusinessException("Kích thước màn hình không hợp lệ! Vui lòng chọn đúng kích thước chuẩn thực tế.");
        }
        if (dto.getListPrice() != null && dto.getListPrice().compareTo(BigDecimal.ZERO) > 0
                && dto.getSalePrice() != null && dto.getSalePrice().compareTo(dto.getListPrice()) > 0) {
            throw new BusinessException("Giá bán không được lớn hơn Giá niêm yết!");
        }
    }

    @Transactional
    public void deleteLaptop(Integer id) {
        Laptop l = laptopRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy laptop ID: " + id));
        long orderRefs = orderDetailRepo.countByLaptopId(id);
        if (orderRefs > 0) {
            throw new BusinessException("Không thể xóa! Laptop đang được tham chiếu trong " + orderRefs + " chi tiết đơn hàng.");
        }
        if (importReceiptDetailRepo.existsByLaptopId(id)) {
            throw new BusinessException("Không thể xóa! Laptop đã được nhập kho qua phiếu nhập. Hãy ẩn sản phẩm thay vì xóa.");
        }
        laptopRepo.delete(l);
    }

    @Transactional(rollbackFor = Exception.class)
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

    private Laptop buildLaptop(Laptop l, LaptopDTO dto, Category cat, Brand brand, boolean keepExistingImageWhenBlank) {
        l.setName(dto.getName().trim());
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
        BigDecimal listPrice = (dto.getListPrice() != null && dto.getListPrice().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getListPrice() : dto.getSalePrice();
        l.setListPrice(listPrice);
        l.setSalePrice(dto.getSalePrice());
        l.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        if (!keepExistingImageWhenBlank || (dto.getImageUrl() != null && !dto.getImageUrl().isBlank())) {
            l.setImageUrl(dto.getImageUrl());
        }
        l.setDescription(dto.getDescription());
        l.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return l;
    }
}
