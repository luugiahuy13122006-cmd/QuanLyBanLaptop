package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.ShopProperties;
import com.laptopshop.dto.CartItemDTO;
import com.laptopshop.dto.CheckoutFormDTO;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.*;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class StoreController {

    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("15000000");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("50000");

    private final LaptopService laptopService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final ShopProperties shopProperties;
    private final QrCodeService qrCodeService;
    private final PaymentGatewayService paymentGatewayService;
    private final CartService cartService;
    private final CouponService couponService;
    private final CustomerAddressService addressService;
    private final ReviewService reviewService;
    private final WishlistAndCompareService wishlistAndCompareService;
    private final ContentAndChatService contentService;

    public StoreController(LaptopService laptopService,
                           CategoryService categoryService,
                           BrandService brandService,
                           CustomerService customerService,
                           CustomerRepository customerRepository,
                           OrderService orderService,
                           ShopProperties shopProperties,
                           QrCodeService qrCodeService,
                           PaymentGatewayService paymentGatewayService,
                           CartService cartService,
                           CouponService couponService,
                           CustomerAddressService addressService,
                           ReviewService reviewService,
                           WishlistAndCompareService wishlistAndCompareService,
                           ContentAndChatService contentService) {
        this.laptopService = laptopService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
        this.shopProperties = shopProperties;
        this.qrCodeService = qrCodeService;
        this.paymentGatewayService = paymentGatewayService;
        this.cartService = cartService;
        this.couponService = couponService;
        this.addressService = addressService;
        this.reviewService = reviewService;
        this.wishlistAndCompareService = wishlistAndCompareService;
        this.contentService = contentService;
    }

    @GetMapping({"/", "/tim-kiem"})
    public String home(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) Integer brandId,
                       @RequestParam(required = false) BigDecimal minPrice,
                       @RequestParam(required = false) BigDecimal maxPrice,
                       @RequestParam(defaultValue = "false") boolean discountedOnly,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "8") int size,
                       @RequestParam(defaultValue = "id") String sortBy,
                       @RequestParam(defaultValue = "asc") String direction,
                       HttpSession session,
                       Model model) {
        var laptopPage = laptopService.filterActiveLaptops(
                keyword, categoryId, brandId, minPrice, maxPrice, discountedOnly, page, size, sortBy, direction);
        model.addAttribute("laptopPage", laptopPage);
        model.addAttribute("flashSaleLaptops", laptopService.getTopDiscountedLaptops(4));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("banners", contentService.listActiveBanners());
        model.addAttribute("compareIds", wishlistAndCompareService.getCompareIds(session));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("discountedOnly", discountedOnly);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        return "store/index";
    }

    @GetMapping("/api/store/filter")
    @ResponseBody
    public Map<String, Object> filterJson(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer categoryId,
                                          @RequestParam(required = false) Integer brandId,
                                          @RequestParam(required = false) BigDecimal minPrice,
                                          @RequestParam(required = false) BigDecimal maxPrice,
                                          @RequestParam(defaultValue = "false") boolean discountedOnly,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "8") int size,
                                          @RequestParam(defaultValue = "id") String sortBy,
                                          @RequestParam(defaultValue = "asc") String direction) {
        var laptopPage = laptopService.filterActiveLaptops(
                keyword, categoryId, brandId, minPrice, maxPrice, discountedOnly, page, size, sortBy, direction);
        List<Map<String, Object>> items = laptopPage.getContent().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("sku", l.getSku());
            m.put("name", l.getName());
            m.put("brand", l.getBrandName());
            m.put("category", l.getCategoryName());
            m.put("cpu", l.getCpu());
            m.put("ram", l.getRam());
            m.put("storage", l.getStorage());
            m.put("salePrice", l.getSalePrice());
            m.put("listPrice", l.getListPrice());
            m.put("discountPercent", l.getDiscountPercent());
            m.put("stockQuantity", l.getStockQuantity());
            m.put("imageUrl", l.getDisplayImageUrl());
            return m;
        }).toList();
        return Map.of(
                "items", items,
                "totalElements", laptopPage.getTotalElements(),
                "totalPages", laptopPage.getTotalPages(),
                "number", laptopPage.getNumber()
        );
    }

    @GetMapping("/api/store/autocomplete")
    @ResponseBody
    public List<Map<String, Object>> autocomplete(@RequestParam(defaultValue = "") String q) {
        if (q == null || q.trim().length() < 1) return List.of();
        return laptopService.searchLaptops(q).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .limit(6)
                .map(l -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", l.getId());
                    m.put("name", l.getName());
                    m.put("sku", l.getSku());
                    m.put("price", l.getSalePrice());
                    m.put("image", l.getDisplayImageUrl());
                    return m;
                }).toList();
    }

    @GetMapping("/api/coupons/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkCoupon(@RequestParam String code,
                                                           @RequestParam BigDecimal subtotal) {
        try {
            BigDecimal discount = couponService.previewDiscount(code, subtotal);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "discount", discount,
                    "code", code.trim().toUpperCase(),
                    "message", "Áp dụng mã giảm giá thành công!"
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "discount", BigDecimal.ZERO,
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping("/san-pham/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails currentUser,
                         HttpSession session,
                         Model model) {
        Laptop laptop = laptopService.getLaptopById(id);
        laptopService.incrementViewCount(id);
        wishlistAndCompareService.recordRecentlyViewed(id, session);

        model.addAttribute("laptop", laptop);
        model.addAttribute("galleryImages", laptopService.getGalleryImages(laptop));
        model.addAttribute("relatedLaptops", laptopService.getRelatedLaptops(laptop, 4));
        model.addAttribute("recentLaptops", wishlistAndCompareService.getRecentlyViewedLaptops(session, id));
        model.addAttribute("reviews", reviewService.getApprovedReviews(id));
        model.addAttribute("avgRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));
        model.addAttribute("inWishlist", currentUser != null
                && wishlistAndCompareService.isInWishlist(currentUser.getAccountId(), id));
        return "store/detail";
    }

    @PostMapping("/san-pham/{id}/danh-gia")
    public String submitReview(@PathVariable Integer id,
                               @RequestParam int rating,
                               @RequestParam(required = false) String title,
                               @RequestParam String content,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            reviewService.submitReview(id, currentUser.getAccountId(), rating, title, content);
            ra.addFlashAttribute("successMessage",
                    "Cảm ơn bạn! Đánh giá đã được ghi nhận và sẽ hiển thị sau khi quản trị viên kiểm duyệt.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/san-pham/" + id;
    }

    @PostMapping("/gio-hang/them/{id}")
    public String addToCart(@PathVariable Integer id,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(defaultValue = "false") boolean buyNow,
                            @AuthenticationPrincipal CustomUserDetails currentUser,
                            HttpSession session,
                            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            cartService.addItem(id, quantity, accountId, session);
            if (buyNow) return "redirect:/dat-hang";
            Laptop laptop = laptopService.getLaptopById(id);
            ra.addFlashAttribute("successMessage", "Đã thêm '" + laptop.getName() + "' vào giỏ hàng.");
            return "redirect:/gio-hang";
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/san-pham/" + id;
        }
    }

    @PostMapping("/gio-hang/cap-nhat/{id}")
    public String updateCartQuantity(@PathVariable Integer id,
                                     @RequestParam int quantity,
                                     @AuthenticationPrincipal CustomUserDetails currentUser,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
        cartService.updateQuantity(id, quantity, accountId, session)
                .ifPresent(msg -> ra.addFlashAttribute("errorMessage", msg));
        return "redirect:/gio-hang";
    }

    @PostMapping("/gio-hang/xoa/{id}")
    public String removeFromCart(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
        cartService.removeItem(id, accountId, session);
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/gio-hang";
    }

    @GetMapping("/gio-hang")
    public String viewCart(@AuthenticationPrincipal CustomUserDetails currentUser,
                           HttpSession session, Model model) {
        Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
        List<CartItemDTO> items = cartService.getCartItems(accountId, session);
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        BigDecimal estShipping = subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0 || items.isEmpty()
                ? BigDecimal.ZERO : STANDARD_SHIPPING_FEE;
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("estimatedShipping", estShipping);
        model.addAttribute("freeShipThreshold", FREE_SHIP_THRESHOLD);
        return "store/cart";
    }

    @GetMapping("/dat-hang")
    public String showCheckoutForm(HttpSession session,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   Model model,
                                   RedirectAttributes ra) {
        Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
        List<CartItemDTO> items = cartService.getCartItems(accountId, session);
        if (items.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Giỏ hàng đang trống. Vui lòng chọn laptop trước khi đặt hàng.");
            return "redirect:/gio-hang";
        }
        CheckoutFormDTO form = new CheckoutFormDTO();
        List<CustomerAddress> savedAddresses = List.of();
        if (currentUser != null) {
            form.setRecipientName(currentUser.getFullName());
            form.setRecipientPhone(currentUser.getPhone());
            form.setEmail(currentUser.getEmail());
            var cusOpt = customerRepository.findByAccountId(currentUser.getAccountId());
            if (cusOpt.isPresent()) {
                Customer cus = cusOpt.get();
                form.setDeliveryAddress(cus.getAddress());
                savedAddresses = addressService.getAddressesForCustomer(cus.getId());
                if (!savedAddresses.isEmpty()) {
                    CustomerAddress def = savedAddresses.get(0);
                    form.setRecipientName(def.getRecipientName());
                    form.setRecipientPhone(def.getPhone());
                    form.setDeliveryAddress(def.getFullAddressText());
                }
            }
        }
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        BigDecimal shippingFee = calculateShippingFee(subtotal, "DELIVERY");
        model.addAttribute("checkoutForm", form);
        model.addAttribute("savedAddresses", savedAddresses);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("totalAmount", subtotal.add(shippingFee));
        model.addAttribute("freeShipThreshold", FREE_SHIP_THRESHOLD);
        model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH));
        model.addAttribute("shopProperties", shopProperties);
        return "store/checkout";
    }

    @PostMapping("/dat-hang")
    public String processCheckout(@Valid @ModelAttribute("checkoutForm") CheckoutFormDTO checkoutForm,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model,
                                  RedirectAttributes ra) {
        Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
        List<CartItemDTO> items = cartService.getCartItems(accountId, session);
        if (items.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Giỏ hàng đang trống.");
            return "redirect:/gio-hang";
        }
        BigDecimal subtotal = cartService.calculateSubtotal(items);
        BigDecimal shippingFee = calculateShippingFee(subtotal, checkoutForm.getDeliveryMethod());
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", items);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("totalAmount", subtotal.add(shippingFee));
            model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH));
            model.addAttribute("shopProperties", shopProperties);
            return "store/checkout";
        }
        try {
            Customer customer = customerService.findOrCreateForCheckout(
                    accountId,
                    checkoutForm.getRecipientName(),
                    checkoutForm.getEmail(),
                    checkoutForm.getRecipientPhone(),
                    checkoutForm.getDeliveryAddress()
            );
            PaymentMethod method = checkoutForm.getPaymentMethod() != null
                    ? checkoutForm.getPaymentMethod()
                    : PaymentMethod.BANK_TRANSFER;
            PlaceOrderRequest req = new PlaceOrderRequest();
            req.setCustomerId(customer.getId());
            req.setRecipientName(checkoutForm.getRecipientName());
            req.setRecipientPhone(checkoutForm.getRecipientPhone());
            req.setDeliveryAddress(checkoutForm.getDeliveryAddress());
            req.setPaymentMethod(method);
            req.setDiscount(BigDecimal.ZERO);
            req.setShippingFee(shippingFee);
            req.setCouponCode(checkoutForm.getCouponCode());
            req.setNote(checkoutForm.getNote());
            req.setItems(items.stream()
                    .map(i -> new OrderItemRequest(i.getLaptop().getId(), i.getQuantity()))
                    .toList());

            Order createdOrder = orderService.placeOrder(req, accountId);
            cartService.clearCart(accountId, session);
            if (method == PaymentMethod.CASH) {
                return "redirect:/thanh-toan/" + createdOrder.getId() + "/thanh-cong";
            }
            return "redirect:/thanh-toan/" + createdOrder.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("items", items);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("totalAmount", subtotal.add(shippingFee));
            model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH));
            model.addAttribute("shopProperties", shopProperties);
            return "store/checkout";
        }
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal, String deliveryMethod) {
        if ("PICKUP".equalsIgnoreCase(deliveryMethod)) return BigDecimal.ZERO;
        return subtotal.compareTo(FREE_SHIP_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING_FEE;
    }

    @GetMapping("/thanh-toan/{id}")
    public String showPaymentQrPage(@PathVariable Integer id,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    HttpServletRequest request,
                                    Model model) {
        if (currentUser == null) return "redirect:/login";
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        var payment = orderService.getPaymentByOrderId(id).orElse(null);
        if (payment != null && (payment.getPaymentMethod() == PaymentMethod.CASH
                || (payment.getPaymentStatus() != null && "PAID".equals(payment.getPaymentStatus().name())))) {
            return "redirect:/thanh-toan/" + id + "/thanh-cong";
        }
        String token = payment != null
                ? qrCodeService.createToken(order.getId(), order.getOrderCode(), payment.getTransactionCode(),
                        order.getTotalAmount(), order.getPaymentExpiresAt())
                : "";
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("shopProperties", shopProperties);
        model.addAttribute("qrPayUrl", qrCodeService.buildMobilePayUrl(request, order.getId(), token));
        model.addAttribute("tunnelActive", qrCodeService.isPublicTunnelActive());
        model.addAttribute("demoPaymentMode", paymentGatewayService.isDemoMode());
        return "store/order-success";
    }

    @GetMapping("/thanh-toan/{id}/qr.png")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable Integer id,
                                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                                 HttpServletRequest request) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        var payment = orderService.getPaymentByOrderId(id).orElse(null);
        String tx = payment != null ? payment.getTransactionCode() : "";
        String token = qrCodeService.createToken(order.getId(), order.getOrderCode(), tx,
                order.getTotalAmount(), order.getPaymentExpiresAt());
        String url = qrCodeService.buildMobilePayUrl(request, order.getId(), token);
        byte[] png = qrCodeService.generateQrPng(url, 260);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(png);
    }

    @GetMapping("/thanh-toan/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Integer id,
                                                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                                                HttpServletRequest request) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        var payment = orderService.getPaymentByOrderId(id).orElse(null);
        boolean paid = payment != null && payment.getPaymentStatus() != null
                && "PAID".equals(payment.getPaymentStatus().name());
        String tx = payment != null ? payment.getTransactionCode() : "";
        String token = qrCodeService.createToken(order.getId(), order.getOrderCode(), tx,
                order.getTotalAmount(), order.getPaymentExpiresAt());
        String qrPayUrl = qrCodeService.buildMobilePayUrl(request, order.getId(), token);
        return ResponseEntity.ok(Map.of(
                "paid", paid,
                "orderStatus", order.getStatus().name(),
                "redirectUrl", "/thanh-toan/" + id + "/thanh-cong",
                "qrPayUrl", qrPayUrl,
                "tunnelActive", qrCodeService.isPublicTunnelActive()
        ));
    }

    @GetMapping("/thanh-toan/{id}/thanh-cong")
    public String showPaymentSuccessPage(@PathVariable Integer id,
                                         @AuthenticationPrincipal CustomUserDetails currentUser,
                                         Model model) {
        if (currentUser == null) return "redirect:/login";
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("shopProperties", shopProperties);
        return "store/payment-success";
    }
}
