package com.laptopshop.controller;

import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.OrderStatus;
import com.laptopshop.entity.PaymentMethod;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.service.CustomerService;
import com.laptopshop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final LaptopRepository laptopRepository;

    public OrderController(OrderService orderService,
                           CustomerService customerService,
                           LaptopRepository laptopRepository) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.laptopRepository = laptopRepository;
    }

    // ==================== GIAO DIỆN QUẢN TRỊ ĐƠN HÀNG ====================

    @GetMapping("/admin/orders")
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Model model) {

        Page<Order> orderPage = orderService.getOrders(
                status, PageRequest.of(page, size, Sort.by("orderDate").descending()));

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "orders/list";
    }

    @GetMapping("/admin/orders/{id}")
    public String viewOrderDetail(@PathVariable Integer id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("histories", orderService.getOrderStatusHistory(id));
        return "orders/detail";
    }

    @GetMapping("/admin/orders/create")
    public String showCreateOrderForm(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("laptops", laptopRepository.findAllActiveWithDetails());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "orders/create";
    }

    @PostMapping("/admin/orders/create")
    public String createOrderFromWeb(
            @RequestParam Integer customerId,
            @RequestParam Integer laptopId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String recipientName,
            @RequestParam(required = false) String recipientPhone,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(defaultValue = "0") BigDecimal discount,
            @RequestParam(defaultValue = "30000") BigDecimal shippingFee,
            @RequestParam(defaultValue = "CASH") PaymentMethod paymentMethod,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {
        try {
            PlaceOrderRequest req = new PlaceOrderRequest();
            req.setCustomerId(customerId);
            req.setRecipientName(recipientName);
            req.setRecipientPhone(recipientPhone);
            req.setDeliveryAddress(deliveryAddress);
            req.setDiscount(discount);
            req.setShippingFee(shippingFee);
            req.setPaymentMethod(paymentMethod);
            req.setNote(note);
            req.setItems(List.of(new OrderItemRequest(laptopId, quantity)));

            Order created = orderService.placeOrder(req);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo đơn hàng thành công! Mã đơn: " + created.getOrderCode());
            return "redirect:/admin/orders/" + created.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/orders/create";
        }
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái đơn hàng thành công sang: " + status.getDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ==================== API ĐẶT HÀNG (HỖ TRỢ ĐA LUỒNG) ====================

    @PostMapping("/orders/place")
    @ResponseBody
    public ResponseEntity<?> placeOrderApi(@Valid @RequestBody PlaceOrderRequest request) {
        try {
            Order order = orderService.placeOrder(request);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "orderId", order.getId(),
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "message", "Đặt hàng thành công!"
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", ex.getMessage()
            ));
        }
    }
}
