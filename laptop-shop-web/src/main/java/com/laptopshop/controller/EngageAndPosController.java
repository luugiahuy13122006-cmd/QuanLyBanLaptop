package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.service.*;
import com.laptopshop.util.PaginationUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EngageAndPosController {

    private final WishlistAndCompareService wishlistAndCompareService;
    private final CustomerAddressService addressService;
    private final CustomerRepository customerRepository;
    private final ReviewService reviewService;
    private final NotificationSseService sseService;

    public EngageAndPosController(WishlistAndCompareService wishlistAndCompareService,
                                  CustomerAddressService addressService,
                                  CustomerRepository customerRepository,
                                  ReviewService reviewService,
                                  NotificationSseService sseService) {
        this.wishlistAndCompareService = wishlistAndCompareService;
        this.addressService = addressService;
        this.customerRepository = customerRepository;
        this.reviewService = reviewService;
        this.sseService = sseService;
    }

    @GetMapping("/yeu-thich")
    public String wishlistPage(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("items", wishlistAndCompareService.getWishlist(currentUser.getAccountId()));
        return "customer/wishlist";
    }

    @PostMapping("/yeu-thich/toggle/{laptopId}")
    public String toggleWishlist(@PathVariable Integer laptopId,
                                 @RequestParam(required = false) String redirect,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        boolean added = wishlistAndCompareService.toggleWishlist(currentUser.getAccountId(), laptopId);
        ra.addFlashAttribute("successMessage", added ? "Đã thêm vào danh sách yêu thích." : "Đã bỏ khỏi danh sách yêu thích.");
        return "redirect:" + (redirect != null && redirect.startsWith("/") ? redirect : "/san-pham/" + laptopId);
    }

    @GetMapping("/so-sanh")
    public String comparePage(HttpSession session, Model model) {
        model.addAttribute("laptops", wishlistAndCompareService.getCompareLaptops(session));
        return "store/compare";
    }

    @PostMapping("/so-sanh/them/{laptopId}")
    public String addCompare(@PathVariable Integer laptopId, HttpSession session, RedirectAttributes ra) {
        try {
            wishlistAndCompareService.addToCompare(laptopId, session);
            ra.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào bảng so sánh.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/so-sanh";
    }

    @PostMapping("/so-sanh/xoa/{laptopId}")
    public String removeCompare(@PathVariable Integer laptopId, HttpSession session, RedirectAttributes ra) {
        wishlistAndCompareService.removeFromCompare(laptopId, session);
        ra.addFlashAttribute("successMessage", "Đã xóa khỏi bảng so sánh.");
        return "redirect:/so-sanh";
    }

    @GetMapping("/tai-khoan/dia-chi")
    public String addressBook(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        if (currentUser == null) return "redirect:/login";
        Integer cusId = currentUser.getCustomerId();
        if (cusId == null) {
            cusId = customerRepository.findByAccountId(currentUser.getAccountId()).map(c -> c.getId()).orElse(null);
        }
        model.addAttribute("addresses", addressService.getAddressesForCustomer(cusId));
        return "auth/addresses";
    }

    @PostMapping("/tai-khoan/dia-chi/them")
    public String addAddress(@AuthenticationPrincipal CustomUserDetails currentUser,
                             @RequestParam String recipientName,
                             @RequestParam String phone,
                             @RequestParam(required = false) String province,
                             @RequestParam(required = false) String district,
                             @RequestParam(required = false) String ward,
                             @RequestParam String addressLine,
                             @RequestParam(defaultValue = "false") boolean makeDefault,
                             RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            Integer cusId = customerRepository.findByAccountId(currentUser.getAccountId())
                    .map(c -> c.getId())
                    .orElseThrow(() -> new IllegalStateException("Tài khoản chưa có hồ sơ khách hàng."));
            addressService.addAddress(cusId, recipientName, phone, province, district, ward, addressLine, makeDefault);
            ra.addFlashAttribute("successMessage", "Đã thêm địa chỉ giao hàng mới.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    @PostMapping("/tai-khoan/dia-chi/{id}/mac-dinh")
    public String setDefaultAddress(@PathVariable Integer id,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            Integer cusId = customerRepository.findByAccountId(currentUser.getAccountId())
                    .map(c -> c.getId()).orElseThrow();
            addressService.setDefaultAddress(cusId, id);
            ra.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ mặc định.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    @PostMapping("/tai-khoan/dia-chi/{id}/xoa")
    public String deleteAddress(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            Integer cusId = customerRepository.findByAccountId(currentUser.getAccountId())
                    .map(c -> c.getId()).orElseThrow();
            addressService.deleteAddress(cusId, id);
            ra.addFlashAttribute("successMessage", "Đã xóa địa chỉ.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    @GetMapping("/admin/reviews")
    public String adminReviews(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reviewPage", reviewService.listAllForModeration(
                PageRequest.of(PaginationUtils.clampPage(page), 15)));
        return "admin/reviews/list";
    }

    @PostMapping("/admin/reviews/{id}/moderate")
    public String moderateReview(@PathVariable Integer id,
                                 @RequestParam boolean approve,
                                 @RequestParam(required = false) String shopReply,
                                 RedirectAttributes ra) {
        reviewService.moderate(id, approve, shopReply);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đánh giá #" + id);
        return "redirect:/admin/reviews";
    }

    @GetMapping(value = "/admin/sse/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        return sseService.subscribe();
    }
}
