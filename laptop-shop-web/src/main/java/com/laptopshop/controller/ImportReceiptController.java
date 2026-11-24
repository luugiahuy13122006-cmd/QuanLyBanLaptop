package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.ImportReceiptForm;
import com.laptopshop.entity.ImportReceipt;
import com.laptopshop.entity.ImportReceiptStatus;
import com.laptopshop.service.ImportReceiptService;
import com.laptopshop.service.LaptopService;
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
@RequestMapping("/admin/import-receipts")
public class ImportReceiptController {

    private final ImportReceiptService importReceiptService;
    private final LaptopService laptopService;

    public ImportReceiptController(ImportReceiptService importReceiptService,
                                   LaptopService laptopService) {
        this.importReceiptService = importReceiptService;
        this.laptopService = laptopService;
    }

    @GetMapping
    public String listReceipts(
            @RequestParam(required = false) ImportReceiptStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<ImportReceipt> receiptPage = importReceiptService.getReceipts(
                status, keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        model.addAttribute("receiptPage", receiptPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", ImportReceiptStatus.values());
        return "admin/import-receipts/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("importForm", new ImportReceiptForm());
        model.addAttribute("laptops", laptopService.getAllLaptops());
        return "admin/import-receipts/create";
    }

    @PostMapping("/create")
    public String createReceipt(
            @Valid @ModelAttribute("importForm") ImportReceiptForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model,
            RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("laptops", laptopService.getAllLaptops());
            return "admin/import-receipts/create";
        }
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            ImportReceipt receipt = importReceiptService.createReceipt(form, accountId);
            ra.addFlashAttribute("successMessage",
                    "Tạo phiếu nhập " + receipt.getReceiptCode() + " thành công! Đang chờ Quản lý duyệt.");
            return "redirect:/admin/import-receipts/" + receipt.getId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("laptops", laptopService.getAllLaptops());
            return "admin/import-receipts/create";
        }
    }

    @GetMapping("/{id}")
    public String viewReceipt(@PathVariable Integer id, Model model) {
        model.addAttribute("receipt", importReceiptService.getReceiptById(id));
        return "admin/import-receipts/detail";
    }

    @PostMapping("/{id}/approve")
    public String approveReceipt(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            ImportReceipt receipt = importReceiptService.approveReceipt(id, accountId);
            ra.addFlashAttribute("successMessage",
                    "Đã duyệt phiếu nhập " + receipt.getReceiptCode() + " và cập nhật tồn kho thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/import-receipts/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectReceipt(
            @PathVariable Integer id,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            importReceiptService.rejectReceipt(id, accountId, reason);
            ra.addFlashAttribute("successMessage", "Đã từ chối phiếu nhập.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/import-receipts/" + id;
    }
}
