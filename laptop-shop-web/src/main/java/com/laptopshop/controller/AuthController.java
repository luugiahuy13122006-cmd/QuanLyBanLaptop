package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.ProfileUpdateDTO;
import com.laptopshop.dto.RegisterDTO;
import com.laptopshop.dto.ResetPasswordDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String required,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }
        if (required != null) {
            model.addAttribute("infoMessage", "Vui lòng đăng nhập để tiếp tục.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            accountService.registerCustomer(registerDTO);
            ra.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/tai-khoan")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        if (currentUser == null) return "redirect:/login";
        Account account = accountService.findByUsername(currentUser.getUsername());
        model.addAttribute("account", account);
        model.addAttribute("profileDTO", accountService.getProfileForm(currentUser.getUsername()));
        return "auth/profile";
    }

    @PostMapping("/tai-khoan")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @Valid @ModelAttribute("profileDTO") ProfileUpdateDTO profileDTO,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes ra,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        if (currentUser == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            Account account = accountService.findByUsername(currentUser.getUsername());
            model.addAttribute("account", account);
            return "auth/profile";
        }
        try {
            Account updated = accountService.updateProfile(currentUser.getUsername(), profileDTO);
            refreshPrincipal(updated, currentUser.getCustomerId(), request, response);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
            return "redirect:/tai-khoan";
        } catch (RuntimeException ex) {
            // Giữ nguyên dữ liệu người dùng vừa nhập và hiển thị lỗi ngay trên form (không redirect làm mất dữ liệu)
            profileDTO.setCurrentPassword(null);
            profileDTO.setNewPassword(null);
            profileDTO.setConfirmNewPassword(null);
            model.addAttribute("account", accountService.findByUsername(currentUser.getUsername()));
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/profile";
        }
    }

    /** Làm mới thông tin người dùng đang đăng nhập (họ tên trên header...) ngay sau khi cập nhật hồ sơ, không cần đăng nhập lại. */
    private void refreshPrincipal(Account account, Integer customerId,
                                  HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails fresh = new CustomUserDetails(account, customerId);
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.authenticated(fresh, null, fresh.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email,
                                       HttpServletRequest request,
                                       RedirectAttributes ra) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath();
        accountService.createPasswordResetToken(email);
        ra.addFlashAttribute("successMessage",
                "Nếu email tồn tại trong hệ thống, liên kết đặt lại mật khẩu (hiệu lực 30 phút) đã được gửi. "
                        + "Trên môi trường thử nghiệm, vui lòng xem token tại console máy chủ.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        boolean valid = accountService.isResetTokenValid(token);
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken(token);
        model.addAttribute("resetDTO", dto);
        model.addAttribute("tokenValid", valid);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetDTO") ResetPasswordDTO resetDTO,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tokenValid", true);
            return "auth/reset-password";
        }
        try {
            accountService.resetPassword(resetDTO);
            ra.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("tokenValid", accountService.isResetTokenValid(resetDTO.getToken()));
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/tai-khoan/doi-mat-khau-bat-buoc")
    public String forceChangePasswordPage(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("username", currentUser.getUsername());
        return "auth/force-change-password";
    }

    @PostMapping("/tai-khoan/doi-mat-khau-bat-buoc")
    public String handleForceChangePassword(@AuthenticationPrincipal CustomUserDetails currentUser,
                                            @RequestParam String currentPassword,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmPassword,
                                            HttpServletRequest request,
                                            HttpServletResponse response,
                                            Model model,
                                            RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            Account updated = accountService.forceChangePassword(
                    currentUser.getUsername(), currentPassword, newPassword, confirmPassword);
            refreshPrincipal(updated, currentUser.getCustomerId(), request, response);
            ra.addFlashAttribute("successMessage", "Đổi mật khẩu mới thành công!");
            boolean staff = "ADMIN".equalsIgnoreCase(updated.getRole().getRoleName())
                    || "STAFF".equalsIgnoreCase(updated.getRole().getRoleName());
            return "redirect:" + (staff ? "/admin/dashboard" : "/");
        } catch (RuntimeException ex) {
            model.addAttribute("username", currentUser.getUsername());
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/force-change-password";
        }
    }

    @GetMapping("/403")
    public String accessDenied(Model model) {
        model.addAttribute("errorTitle", "403 - Không có quyền truy cập");
        model.addAttribute("errorMessage", "Tài khoản của bạn không được phép truy cập khu vực này.");
        return "error/403";
    }
}
