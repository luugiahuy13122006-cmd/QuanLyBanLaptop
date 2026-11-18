package com.laptopshop.controller;
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
    public StockController(LaptopService s) { this.laptopService = s; }

    @GetMapping("/test")
    public String testPage(Model model) {
        model.addAttribute("pageTitle", "Kiểm thử đa luồng tồn kho");
        return "stock/test";
    }

    @PostMapping("/concurrent-test")
    public String runConcurrentTest(@RequestParam Integer laptopId,
                                    @RequestParam int threadCount,
                                    @RequestParam int deltaPerThread,
                                    RedirectAttributes ra) {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        log.info("[CONCURRENT-TEST] Bắt đầu {} luồng cập nhật tồn kho laptop ID={}", threadCount, laptopId);

        for (int i = 1; i <= threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    laptopService.updateStockSafe(laptopId, deltaPerThread);
                    String msg = "[Luồng-" + threadNum + " | " + Thread.currentThread().getName() + "] Thành công";
                    results.add(msg);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    String msg = "[Luồng-" + threadNum + "] Thất bại: " + e.getMessage();
                    results.add(msg);
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try { executor.awaitTermination(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        ra.addFlashAttribute("testResults", results);
        ra.addFlashAttribute("successCount", successCount.get());
        ra.addFlashAttribute("failCount", failCount.get());
        ra.addFlashAttribute("laptopId", laptopId);
        return "redirect:/admin/stock/test";
    }
}
