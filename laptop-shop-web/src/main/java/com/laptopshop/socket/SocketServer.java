package com.laptopshop.socket;

import com.laptopshop.entity.Brand;
import com.laptopshop.entity.Category;
import com.laptopshop.entity.Laptop;
import com.laptopshop.repository.BrandRepository;
import com.laptopshop.repository.CategoryRepository;
import com.laptopshop.repository.LaptopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SocketServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);

    @Value("${socket.server.port:9090}")
    private int port;

    private final LaptopRepository laptopRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    public SocketServer(LaptopRepository laptopRepo,
                        CategoryRepository categoryRepo,
                        BrandRepository brandRepo) {
        this.laptopRepo = laptopRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
    }

    @Override
    public void run() {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("[SOCKET-SERVER] Đang lắng nghe trên cổng {}", port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientCounter.incrementAndGet();
                    log.info("[SOCKET-SERVER] Client #{} kết nối từ {}", clientId, clientSocket.getRemoteSocketAddress());
                    pool.submit(new ClientHandler(clientSocket, clientId, laptopRepo, categoryRepo, brandRepo));
                } catch (IOException e) {
                    log.warn("[SOCKET-SERVER] Lỗi khi chấp nhận kết nối: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("[SOCKET-SERVER] Không thể khởi động server socket trên cổng {}: {}", port, e.getMessage());
        }
    }

    // ====== Client Handler (mỗi client chạy trên 1 thread riêng) ======
    static class ClientHandler implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
        private final Socket socket;
        private final int clientId;
        private final LaptopRepository laptopRepo;
        private final CategoryRepository categoryRepo;
        private final BrandRepository brandRepo;

        ClientHandler(Socket socket, int clientId,
                      LaptopRepository laptopRepo,
                      CategoryRepository categoryRepo,
                      BrandRepository brandRepo) {
            this.socket = socket;
            this.clientId = clientId;
            this.laptopRepo = laptopRepo;
            this.categoryRepo = categoryRepo;
            this.brandRepo = brandRepo;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            log.info("[SOCKET | Thread: {} | Client #{}] Bắt đầu xử lý", threadName, clientId);
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    String cmd = line.trim();
                    log.info("[SOCKET | Thread: {} | Client #{}] Nhận lệnh: {}", threadName, clientId, cmd);
                    handleCommand(cmd, out);
                    if (cmd.equalsIgnoreCase("QUIT")) break;
                }
            } catch (IOException e) {
                log.warn("[SOCKET | Client #{}] Ngắt kết nối: {}", clientId, e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                log.info("[SOCKET | Client #{}] Đã đóng kết nối.", clientId);
            }
        }

        private void handleCommand(String cmd, PrintWriter out) {
            try {
                if (cmd.equalsIgnoreCase("PING")) {
                    out.println("OK");
                    out.println("PONG - Server đang hoạt động bình thường");
                    out.println("END");
                } else if (cmd.equalsIgnoreCase("LIST_CATEGORIES")) {
                    List<Category> cats = categoryRepo.findByIsActiveTrueOrderByNameAsc();
                    out.println("OK");
                    for (Category c : cats) {
                        out.println(c.getId() + "\t" + c.getName() + "\t" + nvl(c.getDescription()));
                    }
                    out.println("END");
                } else if (cmd.equalsIgnoreCase("LIST_BRANDS")) {
                    List<Brand> brands = brandRepo.findByIsActiveTrueOrderByNameAsc();
                    out.println("OK");
                    for (Brand b : brands) {
                        out.println(b.getId() + "\t" + b.getName() + "\t" + nvl(b.getCountry()) + "\t" + nvl(b.getDescription()));
                    }
                    out.println("END");
                } else if (cmd.equalsIgnoreCase("LIST_LAPTOPS")) {
                    List<Laptop> laptops = laptopRepo.findAllWithDetails();
                    out.println("OK");
                    for (Laptop l : laptops) {
                        out.println(l.getId() + "\t" + l.getName() + "\t" + l.getBrandName()
                            + "\t" + l.getSalePrice() + "\t" + l.getStockQuantity()
                            + "\t" + l.getCategoryName());
                    }
                    out.println("END");
                } else if (cmd.toUpperCase().startsWith("SEARCH|")) {
                    String keyword = cmd.substring(7).trim();
                    List<Laptop> results = laptopRepo.searchByKeyword(keyword);
                    out.println("OK");
                    for (Laptop l : results) {
                        out.println(l.getId() + "\t" + l.getName() + "\t" + l.getBrandName()
                            + "\t" + l.getSalePrice() + "\t" + l.getStockQuantity()
                            + "\t" + l.getCategoryName());
                    }
                    out.println("END");
                } else if (cmd.toUpperCase().startsWith("GET|")) {
                    String idStr = cmd.substring(4).trim();
                    int id = Integer.parseInt(idStr);
                    Optional<Laptop> opt = laptopRepo.findByIdWithDetails(id);
                    if (opt.isEmpty()) {
                        out.println("ERROR");
                        out.println("Không tìm thấy laptop ID: " + id);
                        out.println("END");
                    } else {
                        Laptop l = opt.get();
                        out.println("OK");
                        out.println(l.getId() + "\t" + nvl(l.getSku()) + "\t" + l.getName()
                            + "\t" + l.getBrandName() + "\t" + l.getCategoryName()
                            + "\t" + nvl(l.getCpu()) + "\t" + l.getRam()
                            + "\t" + l.getStorage() + "\t" + nvl(l.getGpu())
                            + "\t" + l.getSalePrice() + "\t" + l.getStockQuantity()
                            + "\t" + nvl(l.getDescription()));
                        out.println("END");
                    }
                } else if (cmd.equalsIgnoreCase("QUIT")) {
                    out.println("OK");
                    out.println("Tạm biệt! Kết nối đã đóng.");
                    out.println("END");
                } else {
                    out.println("ERROR");
                    out.println("Lệnh không hợp lệ: " + cmd);
                    out.println("END");
                }
            } catch (NumberFormatException e) {
                out.println("ERROR");
                out.println("ID không hợp lệ: " + e.getMessage());
                out.println("END");
            } catch (Exception e) {
                log.error("[SOCKET | Client #{}] Lỗi xử lý lệnh '{}': {}", clientId, cmd, e.getMessage());
                out.println("ERROR");
                out.println("Lỗi server: " + e.getMessage());
                out.println("END");
            }
        }

        private String nvl(String s) { return s == null ? "" : s; }
    }
}
