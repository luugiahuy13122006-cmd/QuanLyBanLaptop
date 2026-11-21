package com.laptopshop.controller;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.dto.BrandDTO;
import com.laptopshop.entity.Brand;
import com.laptopshop.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/brands")
public class BrandController {

    private final BrandService service;

    public BrandController(BrandService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "name") String sortBy,
                       @RequestParam(defaultValue = "asc") String direction,
                       Model model) {
        var brandPage = service.getBrands(keyword, page, size, sortBy, direction);
        model.addAttribute("brandPage", brandPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "admin/brands/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("brandDTO", new BrandDTO());
        model.addAttribute("isEdit", false);
        return "admin/brands/form";
    }

    @PostMapping("/add")
    public String create(@Valid @ModelAttribute("brandDTO") BrandDTO brandDTO,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/brands/form";
        }
        try {
            service.createBrand(brandDTO);
            ra.addFlashAttribute("successMessage", "Thêm thương hiệu thành công!");
            return "redirect:/admin/brands";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/brands/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            Brand b = service.getBrandById(id);
            BrandDTO d = new BrandDTO();
            d.setId(b.getId());
            d.setName(b.getName());
            d.setCountry(b.getCountry());
            d.setDescription(b.getDescription());
            d.setIsActive(b.getIsActive());
            model.addAttribute("brandDTO", d);
            model.addAttribute("isEdit", true);
            return "admin/brands/form";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/brands";
        }
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("brandDTO") BrandDTO brandDTO,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/brands/form";
        }
        try {
            service.updateBrand(id, brandDTO);
            ra.addFlashAttribute("successMessage", "Cập nhật thương hiệu thành công!");
            return "redirect:/admin/brands";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "admin/brands/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            service.deleteBrand(id);
            ra.addFlashAttribute("successMessage", "Xóa thương hiệu thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/brands";
    }
}
