package com.laptopshop.controller;

import com.laptopshop.entity.Coupon;
import com.laptopshop.entity.DiscountType;
import com.laptopshop.entity.Supplier;
import com.laptopshop.service.AuditLogService;
import com.laptopshop.service.CouponService;
import com.laptopshop.service.SupplierService;
import com.laptopshop.util.PaginationUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class CouponAndSupplierController {

    private final CouponService couponService;
    private final SupplierService supplierService;
    private final AuditLogService auditLogService;

    public CouponAndSupplierController(CouponService couponService,
                                       SupplierService supplierService,
                                       AuditLogService auditLogService) {
        this.couponService = couponService;
        this.supplierService = supplierService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/admin/coupons")
    public String listCoupons(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        model.addAttribute("couponPage", couponService.searchCoupons(
                keyword, PageRequest.of(PaginationUtils.clampPage(page), PaginationUtils.clampSize(size, 10, 100))));
        model.addAttribute("keyword", keyword);
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/coupons/list";
    }

    @PostMapping("/admin/coupons/save")
    public String saveCoupon(@ModelAttribute Coupon coupon, RedirectAttributes ra) {
        try {
            couponService.saveCoupon(coupon);
            ra.addFlashAttribute("successMessage", "Đã lưu mã giảm giá " + coupon.getCode().toUpperCase());
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @GetMapping("/admin/suppliers")
    public String listSuppliers(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        model.addAttribute("supplierPage", supplierService.search(
                keyword, PageRequest.of(PaginationUtils.clampPage(page), PaginationUtils.clampSize(size, 10, 100))));
        model.addAttribute("keyword", keyword);
        return "admin/suppliers/list";
    }

    @PostMapping("/admin/suppliers/save")
    public String saveSupplier(@ModelAttribute Supplier supplier, RedirectAttributes ra) {
        try {
            supplierService.save(supplier);
            ra.addFlashAttribute("successMessage", "Đã lưu thông tin nhà cung cấp.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/admin/suppliers/payables/{receiptId}")
    public String paySupplier(@PathVariable Integer receiptId,
                              @RequestParam BigDecimal amount,
                              RedirectAttributes ra) {
        try {
            supplierService.recordPayablePayment(receiptId, amount);
            ra.addFlashAttribute("successMessage", "Đã ghi nhận thanh toán công nợ phiếu nhập #" + receiptId);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/import-receipts/" + receiptId;
    }

    @PostMapping("/admin/suppliers/{id}/toggle")
    public String toggleSupplier(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            Supplier updated = supplierService.toggleActive(id);
            ra.addFlashAttribute("successMessage",
                    Boolean.TRUE.equals(updated.getIsActive())
                            ? "Đã khôi phục hợp tác với nhà cung cấp " + updated.getName()
                            : "Đã hủy hợp tác với nhà cung cấp " + updated.getName());
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/admin/suppliers/{id}/delete")
    public String deleteSupplier(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            supplierService.delete(id);
            ra.addFlashAttribute("successMessage", "Đã xóa nhà cung cấp thành công.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @GetMapping("/admin/audit-logs")
    public String auditLogs(@RequestParam(defaultValue = "") String keyword,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        model.addAttribute("logPage", auditLogService.searchLogs(
                keyword, PageRequest.of(PaginationUtils.clampPage(page), PaginationUtils.clampSize(size, 20, 100))));
        model.addAttribute("keyword", keyword);
        return "admin/audit-logs/list";
    }
}
