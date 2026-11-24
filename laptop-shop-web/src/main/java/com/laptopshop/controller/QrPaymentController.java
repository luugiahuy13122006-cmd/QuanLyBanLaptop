package com.laptopshop.controller;

import com.laptopshop.config.ShopProperties;
import com.laptopshop.entity.Order;
import com.laptopshop.service.OrderService;
import com.laptopshop.service.PaymentGatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/qr-pay")
public class QrPaymentController {

    private final OrderService orderService;
    private final ShopProperties shopProperties;
    private final PaymentGatewayService paymentGatewayService;

    public QrPaymentController(OrderService orderService,
                               ShopProperties shopProperties,
                               PaymentGatewayService paymentGatewayService) {
        this.orderService = orderService;
        this.shopProperties = shopProperties;
        this.paymentGatewayService = paymentGatewayService;
    }

    @GetMapping("/{id}/{token}")
    public String showMobileConfirmPage(@PathVariable Integer id,
                                        @PathVariable String token,
                                        Model model) {
        Order order = orderService.getOrderForQrToken(id, token);
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.getPaymentByOrderId(id).orElse(null));
        model.addAttribute("shopProperties", shopProperties);
        model.addAttribute("qrToken", token);
        model.addAttribute("demoPaymentMode", paymentGatewayService.isDemoMode());
        return "store/qr-mobile-pay";
    }

    @PostMapping("/{id}/{token}/confirm")
    public String confirmFromMobile(@PathVariable Integer id,
                                    @PathVariable String token,
                                    RedirectAttributes ra) {
        try {
            orderService.confirmBankTransferByQrToken(id, token);
            ra.addFlashAttribute("successMessage", "Xác nhận chuyển khoản thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/qr-pay/" + id + "/" + token;
    }
}
