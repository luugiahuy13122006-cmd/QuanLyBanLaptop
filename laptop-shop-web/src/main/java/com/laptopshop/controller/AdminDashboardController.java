package com.laptopshop.controller;

import com.laptopshop.entity.PaymentStatus;
import com.laptopshop.repository.*;
import com.laptopshop.service.OrderService;
import com.laptopshop.service.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final LaptopRepository laptopRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final StatisticsService statisticsService;
    private final OrderService orderService;

    public AdminDashboardController(LaptopRepository laptopRepository,
                                    CategoryRepository categoryRepository,
                                    BrandRepository brandRepository,
                                    CustomerRepository customerRepository,
                                    OrderRepository orderRepository,
                                    AccountRepository accountRepository,
                                    PaymentRepository paymentRepository,
                                    ImportReceiptRepository importReceiptRepository,
                                    StatisticsService statisticsService,
                                    OrderService orderService) {
        this.laptopRepository = laptopRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.importReceiptRepository = importReceiptRepository;
        this.statisticsService = statisticsService;
        this.orderService = orderService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalLaptops", laptopRepository.count());
        model.addAttribute("totalCategories", categoryRepository.count());
        model.addAttribute("totalBrands", brandRepository.count());
        model.addAttribute("totalCustomers", customerRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("totalAccounts", accountRepository.count());
        model.addAttribute("totalRevenue", statisticsService.getTotalRevenue());
        model.addAttribute("totalImportCost", statisticsService.getTotalImportCost());
        model.addAttribute("lowStockLaptops", statisticsService.getLowStockLaptops(5));
        model.addAttribute("pendingProofCount", paymentRepository.countByPaymentStatus(PaymentStatus.PENDING_CONFIRMATION));
        model.addAttribute("pendingImportCount", importReceiptRepository.countByStatus(
                com.laptopshop.entity.ImportReceiptStatus.PENDING));
        model.addAttribute("recentOrders", orderService.getOrders(null, "",
                org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by("orderDate").descending())));
        return "admin/dashboard";
    }
}
