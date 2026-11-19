package com.laptopshop.service;

import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.Laptop;

import java.math.BigDecimal;
import java.util.List;

public interface StatisticsService {
    BigDecimal getTotalRevenue();
    List<MonthlyRevenueDTO> getMonthlyRevenue(int year);
    List<TopSellingLaptopDTO> getTopSellingLaptops(int limit);
    List<OrderStatusCountDTO> getOrderStatusCounts();
    List<Laptop> getLowStockLaptops(int threshold);
}
