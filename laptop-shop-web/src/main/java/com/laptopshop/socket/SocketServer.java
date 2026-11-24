package com.laptopshop.socket;

import com.laptopshop.entity.*;
import com.laptopshop.repository.BrandRepository;
import com.laptopshop.repository.CategoryRepository;
import com.laptopshop.repository.LaptopRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.service.AuditLogService;
import com.laptopshop.service.LaptopService;
import com.laptopshop.util.VietnameseTextNormalizer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SocketServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SocketServer.class);
    private static final int SOCKET_TIMEOUT_MS = 30_000;
    private static final int MAX_LINE_LENGTH = 4096;

    private int port;
    private final String authToken;
    private final LaptopRepository laptopRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final OrderRepository orderRepo;
    private final LaptopService laptopService;
    private final AuditLogService auditLogService;

    private final AtomicInteger clientCounter = new AtomicInteger(0);
    private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            8, 32, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r);
                t.setName("socket-worker-" + t.getId());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );
    private volatile ServerSocket serverSocket;
    private volatile boolean running = true;

    public SocketServer(LaptopRepository laptopRepo,
                        CategoryRepository categoryRepo,
                        BrandRepository brandRepo) {
        this(9090, "NEXORA_SOCKET_DEV_TOKEN_2026", laptopRepo, categoryRepo, brandRepo, null, null, null);
    }

    @Autowired
    public SocketServer(
            @Value("${laptop.socket.port:9090}") int port,
            @Value("${laptop.socket.auth-token:NEXORA_SOCKET_DEV_TOKEN_2026}") String authToken,
            LaptopRepository laptopRepo,
            CategoryRepository categoryRepo,
            BrandRepository brandRepo,
            OrderRepository orderRepo,
            LaptopService laptopService,
            AuditLogService auditLogService) {
        this.port = port;
        this.authToken = authToken != null ? authToken.trim() : "";
        this.laptopRepo = laptopRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.orderRepo = orderRepo;
        this.laptopService = laptopService;
        this.auditLogService = auditLogService;
    }

    public int getPort() {
        return serverSocket != null && serverSocket.isBound() ? serverSocket.getLocalPort() : port;
    }

    @Override
    public void run() {
        running = true;
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            log.info("[SOCKET-SERVER] Đang lắng nghe trên cổng {} (pool 8..32)", ss.getLocalPort());
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = ss.accept();
                    clientSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                    activeSockets.add(clientSocket);
                    int clientId = clientCounter.incrementAndGet();
                    try {
                        pool.execute(new ClientHandler(
                                clientSocket, clientId, authToken, activeSockets,
                                laptopRepo, categoryRepo, brandRepo, orderRepo, laptopService, auditLogService));
                    } catch (RejectedExecutionException rej) {
                        activeSockets.remove(clientSocket);
                        closeQuietly(clientSocket);
                        log.warn("[SOCKET-SERVER] Hệ thống quá tải, từ chối client #{}", clientId);
                    }
                } catch (IOException e) {
                    if (running) {
                        log.warn("[SOCKET-SERVER] Lỗi khi chấp nhận kết nối: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("[SOCKET-SERVER] Không thể khởi động server socket trên cổng {}: {}", port, e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        closeQuietly(serverSocket);
        for (Socket s : activeSockets) {
            closeQuietly(s);
        }
        activeSockets.clear();
        pool.shutdownNow();
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try { c.close(); } catch (IOException ignored) {}
        }
    }

    static class ClientHandler implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
        private final Socket socket;
        private final int clientId;
        private final String expectedToken;
        private final Set<Socket> activeSockets;
        private final LaptopRepository laptopRepo;
        private final CategoryRepository categoryRepo;
        private final BrandRepository brandRepo;
        private final OrderRepository orderRepo;
        private final LaptopService laptopService;
        private final AuditLogService auditLogService;
        private boolean authenticated = false;

        ClientHandler(Socket socket, int clientId, String expectedToken, Set<Socket> activeSockets,
                      LaptopRepository laptopRepo, CategoryRepository categoryRepo,
                      BrandRepository brandRepo, OrderRepository orderRepo,
                      LaptopService laptopService, AuditLogService auditLogService) {
            this.socket = socket;
            this.clientId = clientId;
            this.expectedToken = expectedToken;
            this.activeSockets = activeSockets;
            this.laptopRepo = laptopRepo;
            this.categoryRepo = categoryRepo;
            this.brandRepo = brandRepo;
            this.orderRepo = orderRepo;
            this.laptopService = laptopService;
            this.auditLogService = auditLogService;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            log.info("[SOCKET | Thread: {} | Client #{}] Kết nối từ {}", threadName, clientId, socket.getRemoteSocketAddress());
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)
            ) {
                String line;
                while ((line = readLineBounded(in)) != null) {
                    String cmd = line.trim();
                    if (cmd.isEmpty()) continue;
                    handleCommand(cmd, out);
                    if (cmd.equalsIgnoreCase("QUIT")) break;
                }
            } catch (SocketTimeoutException ste) {
                log.info("[SOCKET | Client #{}] Hết thời gian chờ ({}ms).", clientId, SOCKET_TIMEOUT_MS);
            } catch (IOException e) {
                log.warn("[SOCKET | Client #{}] Ngắt kết nối: {}", clientId, e.getMessage());
            } finally {
                activeSockets.remove(socket);
                closeQuietly(socket);
                log.info("[SOCKET | Client #{}] Đã đóng kết nối.", clientId);
            }
        }

        private String readLineBounded(BufferedReader in) throws IOException {
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = in.read()) != -1) {
                if (ch == '\n') {
                    return sb.toString();
                }
                if (ch != '\r') {
                    if (sb.length() >= MAX_LINE_LENGTH) {
                        throw new IOException("Độ dài lệnh vượt quá " + MAX_LINE_LENGTH + " ký tự.");
                    }
                    sb.append((char) ch);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }

        private void handleCommand(String cmd, PrintWriter out) {
            try {
                if (cmd.equalsIgnoreCase("PING")) {
                    out.println("OK");
                    out.println("PONG - NEXORA Socket Server v2 (AUTH enabled)");
                    out.println("END");
                    return;
                }
                if (cmd.equalsIgnoreCase("QUIT")) {
                    out.println("OK");
                    out.println("Tạm biệt! Kết nối đã đóng.");
                    out.println("END");
                    return;
                }
                if (cmd.toUpperCase().startsWith("AUTH|")) {
                    String supplied = cmd.substring(5).trim();
                    if (!expectedToken.isEmpty() && MessageDigest.isEqual(
                            expectedToken.getBytes(StandardCharsets.UTF_8),
                            supplied.getBytes(StandardCharsets.UTF_8))) {
                        authenticated = true;
                        out.println("OK");
                        out.println("AUTHENTICATED");
                        out.println("END");
                    } else {
                        out.println("ERROR");
                        out.println("UNAUTHORIZED - Sai token xác thực Socket.");
                        out.println("END");
                    }
                    return;
                }

                if (!authenticated) {
                    out.println("ERROR");
                    out.println("UNAUTHORIZED - Vui lòng gửi AUTH|<token> trước khi thực hiện lệnh.");
                    out.println("END");
                    return;
                }

                if (cmd.equalsIgnoreCase("LIST_CATEGORIES")) {
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
                    List<Laptop> laptops = laptopRepo.findAllActiveWithDetails();
                    out.println("OK");
                    for (Laptop l : laptops) {
                        out.println(l.getId() + "\t" + nvl(l.getSku()) + "\t" + l.getName() + "\t" + l.getBrandName()
                                + "\t" + l.getSalePrice() + "\t" + l.getStockQuantity() + "\t" + l.getCategoryName());
                    }
                    out.println("END");
                } else if (cmd.toUpperCase().startsWith("SEARCH|")) {
                    String keyword = cmd.substring(7).trim();
                    List<Laptop> results = laptopRepo.searchByKeyword(keyword, VietnameseTextNormalizer.normalize(keyword))
                            .stream().filter(l -> Boolean.TRUE.equals(l.getIsActive())).toList();
                    out.println("OK");
                    for (Laptop l : results) {
                        out.println(l.getId() + "\t" + nvl(l.getSku()) + "\t" + l.getName() + "\t" + l.getBrandName()
                                + "\t" + l.getSalePrice() + "\t" + l.getStockQuantity() + "\t" + l.getCategoryName());
                    }
                    out.println("END");
                } else if (cmd.toUpperCase().startsWith("GET|")) {
                    int id = Integer.parseInt(cmd.substring(4).trim());
                    Optional<Laptop> opt = laptopRepo.findByIdWithDetails(id)
                            .filter(l -> Boolean.TRUE.equals(l.getIsActive()));
                    if (opt.isEmpty()) {
                        out.println("ERROR");
                        out.println("Không tìm thấy laptop đang bán ID: " + id);
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
                } else if (cmd.toUpperCase().startsWith("CHECK_STOCK|")) {
                    String sku = cmd.substring(12).trim();
                    Optional<Laptop> opt = laptopRepo.findBySkuIgnoreCase(sku);
                    if (opt.isEmpty()) {
                        out.println("ERROR");
                        out.println("Không tìm thấy SKU: " + sku);
                        out.println("END");
                    } else {
                        Laptop l = opt.get();
                        out.println("OK");
                        out.println(l.getSku() + "\t" + l.getName() + "\t" + l.getStockQuantity()
                                + "\t" + (Boolean.TRUE.equals(l.getIsActive()) ? "ACTIVE" : "INACTIVE"));
                        out.println("END");
                    }
                } else if (cmd.toUpperCase().startsWith("ORDER_STATUS|")) {
                    if (orderRepo == null) {
                        out.println("ERROR");
                        out.println("OrderRepository chưa được cấu hình.");
                        out.println("END");
                        return;
                    }
                    String code = cmd.substring(13).trim();
                    Optional<Order> opt = orderRepo.findByOrderCodeIgnoreCase(code);
                    if (opt.isEmpty()) {
                        out.println("ERROR");
                        out.println("Không tìm thấy đơn hàng: " + code);
                        out.println("END");
                    } else {
                        Order o = opt.get();
                        out.println("OK");
                        out.println(o.getOrderCode() + "\t" + o.getStatus().name()
                                + "\t" + o.getStatus().getDisplayName()
                                + "\t" + o.getTotalAmount());
                        out.println("END");
                    }
                } else if (cmd.equalsIgnoreCase("LOW_STOCK")) {
                    List<Laptop> low = laptopRepo.findLowStock(5);
                    out.println("OK");
                    for (Laptop l : low) {
                        out.println(l.getSku() + "\t" + l.getName() + "\t" + l.getStockQuantity());
                    }
                    out.println("END");
                } else if (cmd.toUpperCase().startsWith("STOCK_ADJUST|")) {
                    if (laptopService == null) {
                        out.println("ERROR");
                        out.println("LaptopService chưa được cấu hình.");
                        out.println("END");
                        return;
                    }
                    String[] parts = cmd.split("\\|", 4);
                    if (parts.length < 4) {
                        out.println("ERROR");
                        out.println("Cú pháp đúng: STOCK_ADJUST|<sku>|<delta>|<reason>");
                        out.println("END");
                        return;
                    }
                    String sku = parts[1].trim();
                    int delta = Integer.parseInt(parts[2].trim());
                    String reason = parts[3].trim();
                    Laptop laptop = laptopRepo.findBySkuIgnoreCase(sku).orElse(null);
                    if (laptop == null) {
                        out.println("ERROR");
                        out.println("Không tìm thấy SKU: " + sku);
                        out.println("END");
                        return;
                    }
                    laptopService.updateStockSafe(laptop.getId(), delta);
                    Laptop refreshed = laptopRepo.findById(laptop.getId()).orElse(laptop);
                    if (auditLogService != null) {
                        auditLogService.logSecurityEvent(null, "SOCKET_CLIENT_" + clientId,
                                "SOCKET_STOCK_ADJUST", "Laptop", sku,
                                "Delta=" + delta + ", NewStock=" + refreshed.getStockQuantity() + ", Reason=" + reason);
                    }
                    out.println("OK");
                    out.println(refreshed.getSku() + "\t" + refreshed.getStockQuantity());
                    out.println("END");
                } else {
                    out.println("ERROR");
                    out.println("Lệnh không hợp lệ: " + cmd);
                    out.println("END");
                }
            } catch (NumberFormatException e) {
                out.println("ERROR");
                out.println("Tham số số không hợp lệ: " + e.getMessage());
                out.println("END");
            } catch (Exception e) {
                log.error("[SOCKET | Client #{}] Lỗi xử lý lệnh '{}': {}", clientId, cmd, e.getMessage());
                out.println("ERROR");
                out.println("Lỗi server: " + e.getMessage());
                out.println("END");
            }
        }

        private String nvl(String s) {
            return s == null ? "" : s;
        }
    }
}
