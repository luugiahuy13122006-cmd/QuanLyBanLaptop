package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.ShopProperties;
import com.laptopshop.dto.CartItemDTO;
import com.laptopshop.dto.CheckoutFormDTO;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.Customer;
import com.laptopshop.entity.Laptop;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.PaymentMethod;
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

    private final LaptopService laptopService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;
    private final ShopProperties shopProperties;
    private final QrCodeService qrCodeService;

    public StoreController(LaptopService laptopService,
                           CategoryService categoryService,
                           BrandService brandService,
                           CustomerService customerService,
                           CustomerRepository customerRepository,
                           OrderService orderService,
                           ShopProperties shopProperties,
                           QrCodeService qrCodeService) {
        this.laptopService = laptopService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
        this.shopProperties = shopProperties;
        this.qrCodeService = qrCodeService;
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
                       Model model) {
        var laptopPage = laptopService.filterActiveLaptops(
                keyword, categoryId, brandId, minPrice, maxPrice, discountedOnly, page, size, sortBy, direction);
        model.addAttribute("laptopPage", laptopPage);
        model.addAttribute("flashSaleLaptops", laptopService.getTopDiscountedLaptops(4));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
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

    @GetMapping("/san-pham/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Laptop laptop = laptopService.getLaptopById(id);
        model.addAttribute("laptop", laptop);
        model.addAttribute("galleryImages", laptopService.getGalleryImages(laptop));
        model.addAttribute("relatedLaptops", laptopService.getRelatedLaptops(laptop, 4));
        return "store/detail";
    }

    @PostMapping("/gio-hang/them/{id}")
    @SuppressWarnings("unchecked")
    public String addToCart(@PathVariable Integer id,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(defaultValue = "false") boolean buyNow,
                            HttpSession session,
                            RedirectAttributes ra) {
        Laptop laptop = laptopService.getLaptopById(id);
        if (!Boolean.TRUE.equals(laptop.getIsActive()) || laptop.getStockQuantity() <= 0) {
            ra.addFlashAttribute("errorMessage", "Sản phẩm hiện đã hết hàng hoặc ngừng kinh doanh.");
            return "redirect:/san-pham/" + id;
        }
        int qtyToAdd = Math.max(1, quantity);
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute("cart");
        if (cart == null) cart = new LinkedHashMap<>();
        int newQty = Math.min(laptop.getStockQuantity(), cart.getOrDefault(id, 0) + qtyToAdd);
        cart.put(id, newQty);
        session.setAttribute("cart", cart);
        if (buyNow) return "redirect:/dat-hang";
        ra.addFlashAttribute("successMessage", "Đã thêm '" + laptop.getName() + "' vào giỏ hàng.");
        return "redirect:/gio-hang";
    }

    @PostMapping("/gio-hang/cap-nhat/{id}")
    @SuppressWarnings("unchecked")
    public String updateCartQuantity(@PathVariable Integer id,
                                     @RequestParam int quantity,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute("cart");
        if (cart != null) {
            if (quantity <= 0) {
                cart.remove(id);
            } else {
                Laptop laptop = laptopService.getLaptopById(id);
                if (quantity > laptop.getStockQuantity()) {
                    ra.addFlashAttribute("errorMessage",
                            "Sản phẩm '" + laptop.getName() + "' chỉ còn " + laptop.getStockQuantity() + " chiếc trong kho.");
                    cart.put(id, laptop.getStockQuantity());
                } else {
                    cart.put(id, quantity);
                }
            }
            session.setAttribute("cart", cart);
        }
        return "redirect:/gio-hang";
    }

    @PostMapping("/gio-hang/xoa/{id}")
    @SuppressWarnings("unchecked")
    public String removeFromCart(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute("cart");
        if (cart != null) { cart.remove(id); session.setAttribute("cart", cart); }
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/gio-hang";
    }

    @GetMapping("/gio-hang")
    public String viewCart(HttpSession session, Model model) {
        List<CartItemDTO> items = buildCartItems(session);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", calculateSubtotal(items));
        return "store/cart";
    }

    @GetMapping("/dat-hang")
    public String showCheckoutForm(HttpSession session,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   Model model,
                                   RedirectAttributes ra) {
        List<CartItemDTO> items = buildCartItems(session);
        if (items.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Giỏ hàng đang trống. Vui lòng chọn laptop trước khi đặt hàng.");
            return "redirect:/gio-hang";
        }
        CheckoutFormDTO form = new CheckoutFormDTO();
        if (currentUser != null) {
            form.setRecipientName(currentUser.getFullName());
            form.setRecipientPhone(currentUser.getPhone());
            form.setEmail(currentUser.getEmail());
            customerRepository.findByAccountId(currentUser.getAccountId())
                    .ifPresent(c -> form.setDeliveryAddress(c.getAddress()));
        }
        BigDecimal subtotal = calculateSubtotal(items);
        model.addAttribute("checkoutForm", form);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("totalAmount", subtotal);
        model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER));
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
        List<CartItemDTO> items = buildCartItems(session);
        if (items.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Giỏ hàng đang trống.");
            return "redirect:/gio-hang";
        }
        BigDecimal subtotal = calculateSubtotal(items);
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", items);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("totalAmount", subtotal);
            model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER));
            model.addAttribute("shopProperties", shopProperties);
            return "store/checkout";
        }
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            Customer customer = customerService.findOrCreateForCheckout(
                    accountId,
                    checkoutForm.getRecipientName(),
                    checkoutForm.getEmail(),
                    checkoutForm.getRecipientPhone(),
                    checkoutForm.getDeliveryAddress()
            );
            PlaceOrderRequest req = new PlaceOrderRequest();
            req.setCustomerId(customer.getId());
            req.setRecipientName(checkoutForm.getRecipientName());
            req.setRecipientPhone(checkoutForm.getRecipientPhone());
            req.setDeliveryAddress(checkoutForm.getDeliveryAddress());
            // Sản phẩm laptop giá trị cao chỉ áp dụng chuyển khoản ngân hàng
            req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            req.setDiscount(BigDecimal.ZERO);
            req.setNote(checkoutForm.getNote());
            req.setItems(items.stream()
                    .map(i -> new OrderItemRequest(i.getLaptop().getId(), i.getQuantity()))
                    .toList());

            Order createdOrder = orderService.placeOrder(req, accountId);
            session.removeAttribute("cart");
            return "redirect:/thanh-toan/" + createdOrder.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("items", items);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("totalAmount", subtotal);
            model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER));
            model.addAttribute("shopProperties", shopProperties);
            return "store/checkout";
        }
    }

    @GetMapping("/thanh-toan/{id}")
    public String showPaymentQrPage(@PathVariable Integer id,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    HttpServletRequest request,
                                    Model model) {
        if (currentUser == null) return "redirect:/login";
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        var payment = orderService.getPaymentByOrderId(id).orElse(null);
        if (payment != null && payment.getPaymentStatus() != null
                && "PAID".equals(payment.getPaymentStatus().name())) {
            return "redirect:/thanh-toan/" + id + "/thanh-cong";
        }
        String token = payment != null
                ? qrCodeService.createToken(order.getId(), order.getOrderCode(), payment.getTransactionCode())
                : "";
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("shopProperties", shopProperties);
        model.addAttribute("qrPayUrl", qrCodeService.buildMobilePayUrl(request, order.getId(), token));
        model.addAttribute("tunnelActive", qrCodeService.isPublicTunnelActive());
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
        String token = qrCodeService.createToken(order.getId(), order.getOrderCode(), tx);
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
        String token = qrCodeService.createToken(order.getId(), order.getOrderCode(), tx);
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

    @SuppressWarnings("unchecked")
    private List<CartItemDTO> buildCartItems(HttpSession session) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) return List.of();
        List<CartItemDTO> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            try { items.add(new CartItemDTO(laptopService.getLaptopById(entry.getKey()), entry.getValue())); }
            catch (Exception ignored) {}
        }
        return items;
    }

    private BigDecimal calculateSubtotal(List<CartItemDTO> items) {
        return items.stream().map(CartItemDTO::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
