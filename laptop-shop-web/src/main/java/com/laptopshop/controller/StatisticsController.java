package com.laptopshop.controller;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
// Thống kê & báo cáo kinh doanh; xuất báo cáo doanh thu (in trực tiếp / tải PDF / tải CSV mở bằng Excel).

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.dto.MonthlyImportCostDTO;
import com.laptopshop.dto.MonthlyRevenueDTO;
import com.laptopshop.dto.OrderStatusCountDTO;
import com.laptopshop.service.StatisticsService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/statistics")
public class StatisticsController {

    /** Một dòng báo cáo theo tháng: doanh thu bán hàng đối chiếu chi phí nhập kho. */
    public static class MonthRow {
        private final int month;
        private final long orders;
        private final BigDecimal revenue;
        private final long receipts;
        private final BigDecimal importCost;
        private int revenuePct;

        public MonthRow(int month, long orders, BigDecimal revenue, long receipts, BigDecimal importCost) {
            this.month = month;
            this.orders = orders;
            this.revenue = revenue;
            this.receipts = receipts;
            this.importCost = importCost;
        }

        public int getRevenuePct() { return revenuePct; }
        public void setRevenuePct(int revenuePct) { this.revenuePct = revenuePct; }

        public int getMonth() { return month; }
        public long getOrders() { return orders; }
        public BigDecimal getRevenue() { return revenue; }
        public long getReceipts() { return receipts; }
        public BigDecimal getImportCost() { return importCost; }
        public BigDecimal getBalance() { return revenue.subtract(importCost); }
    }

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    private int resolveYear(Integer year) {
        int current = LocalDate.now().getYear();
        if (year == null) return current;
        return (year < 2000 || year > current + 1) ? current : year;
    }

    @GetMapping
    public String showStatistics(@RequestParam(required = false) Integer year, Model model) {
        int selectedYear = resolveYear(year);
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

    /** Gộp doanh thu và chi phí nhập theo đủ 12 tháng của năm (tháng không phát sinh = 0). */
    private List<MonthRow> buildMonthRows(int year) {
        List<MonthlyRevenueDTO> revenues = statisticsService.getMonthlyRevenue(year);
        List<MonthlyImportCostDTO> costs = statisticsService.getMonthlyImportCost(year);
        List<MonthRow> rows = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            long orders = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            for (MonthlyRevenueDTO r : revenues) {
                if (r.getMonth() != null && r.getMonth() == month) {
                    orders = r.getTotalOrders() != null ? r.getTotalOrders() : 0;
                    revenue = r.getRevenue() != null ? r.getRevenue() : BigDecimal.ZERO;
                }
            }
            long receipts = 0;
            BigDecimal cost = BigDecimal.ZERO;
            for (MonthlyImportCostDTO c : costs) {
                if (c.getMonth() != null && c.getMonth() == month) {
                    receipts = c.getReceiptCount() != null ? c.getReceiptCount() : 0;
                    cost = c.getCost() != null ? c.getCost() : BigDecimal.ZERO;
                }
            }
            rows.add(new MonthRow(month, orders, revenue, receipts, cost));
        }
        return rows;
    }

    /** Trang báo cáo doanh thu chuẩn A4: in trực tiếp hoặc lưu thành PDF. */
    @GetMapping("/report")
    public String report(@RequestParam(required = false) Integer year,
                         @AuthenticationPrincipal CustomUserDetails currentUser,
                         Model model) {
        int y = resolveYear(year);
        List<MonthRow> rows = buildMonthRows(y);
        long totalOrders = rows.stream().mapToLong(MonthRow::getOrders).sum();
        long totalReceipts = rows.stream().mapToLong(MonthRow::getReceipts).sum();
        BigDecimal yearRevenue = rows.stream().map(MonthRow::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yearImportCost = rows.stream().map(MonthRow::getImportCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal maxRevenue = rows.stream().map(MonthRow::getRevenue).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        if (maxRevenue.signum() > 0) {
            rows.forEach(r -> r.setRevenuePct(r.getRevenue()
                    .multiply(BigDecimal.valueOf(100)).divide(maxRevenue, 0, java.math.RoundingMode.HALF_UP).intValue()));
        }
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int i = currentYear; i >= currentYear - 5; i--) years.add(i);
        model.addAttribute("years", years);

        model.addAttribute("year", y);
        model.addAttribute("monthRows", rows);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalReceipts", totalReceipts);
        model.addAttribute("yearRevenue", yearRevenue);
        model.addAttribute("yearImportCost", yearImportCost);
        model.addAttribute("yearBalance", yearRevenue.subtract(yearImportCost));
        model.addAttribute("averageOrderValue", totalOrders > 0
                ? yearRevenue.divide(BigDecimal.valueOf(totalOrders), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        model.addAttribute("statusCounts", statisticsService.getOrderStatusCounts());
        model.addAttribute("topSellingLaptops", statisticsService.getTopSellingLaptops(5));
        model.addAttribute("lowStockLaptops", statisticsService.getLowStockLaptops(5));
        model.addAttribute("exportedAt", now);
        model.addAttribute("exporterName", currentUser != null ? currentUser.getFullName() : "-");
        model.addAttribute("exporterRole", currentUser != null ? currentUser.getRoleName() : "-");
        model.addAttribute("reportCode", "BC-DT-" + y + "-" + now.format(DateTimeFormatter.ofPattern("MMdd-HHmm")));
        return "admin/statistics/report";
    }

    private static String csv(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    private static String money(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Xuất bảng doanh thu theo tháng dạng CSV (mở được bằng Excel, có BOM UTF-8 để không lỗi tiếng Việt). */
    @GetMapping(value = "/report.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Integer year,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        int y = resolveYear(year);
        List<MonthRow> rows = buildMonthRows(y);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("sep=;\r\n");
        sb.append(csv("BAO CAO DOANH THU NAM " + y)).append("\r\n");
        sb.append(csv("Nguoi xuat")).append(';').append(csv(currentUser != null ? currentUser.getFullName() : "-")).append("\r\n");
        sb.append(csv("Ngay xuat")).append(';')
                .append(csv(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))).append("\r\n\r\n");
        sb.append(csv("Thang")).append(';').append(csv("So don")).append(';').append(csv("Doanh thu (VND)")).append(';')
                .append(csv("So phieu nhap")).append(';').append(csv("Chi phi nhap kho (VND)")).append(';')
                .append(csv("Chenh lech thu - chi nhap (VND)")).append("\r\n");
        long orders = 0, receipts = 0;
        BigDecimal rev = BigDecimal.ZERO, cost = BigDecimal.ZERO;
        for (MonthRow r : rows) {
            sb.append(r.getMonth()).append(';').append(r.getOrders()).append(';').append(money(r.getRevenue())).append(';')
                    .append(r.getReceipts()).append(';').append(money(r.getImportCost())).append(';')
                    .append(money(r.getBalance())).append("\r\n");
            orders += r.getOrders();
            receipts += r.getReceipts();
            rev = rev.add(r.getRevenue());
            cost = cost.add(r.getImportCost());
        }
        sb.append(csv("TONG")).append(';').append(orders).append(';').append(money(rev)).append(';')
                .append(receipts).append(';').append(money(cost)).append(';').append(money(rev.subtract(cost))).append("\r\n");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("BaoCao_DoanhThu_" + y + ".csv", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
