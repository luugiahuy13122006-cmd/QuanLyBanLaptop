package com.example.laptopshop.controller;

import com.example.laptopshop.service.ProductService;
import com.example.laptopshop.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final ProductService productService;

    public AdminController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalAdmins", userService.countAdmins());
        model.addAttribute("totalProducts", productService.getAll().size());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model, Principal principal) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("currentUser", principal.getName());
        return "admin/users";
    }

    @PostMapping("/users/{id}/khoa")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> userService.toggleEnabled(id, principal.getName()), "Đã cập nhật trạng thái tài khoản");
    }

    @PostMapping("/users/{id}/quyen")
    public String role(@PathVariable Long id, @RequestParam String role, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> userService.changeRole(id, role, principal.getName()), "Đã đổi quyền tài khoản");
    }

    @PostMapping("/users/{id}/xoa")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return run(ra, () -> userService.delete(id, principal.getName()), "Đã xóa tài khoản");
    }

    private String run(RedirectAttributes ra, Runnable action, String okMessage) {
        try {
            action.run();
            ra.addFlashAttribute("success", okMessage);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
