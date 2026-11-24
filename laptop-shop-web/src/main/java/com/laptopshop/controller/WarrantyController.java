package com.laptopshop.controller;

import com.laptopshop.entity.WarrantyStatus;
import com.laptopshop.service.LaptopService;
import com.laptopshop.service.WarrantyService;
import com.laptopshop.util.PaginationUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WarrantyController {

    private final WarrantyService warrantyService;
    private final LaptopService laptopService;

    public WarrantyController(WarrantyService warrantyService, LaptopService laptopService) {
        this.warrantyService = warrantyService;
        this.laptopService = laptopService;
    }

    @GetMapping("/bao-hanh")
    public String publicLookup(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.isBlank()) {
            var serials = warrantyService.lookupWarranty(query);
            model.addAttribute("serials", serials);
            if (!serials.isEmpty()) {
                model.addAttribute("claims", warrantyService.getClaimsForSerial(serials.get(0).getSerialNumber()));
            }
        }
        model.addAttribute("query", query);
        return "store/warranty-lookup";
    }

    @PostMapping("/bao-hanh/yeu-cau")
    public String submitPublicClaim(@RequestParam String serialNumber,
                                    @RequestParam String customerName,
                                    @RequestParam String phone,
                                    @RequestParam String issueDescription,
                                    RedirectAttributes ra) {
        try {
            warrantyService.submitClaim(serialNumber, customerName, phone, issueDescription);
            ra.addFlashAttribute("successMessage",
                    "Đã tiếp nhận yêu cầu bảo hành cho Serial " + serialNumber.toUpperCase() + ". Kỹ thuật viên sẽ liên hệ sớm!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/bao-hanh?query=" + serialNumber;
    }

    @GetMapping("/admin/warranties")
    public String adminWarrantyList(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        int p = PaginationUtils.clampPage(page);
        int s = PaginationUtils.clampSize(size, 10, 100);
        model.addAttribute("serialPage", warrantyService.listSerials(PageRequest.of(p, s)));
        model.addAttribute("claimPage", warrantyService.listClaims(PageRequest.of(p, s)));
        model.addAttribute("laptops", laptopService.getAllLaptops());
        model.addAttribute("statuses", WarrantyStatus.values());
        return "admin/warranties/index";
    }

    @PostMapping("/admin/warranties/serials/add")
    public String addSerial(@RequestParam Integer laptopId,
                            @RequestParam String serialNumber,
                            RedirectAttributes ra) {
        try {
            warrantyService.addSerialToStock(laptopId, serialNumber);
            ra.addFlashAttribute("successMessage", "Đã thêm Serial " + serialNumber + " vào kho.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/warranties";
    }

    @PostMapping("/admin/warranties/claims/{id}/status")
    public String updateClaim(@PathVariable Integer id,
                              @RequestParam WarrantyStatus status,
                              @RequestParam(required = false) String technicianNote,
                              RedirectAttributes ra) {
        try {
            warrantyService.updateClaimStatus(id, status, technicianNote);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái phiếu bảo hành #" + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/warranties";
    }
}
