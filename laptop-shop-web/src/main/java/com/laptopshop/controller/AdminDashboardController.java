package com.laptopshop.controller;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Trang tổng quan quản trị (Admin/Staff Dashboard).

import com.laptopshop.repository.*;
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
    private final StatisticsService statisticsService;

    public AdminDashboardController(LaptopRepository laptopRepository,
                                    CategoryRepository categoryRepository,
                                    BrandRepository brandRepository,
                                    CustomerRepository customerRepository,
                                    OrderRepository orderRepository,
                                    AccountRepository accountRepository,
                                    StatisticsService statisticsService) {
        this.laptopRepository = laptopRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.statisticsService = statisticsService;
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
        model.addAttribute("lowStockLaptops", statisticsService.getLowStockLaptops(5));
        return "admin/dashboard";
    }
}
