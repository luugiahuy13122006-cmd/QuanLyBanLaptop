package com.laptopshop.service;

import com.laptopshop.dto.MonthlyImportCostDTO;
import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.Laptop;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StatisticsService {
    BigDecimal getTotalRevenue();
    List<MonthlyRevenueDTO> getMonthlyRevenue(int year);
    List<TopSellingLaptopDTO> getTopSellingLaptops(int limit);
    List<OrderStatusCountDTO> getOrderStatusCounts();
    List<Laptop> getLowStockLaptops(int threshold);
    BigDecimal getTotalImportCost();
    List<MonthlyImportCostDTO> getMonthlyImportCost(int year);

    BigDecimal getRevenueToday();
    BigDecimal getRevenueThisWeek();
    BigDecimal getRevenueThisMonth();
    BigDecimal getTotalGrossProfit();
    double getCancellationRatePercent();
    BigDecimal getAverageOrderValue();
    List<Map<String, Object>> getStaffPerformance();
}
