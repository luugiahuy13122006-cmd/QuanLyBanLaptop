package com.laptopshop.controller;

// Thành viên phụ trách: TV3 - Trương Xuân Đạt (tích hợp hoàn chỉnh với TV1/TV2/TV4/TV5)
// Điều khiển giao diện Website khách hàng: Trang chủ, Chi tiết sản phẩm, Tìm kiếm/Lọc, Giỏ hàng, Đặt hàng thật & Đơn hàng của tôi.

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.CartItemDTO;
import com.laptopshop.dto.CheckoutFormDTO;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.Customer;
import com.laptopshop.entity.Laptop;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.PaymentMethod;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.service.BrandService;
import com.laptopshop.service.CategoryService;
import com.laptopshop.service.CustomerService;
import com.laptopshop.service.LaptopService;
import com.laptopshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    private final LaptopService laptopService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;

    public StoreController(LaptopService laptopService,
                           CategoryService categoryService,
                           BrandService brandService,
                           CustomerService customerService,
                           CustomerRepository customerRepository,
                           OrderService orderService) {
        this.laptopService = laptopService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
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
        if (cart == null) {
            cart = new LinkedHashMap<>();
        }
        int newQty = Math.min(laptop.getStockQuantity(), cart.getOrDefault(id, 0) + qtyToAdd);
        cart.put(id, newQty);
        session.setAttribute("cart", cart);
        if (buyNow) {
            return "redirect:/dat-hang";
        }
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
        if (cart != null) {
            cart.remove(id);
            session.setAttribute("cart", cart);
        }
        ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/gio-hang";
    }

    @GetMapping("/gio-hang")
    public String viewCart(HttpSession session, Model model) {
        List<CartItemDTO> items = buildCartItems(session);
        BigDecimal subtotal = calculateSubtotal(items);
        model.addAttribute("items", items);
        model.addAttribute("subtotal", subtotal);
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
        model.addAttribute("shippingFee", DEFAULT_SHIPPING_FEE);
        model.addAttribute("totalAmount", subtotal.add(DEFAULT_SHIPPING_FEE));
        model.addAttribute("paymentMethods", PaymentMethod.values());
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
            model.addAttribute("shippingFee", DEFAULT_SHIPPING_FEE);
            model.addAttribute("totalAmount", subtotal.add(DEFAULT_SHIPPING_FEE));
            model.addAttribute("paymentMethods", PaymentMethod.values());
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
            req.setPaymentMethod(checkoutForm.getPaymentMethod());
            req.setShippingFee(DEFAULT_SHIPPING_FEE);
            req.setDiscount(BigDecimal.ZERO);
            req.setNote(checkoutForm.getNote());
            req.setItems(items.stream()
                    .map(i -> new OrderItemRequest(i.getLaptop().getId(), i.getQuantity()))
                    .toList());

            Order createdOrder = orderService.placeOrder(req, accountId);
            session.removeAttribute("cart");
            model.addAttribute("order", orderService.getOrderById(createdOrder.getId()));
            model.addAttribute("payment", orderService.getPaymentByOrderId(createdOrder.getId()).orElse(null));
            return "store/order-success";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("items", items);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", DEFAULT_SHIPPING_FEE);
            model.addAttribute("totalAmount", subtotal.add(DEFAULT_SHIPPING_FEE));
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "store/checkout";
        }
    }

    @GetMapping("/don-hang-cua-toi")
    public String myOrders(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        Customer customer = customerRepository.findByAccountId(currentUser.getAccountId()).orElse(null);
        List<Order> orders = customer != null
                ? orderService.getOrdersByCustomerId(customer.getId())
                : List.of();
        model.addAttribute("orders", orders);
        model.addAttribute("customer", customer);
        return "store/my-orders";
    }

    @PostMapping("/don-hang-cua-toi/{id}/huy")
    public String cancelMyOrder(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes ra) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            Customer customer = customerRepository.findByAccountId(currentUser.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng."));
            orderService.cancelOrderByCustomer(id, customer.getId(), currentUser.getAccountId());
            ra.addFlashAttribute("successMessage", "Đã hủy đơn hàng và hoàn lại tồn kho thành công!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/don-hang-cua-toi";
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDTO> buildCartItems(HttpSession session) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            return List.of();
        }
        List<CartItemDTO> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            try {
                Laptop laptop = laptopService.getLaptopById(entry.getKey());
                items.add(new CartItemDTO(laptop, entry.getValue()));
            } catch (Exception ignored) {
            }
        }
        return items;
    }

    private BigDecimal calculateSubtotal(List<CartItemDTO> items) {
        return items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
