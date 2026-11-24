package com.laptopshop.controller;

import com.laptopshop.service.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public String showStatistics(@RequestParam(required = false) Integer year, Model model) {
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("totalRevenue", statisticsService.getTotalRevenue());
        model.addAttribute("totalImportCost", statisticsService.getTotalImportCost());
        model.addAttribute("monthlyRevenues", statisticsService.getMonthlyRevenue(selectedYear));
        model.addAttribute("monthlyImportCosts", statisticsService.getMonthlyImportCost(selectedYear));
        model.addAttribute("topSellingLaptops", statisticsService.getTopSellingLaptops(5));
        model.addAttribute("statusCounts", statisticsService.getOrderStatusCounts());
        model.addAttribute("lowStockLaptops", statisticsService.getLowStockLaptops(5));
        return "admin/statistics/index";
    }
}
