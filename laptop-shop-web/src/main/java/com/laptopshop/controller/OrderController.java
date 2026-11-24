package com.laptopshop.controller;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.ShopProperties;
import com.laptopshop.dto.OrderItemRequest;
import com.laptopshop.dto.PlaceOrderRequest;
import com.laptopshop.dto.RefundRequestDTO;
import com.laptopshop.entity.Order;
import com.laptopshop.entity.OrderStatus;
import com.laptopshop.entity.Payment;
import com.laptopshop.entity.PaymentMethod;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.service.CustomerService;
import com.laptopshop.service.FileStorageService;
import com.laptopshop.service.OrderService;
import com.laptopshop.util.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final LaptopRepository laptopRepository;
    private final FileStorageService fileStorageService;
    private final ShopProperties shopProperties;

    public OrderController(OrderService orderService,
                           CustomerService customerService,
                           LaptopRepository laptopRepository,
                           FileStorageService fileStorageService,
                           ShopProperties shopProperties) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.laptopRepository = laptopRepository;
        this.fileStorageService = fileStorageService;
        this.shopProperties = shopProperties;
    }

    // ==================== ADMIN ====================

    @GetMapping("/admin/orders")
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        String sortField = switch (sortBy) {
            case "orderCode", "totalAmount", "status", "id" -> sortBy;
            default -> "orderDate";
        };
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();
        Page<Order> orderPage = orderService.getOrders(status, keyword,
                PageRequest.of(PaginationUtils.clampPage(page), PaginationUtils.clampSize(size, 8, 100), sort));
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("statuses", OrderStatus.values());
        List<Payment> pendingProofs = orderService.getPaymentsAwaitingConfirmation();
        model.addAttribute("pendingProofCount", pendingProofs.size());
        return "admin/orders/list";
    }

    @GetMapping("/admin/orders/{id}")
    public String viewOrderDetail(@PathVariable Integer id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("statuses", java.util.Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.REFUND_REQUESTED && s != OrderStatus.REFUNDED)
                .toList());
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("histories", orderService.getOrderStatusHistory(id));
        return "admin/orders/detail";
    }

    @GetMapping("/admin/orders/create")
    public String showCreateOrderForm(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("laptops", laptopRepository.findAllActiveWithDetails());
        model.addAttribute("paymentMethods", List.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH));
        return "admin/orders/create";
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
            @RequestParam(defaultValue = "BANK_TRANSFER") PaymentMethod paymentMethod,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            PlaceOrderRequest req = new PlaceOrderRequest();
            req.setCustomerId(customerId);
            req.setRecipientName(recipientName);
            req.setRecipientPhone(recipientPhone);
            req.setDeliveryAddress(deliveryAddress);
            req.setDiscount(discount);
            req.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER);
            req.setNote(note);
            req.setItems(List.of(new OrderItemRequest(laptopId, quantity)));

            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            Order created = orderService.placeOrder(req, accountId);
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
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            orderService.updateOrderStatus(id, status, accountId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái đơn hàng thành công sang: " + status.getDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/confirm-payment")
    public String confirmPayment(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            orderService.confirmPayment(id, accountId);
            ra.addFlashAttribute("successMessage", "Đã xác nhận thanh toán chuyển khoản thành công.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/reject-proof")
    public String rejectPaymentProof(
            @PathVariable Integer id,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            orderService.rejectPaymentProof(id, accountId, reason);
            ra.addFlashAttribute("successMessage", "Đã từ chối ảnh minh chứng và yêu cầu khách tải lại.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/confirm-refund")
    public String confirmRefund(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer accountId = currentUser != null ? currentUser.getAccountId() : null;
            orderService.confirmRefund(id, accountId);
            ra.addFlashAttribute("successMessage", "Đã xác nhận hoàn tiền thành công. Tồn kho đã được khôi phục.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/tracking")
    public String updateTracking(
            @PathVariable Integer id,
            @RequestParam(required = false) String carrierName,
            @RequestParam(required = false) String trackingCode,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes ra) {
        try {
            Integer actorId = currentUser != null ? currentUser.getAccountId() : null;
            orderService.updateShippingInfo(id, carrierName, trackingCode, actorId);
            ra.addFlashAttribute("successMessage", "Cập nhật thông tin vận chuyển & mã vận đơn thành công.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/admin/orders/{id}/invoice")
    public String printAdminInvoice(@PathVariable Integer id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("shopProperties", shopProperties);
        return "admin/orders/invoice";
    }

    @GetMapping("/admin/orders/pending-proofs")
    public String listPendingProofs(Model model) {
        model.addAttribute("pendingPayments", orderService.getPaymentsAwaitingConfirmation());
        return "admin/orders/pending-proofs";
    }

    // Xem ảnh minh chứng (admin/staff, không cần kiểm tra chủ sở hữu)
    @GetMapping("/admin/payment-proof/{filename:.+}")
    public ResponseEntity<Resource> serveProofAdmin(@PathVariable String filename) {
        Resource resource = fileStorageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .contentType(fileStorageService.mediaTypeOf(filename))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ==================== CUSTOMER ====================

    @GetMapping("/don-hang-cua-toi")
    public String myOrders(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) OrderStatus status,
                           @RequestParam(defaultValue = "0") int page,
                           @AuthenticationPrincipal CustomUserDetails currentUser,
                           Model model) {
        if (currentUser == null) return "redirect:/login";
        List<Order> all = orderService.getOrdersByCustomerId(
                currentUser.getCustomerId() != null ? currentUser.getCustomerId() : -1);
        List<Order> filtered = all.stream().filter(o -> {
            if (status != null && o.getStatus() != status) return false;
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim().toLowerCase();
                return o.getOrderCode().toLowerCase().contains(kw);
            }
            return true;
        }).toList();
        int safePage = PaginationUtils.clampPage(page);
        int pageSize = 8;
        int start = Math.min(safePage * pageSize, filtered.size());
        int end = Math.min(start + pageSize, filtered.size());
        List<Order> pageContent = filtered.subList(start, end);
        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / pageSize));
        model.addAttribute("orders", pageContent);
        model.addAttribute("allOrderCount", all.size());
        model.addAttribute("filteredCount", filtered.size());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("shopProperties", shopProperties);
        return "customer/my-orders";
    }

    @GetMapping("/don-hang-cua-toi/{id}/hoa-don")
    public String printCustomerInvoice(@PathVariable Integer id,
                                       @AuthenticationPrincipal CustomUserDetails currentUser,
                                       Model model) {
        if (currentUser == null) return "redirect:/login";
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("shopProperties", shopProperties);
        return "admin/orders/invoice";
    }

    @GetMapping("/don-hang-cua-toi/{id}")
    public String myOrderDetail(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                Model model) {
        if (currentUser == null) return "redirect:/login";
        Order order = orderService.getOrderForCustomer(id, currentUser.getAccountId());
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("histories", orderService.getOrderStatusHistory(id));
        model.addAttribute("shopProperties", shopProperties);
        return "customer/order-detail";
    }

    @PostMapping("/don-hang-cua-toi/{id}/huy")
    public String cancelMyOrder(@PathVariable Integer id,
                                @RequestParam(required = false) String reason,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            orderService.cancelOrderByCustomer(id, currentUser.getAccountId(), reason);
            ra.addFlashAttribute("successMessage", "Đơn hàng đã được hủy thành công.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/don-hang-cua-toi/" + id;
    }

    @PostMapping("/don-hang-cua-toi/{id}/yeu-cau-hoan-tien")
    public String requestRefund(@PathVariable Integer id,
                                @Valid @ModelAttribute RefundRequestDTO dto,
                                org.springframework.validation.BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            ra.addFlashAttribute("errorMessage", msg != null ? msg : "Thông tin hoàn tiền không hợp lệ.");
            return "redirect:/don-hang-cua-toi/" + id;
        }
        try {
            orderService.requestRefund(id, currentUser.getAccountId(), dto);
            ra.addFlashAttribute("successMessage", "Yêu cầu hoàn tiền đã được gửi. Chúng tôi sẽ xử lý trong 1-3 ngày làm việc.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/don-hang-cua-toi/" + id;
    }

    @PostMapping("/don-hang-cua-toi/{id}/upload-minh-chung")
    public String uploadPaymentProof(@PathVariable Integer id,
                                     @RequestParam("proofFile") MultipartFile file,
                                     @RequestParam(required = false) String redirectTo,
                                     @AuthenticationPrincipal CustomUserDetails currentUser,
                                     RedirectAttributes ra) {
        if (currentUser == null) return "redirect:/login";
        try {
            orderService.uploadPaymentProof(id, currentUser.getAccountId(), file);
            ra.addFlashAttribute("successMessage", "Tải ảnh minh chứng thành công! Cửa hàng sẽ xác nhận trong thời gian sớm nhất.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        if ("checkout".equalsIgnoreCase(redirectTo)) {
            return "redirect:/thanh-toan/" + id;
        }
        return "redirect:/don-hang-cua-toi/" + id;
    }

    // Xem ảnh minh chứng (chỉ chủ đơn hàng)
    @GetMapping("/don-hang-cua-toi/proof/{filename:.+}")
    public ResponseEntity<Resource> serveProofCustomer(@PathVariable String filename,
                                                        @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        if (!orderService.isPaymentProofOwnedBy(filename, currentUser.getAccountId())) {
            return ResponseEntity.status(403).build();
        }
        Resource resource = fileStorageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .contentType(fileStorageService.mediaTypeOf(filename))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ==================== API ====================

    @PostMapping("/orders/place")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @ResponseBody
    public ResponseEntity<?> placeOrderApi(@Valid @RequestBody PlaceOrderRequest request,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            Integer actorId = currentUser != null ? currentUser.getAccountId() : null;
            Order order = orderService.placeOrder(request, actorId);
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
