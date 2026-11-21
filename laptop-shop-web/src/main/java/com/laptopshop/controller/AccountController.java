package com.laptopshop.controller;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Quản lý tài khoản & phân quyền người dùng (chỉ dành cho vai trò ADMIN).

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.AccountDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String listAccounts(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer roleId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "8") int size,
                               @RequestParam(defaultValue = "id") String sortBy,
                               @RequestParam(defaultValue = "asc") String direction,
                               Model model) {
        String sortField = switch (sortBy) {
            case "username", "fullName", "isActive" -> sortBy;
            default -> "id";
        };
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Page<Account> accountPage = accountService.getAccounts(keyword, roleId, PageRequest.of(page, size, sort));
        model.addAttribute("accountPage", accountPage);
        model.addAttribute("roles", accountService.getAllRoles());
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "admin/accounts/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("accountDTO", new AccountDTO());
        model.addAttribute("roles", accountService.getAllRoles());
        model.addAttribute("isEdit", false);
        return "admin/accounts/form";
    }

    @PostMapping("/add")
    public String createAccount(@Valid @ModelAttribute("accountDTO") AccountDTO accountDTO,
                                BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("isEdit", false);
            return "admin/accounts/form";
        }
        try {
            accountService.createAccount(accountDTO);
            ra.addFlashAttribute("successMessage", "Tạo tài khoản mới thành công!");
            return "redirect:/admin/accounts";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("isEdit", false);
            return "admin/accounts/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            Account a = accountService.getAccountById(id);
            AccountDTO dto = new AccountDTO();
            dto.setId(a.getId());
            dto.setUsername(a.getUsername());
            dto.setFullName(a.getFullName());
            dto.setEmail(a.getEmail());
            dto.setPhone(a.getPhone());
            dto.setRoleId(a.getRole().getId());
            dto.setIsActive(a.getIsActive());
            model.addAttribute("accountDTO", dto);
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("isEdit", true);
            return "admin/accounts/form";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/accounts";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateAccount(@PathVariable Integer id,
                                @Valid @ModelAttribute("accountDTO") AccountDTO accountDTO,
                                BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("isEdit", true);
            return "admin/accounts/form";
        }
        try {
            accountService.updateAccount(id, accountDTO);
            ra.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/accounts";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("isEdit", true);
            return "admin/accounts/form";
        }
    }

    @PostMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes ra) {
        try {
            Integer currentId = currentUser != null ? currentUser.getAccountId() : -1;
            accountService.toggleActive(id, currentId);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái hoạt động tài khoản!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes ra) {
        try {
            Integer currentId = currentUser != null ? currentUser.getAccountId() : -1;
            accountService.deleteAccount(id, currentId);
            ra.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}
