package com.laptopshop.controller;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
import com.laptopshop.entity.Laptop;
import com.laptopshop.service.LaptopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/admin/stock")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);
    private final LaptopService laptopService;

    public StockController(LaptopService laptopService) {
        this.laptopService = laptopService;
    }

    @GetMapping("/test")
    public String testPage(Model model) {
        model.addAttribute("pageTitle", "Kiểm tra Đa luồng - Tồn kho");
        model.addAttribute("laptops", laptopService.getAllLaptops());
        return "admin/stock/test";
    }

    @PostMapping("/concurrent-test")
    public String runConcurrentTest(@RequestParam Integer laptopId,
                                    @RequestParam int threadCount,
                                    @RequestParam int deltaPerThread,
                                    RedirectAttributes ra) {
        int safeThreads = Math.max(2, Math.min(threadCount, 30));
        Laptop before = laptopService.getLaptopById(laptopId);
        int stockBefore = before.getStockQuantity() != null ? before.getStockQuantity() : 0;

        List<String> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(safeThreads);
        CountDownLatch readyLatch = new CountDownLatch(safeThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        log.info("[CONCURRENT-TEST] Bắt đầu {} luồng cập nhật tồn kho laptop ID={} (tồn trước={})",
                safeThreads, laptopId, stockBefore);

        for (int i = 1; i <= safeThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    laptopService.updateStockSafe(laptopId, deltaPerThread);
                    results.add("[Luồng-" + threadNum + " | " + Thread.currentThread().getName()
                            + "] Thành công (delta=" + (deltaPerThread > 0 ? "+" + deltaPerThread : deltaPerThread) + ")");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    results.add("[Luồng-" + threadNum + " | " + Thread.currentThread().getName()
                            + "] Bị chặn an toàn: " + e.getMessage());
                    failCount.incrementAndGet();
                }
            });
        }

        try {
            readyLatch.await(10, TimeUnit.SECONDS);
            startLatch.countDown();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        Laptop after = laptopService.getLaptopById(laptopId);
        int stockAfter = after.getStockQuantity() != null ? after.getStockQuantity() : 0;
        int expectedAfter = stockBefore + successCount.get() * deltaPerThread;

        ra.addFlashAttribute("testResults", results);
        ra.addFlashAttribute("successCount", successCount.get());
        ra.addFlashAttribute("failCount", failCount.get());
        ra.addFlashAttribute("threadCount", safeThreads);
        ra.addFlashAttribute("deltaPerThread", deltaPerThread);
        ra.addFlashAttribute("stockBefore", stockBefore);
        ra.addFlashAttribute("stockAfter", stockAfter);
        ra.addFlashAttribute("expectedAfter", expectedAfter);
        ra.addFlashAttribute("testedLaptopName", after.getSku() + " - " + after.getName());
        ra.addFlashAttribute("laptopId", laptopId);
        ra.addFlashAttribute("successMessage",
                "Hoàn tất kiểm tra " + safeThreads + " luồng đồng thời! Tồn kho: " + stockBefore + " → " + stockAfter);
        return "redirect:/admin/stock/test";
    }
}
