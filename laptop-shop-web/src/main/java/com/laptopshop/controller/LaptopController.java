package com.laptopshop.controller;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.dto.LaptopDTO;
import com.laptopshop.entity.Laptop;
import com.laptopshop.service.BrandService;
import com.laptopshop.service.CategoryService;
import com.laptopshop.service.LaptopService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/laptops")
public class LaptopController {

    private final LaptopService service;
    private final CategoryService categories;
    private final BrandService brands;

    public LaptopController(LaptopService service, CategoryService categories, BrandService brands) {
        this.service = service;
        this.categories = categories;
        this.brands = brands;
    }

    private void options(Model model) {
        model.addAttribute("categories", categories.getAllCategories());
        model.addAttribute("brands", brands.getAllBrands());
        model.addAttribute("ramOptions", LaptopService.VALID_RAM_OPTIONS);
        model.addAttribute("storageOptions", LaptopService.VALID_STORAGE_OPTIONS);
        model.addAttribute("screenOptions", LaptopService.VALID_SCREEN_OPTIONS);
        model.addAttribute("cpuOptions", java.util.List.of(
                "Intel Core i3-1215U", "Intel Core i5-1235U", "Intel Core i5-12450H", "Intel Core i5-12500H",
                "Intel Core i5-1335U", "Intel Core i5-13420H", "Intel Core i7-1355U", "Intel Core i7-13620H", "Intel Core i7-13700H",
                "Intel Core Ultra 5", "Intel Core Ultra 7", "Intel Core Ultra 9",
                "AMD Ryzen 5 7530U", "AMD Ryzen 7 7735HS", "AMD Ryzen 7 8845HS",
                "Apple M2", "Apple M3", "Apple M3 Pro", "Apple M4"
        ));
        model.addAttribute("gpuOptions", java.util.List.of(
                "Intel UHD Graphics", "Intel Iris Xe Graphics", "Intel Arc Graphics",
                "AMD Radeon Graphics", "Apple GPU 10-Core",
                "NVIDIA GeForce RTX 2050", "NVIDIA GeForce RTX 3050",
                "NVIDIA GeForce RTX 4050", "NVIDIA GeForce RTX 4060", "NVIDIA GeForce RTX 4070"
        ));
        model.addAttribute("osOptions", java.util.List.of(
                "Windows 11 Home", "Windows 11 Pro", "macOS", "Ubuntu Linux", "FreeDOS"
        ));
        model.addAttribute("colorOptions", java.util.List.of(
                "Bạc", "Đen", "Xám", "Xanh", "Trắng", "Vàng đồng"
        ));
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) Integer brandId,
                       @RequestParam(required = false) BigDecimal minPrice,
                       @RequestParam(required = false) BigDecimal maxPrice,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "id") String sortBy,
                       @RequestParam(defaultValue = "asc") String direction,
                       Model model) {
        var laptopPage = service.filterLaptops(keyword, categoryId, brandId, minPrice, maxPrice, page, size, sortBy, direction);
        options(model);
        model.addAttribute("laptopPage", laptopPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "admin/laptops/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("laptopDTO", new LaptopDTO());
        model.addAttribute("isEdit", false);
        options(model);
        return "admin/laptops/form";
    }

    @PostMapping("/add")
    public String create(@Valid @ModelAttribute("laptopDTO") LaptopDTO laptopDTO,
                         BindingResult br,
                         @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                         Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", false);
            options(model);
            return "admin/laptops/form";
        }
        try {
            service.createLaptopWithImages(laptopDTO, imageFiles);
            ra.addFlashAttribute("successMessage", "Thêm laptop và tải lên 5 ảnh sản phẩm thành công!");
            return "redirect:/admin/laptops";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            options(model);
            return "admin/laptops/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            Laptop l = service.getLaptopById(id);
            LaptopDTO d = new LaptopDTO();
            d.setId(l.getId());
            d.setSku(l.getSku());
            d.setName(l.getName());
            d.setCategoryId(l.getCategory().getId());
            d.setBrandId(l.getBrand().getId());
            d.setCpu(l.getCpu());
            d.setRamGb(l.getRamGb());
            d.setStorageGb(l.getStorageGb());
            d.setGpu(l.getGpu());
            d.setScreenSize(l.getScreenSize());
            d.setOperatingSystem(l.getOperatingSystem());
            d.setColor(l.getColor());
            d.setPurchasePrice(l.getPurchasePrice());
            d.setListPrice(l.getListPrice());
            d.setSalePrice(l.getSalePrice());
            d.setStockQuantity(l.getStockQuantity());
            d.setDescription(l.getDescription());
            d.setIsActive(l.getIsActive());
            model.addAttribute("laptopDTO", d);
            model.addAttribute("isEdit", true);
            options(model);
            return "admin/laptops/form";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/laptops";
        }
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("laptopDTO") LaptopDTO laptopDTO,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", true);
            options(model);
            return "admin/laptops/form";
        }
        try {
            service.updateLaptop(id, laptopDTO);
            ra.addFlashAttribute("successMessage", "Cập nhật laptop thành công!");
            return "redirect:/admin/laptops";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            options(model);
            return "admin/laptops/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            service.deleteLaptop(id);
            ra.addFlashAttribute("successMessage", "Xóa laptop thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/laptops";
    }
}
