package com.laptopshop.service.impl;

import com.laptopshop.dto.MonthlyImportCostDTO;
import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.dto.TopSellingLaptopDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Laptop;
import com.laptopshop.entity.OrderStatus;
import com.laptopshop.repository.*;
import com.laptopshop.service.StatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final LaptopRepository laptopRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final AccountRepository accountRepository;

    public StatisticsServiceImpl(OrderRepository orderRepository,
                                 OrderDetailRepository orderDetailRepository,
                                 LaptopRepository laptopRepository,
                                 ImportReceiptRepository importReceiptRepository,
                                 AccountRepository accountRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.laptopRepository = laptopRepository;
        this.importReceiptRepository = importReceiptRepository;
        this.accountRepository = accountRepository;
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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalImportCost() {
        BigDecimal total = importReceiptRepository.calculateTotalImportCost();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyImportCostDTO> getMonthlyImportCost(int year) {
        return importReceiptRepository.getMonthlyImportCost(year);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRevenueToday() {
        BigDecimal v = orderRepository.calculateRevenueSince(LocalDate.now().atStartOfDay());
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRevenueThisWeek() {
        LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        BigDecimal v = orderRepository.calculateRevenueSince(startOfWeek);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRevenueThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal v = orderRepository.calculateRevenueSince(startOfMonth);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalGrossProfit() {
        BigDecimal rev = getTotalRevenue();
        BigDecimal cogs = orderDetailRepository.calculateTotalCogs();
        return rev.subtract(cogs != null ? cogs : BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public double getCancellationRatePercent() {
        List<OrderStatusCountDTO> counts = orderRepository.countOrdersByStatus();
        long total = counts.stream().mapToLong(OrderStatusCountDTO::getCount).sum();
        if (total == 0) return 0.0;
        long cancelled = counts.stream()
                .filter(c -> c.getStatus() == OrderStatus.CANCELLED
                        || c.getStatus() == OrderStatus.RETURNED
                        || c.getStatus() == OrderStatus.REFUNDED)
                .mapToLong(OrderStatusCountDTO::getCount).sum();
        return Math.round((cancelled * 1000.0) / total) / 10.0;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageOrderValue() {
        List<OrderStatusCountDTO> counts = orderRepository.countOrdersByStatus();
        long validOrders = counts.stream()
                .filter(c -> c.getStatus() != OrderStatus.CANCELLED
                        && c.getStatus() != OrderStatus.RETURNED
                        && c.getStatus() != OrderStatus.REFUND_REQUESTED
                        && c.getStatus() != OrderStatus.REFUNDED)
                .mapToLong(OrderStatusCountDTO::getCount).sum();
        if (validOrders == 0) return BigDecimal.ZERO;
        return getTotalRevenue().divide(BigDecimal.valueOf(validOrders), 0, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStaffPerformance() {
        List<Object[]> rows = orderRepository.getStaffSalesPerformance();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer accId = row[0] instanceof Number n ? n.intValue() : null;
            if (accId == null) continue;
            Account acc = accountRepository.findById(accId).orElse(null);
            if (acc == null || acc.getRole() == null
                    || ("CUSTOMER".equalsIgnoreCase(acc.getRole().getRoleName()))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", acc.getUsername());
            item.put("fullName", acc.getFullName());
            item.put("role", acc.getRole().getRoleName());
            item.put("orderCount", row[1]);
            item.put("revenue", row[2]);
            result.add(item);
        }
        return result;
    }
}
