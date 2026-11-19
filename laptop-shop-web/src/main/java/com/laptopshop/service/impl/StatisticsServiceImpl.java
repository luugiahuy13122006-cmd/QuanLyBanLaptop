package com.laptopshop.service.impl;

import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.Laptop;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.OrderDetailRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.service.StatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final LaptopRepository laptopRepository;

    public StatisticsServiceImpl(OrderRepository orderRepository,
                                 OrderDetailRepository orderDetailRepository,
                                 LaptopRepository laptopRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.laptopRepository = laptopRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal total = orderRepository.calculateTotalRevenue();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyRevenueDTO> getMonthlyRevenue(int year) {
        return orderRepository.getMonthlyRevenueByYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSellingLaptopDTO> getTopSellingLaptops(int limit) {
        return orderDetailRepository.findTopSellingLaptops(PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusCountDTO> getOrderStatusCounts() {
        return orderRepository.countOrdersByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Laptop> getLowStockLaptops(int threshold) {
        return laptopRepository.findLowStockLaptops(threshold);
    }
}
