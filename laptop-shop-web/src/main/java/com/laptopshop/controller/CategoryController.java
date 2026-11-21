package com.laptopshop.controller;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.dto.CategoryDTO;
import com.laptopshop.entity.Category;
import com.laptopshop.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "name") String sortBy,
                       @RequestParam(defaultValue = "asc") String direction,
                       Model model) {
        var categoryPage = service.getCategories(keyword, page, size, sortBy, direction);
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "admin/categories/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("categoryDTO", new CategoryDTO());
        model.addAttribute("isEdit", false);
        return "admin/categories/form";
    }

    @PostMapping("/add")
    public String create(@Valid @ModelAttribute("categoryDTO") CategoryDTO categoryDTO,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
        try {
            service.createCategory(categoryDTO);
            ra.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
            return "redirect:/admin/categories";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            Category c = service.getCategoryById(id);
            CategoryDTO d = new CategoryDTO();
            d.setId(c.getId());
            d.setName(c.getName());
            d.setDescription(c.getDescription());
            d.setIsActive(c.getIsActive());
            model.addAttribute("categoryDTO", d);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("categoryDTO") CategoryDTO categoryDTO,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
        try {
            service.updateCategory(id, categoryDTO);
            ra.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
            return "redirect:/admin/categories";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            service.deleteCategory(id);
            ra.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
