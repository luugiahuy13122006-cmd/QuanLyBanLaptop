package com.laptopshop.controller;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Quản lý tài khoản & phân quyền người dùng (chỉ dành cho vai trò ADMIN).
// Nghiệp vụ: quản trị chỉ cấp tài khoản nhân viên (STAFF) / quản trị (ADMIN). Tài khoản khách hàng (CUSTOMER) do khách tự đăng ký.

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.AccountDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Role;
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

import java.util.List;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Vai trò được phép cấp trong khu vực quản trị: chỉ ADMIN / STAFF. */
    private List<Role> staffRoles() {
        return accountService.getAllRoles().stream()
                .filter(r -> !"CUSTOMER".equalsIgnoreCase(r.getRoleName()))
                .toList();
    }

    /** Chuẩn bị dữ liệu dùng chung cho form thêm/sửa tài khoản (danh sách vai trò, khóa vai trò của khách hàng). */
    private void prepareForm(Model model, boolean isEdit, Integer accountId) {
        boolean lockRole = false;
        if (isEdit && accountId != null) {
            try {
                lockRole = "CUSTOMER".equalsIgnoreCase(accountService.getAccountById(accountId).getRole().getRoleName());
            } catch (RuntimeException ignored) {
                // tài khoản không tồn tại: thao tác lưu sẽ báo lỗi phù hợp
            }
        }
        model.addAttribute("roles", lockRole ? accountService.getAllRoles() : staffRoles());
        model.addAttribute("lockRole", lockRole);
        model.addAttribute("isEdit", isEdit);
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
        prepareForm(model, false, null);
        return "admin/accounts/form";
    }

    @PostMapping("/add")
    public String createAccount(@Valid @ModelAttribute("accountDTO") AccountDTO accountDTO,
                                BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            prepareForm(model, false, null);
            return "admin/accounts/form";
        }
        try {
            accountService.createAccount(accountDTO);
            ra.addFlashAttribute("successMessage", "Tạo tài khoản mới thành công!");
            return "redirect:/admin/accounts";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareForm(model, false, null);
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
            prepareForm(model, true, id);
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
            prepareForm(model, true, id);
            return "admin/accounts/form";
        }
        try {
            accountService.updateAccount(id, accountDTO);
            ra.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/accounts";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareForm(model, true, id);
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
