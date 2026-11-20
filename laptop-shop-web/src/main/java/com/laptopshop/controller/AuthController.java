package com.example.laptopshop.controller;

import com.example.laptopshop.dto.RegisterForm;
import com.example.laptopshop.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) { this.userService = userService; }

    @GetMapping("/dang-nhap")
    public String loginPage() { return "login"; }

    @GetMapping("/dang-ky")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/dang-ky")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult result, RedirectAttributes redirect) {
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
        if (result.hasErrors()) return "register";
        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            result.reject("register.failed", e.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
        return "redirect:/dang-nhap";
    }
}
