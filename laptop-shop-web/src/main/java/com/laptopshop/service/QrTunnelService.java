package com.laptopshop.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QrTunnelService implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(QrTunnelService.class);
    private static final Pattern TRY_CLOUDFLARE_PATTERN =
            Pattern.compile("https://[a-zA-Z0-9-]+\\.trycloudflare\\.com");

    @Value("${laptop.qr.tunnel-enabled:true}")
    private boolean tunnelEnabled;

    @Value("${laptop.qr.base-url:}")
    private String manualBaseUrl;

    private volatile String activeTunnelUrl;
    private volatile Process tunnelProcess;
    private volatile boolean shuttingDown = false;
    private volatile boolean started = false;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (started) return;
        started = true;
        if (!tunnelEnabled || (manualBaseUrl != null && !manualBaseUrl.isBlank())) {
            return;
        }
        int port = event.getWebServer().getPort();
        Thread t = new Thread(() -> runTunnelLoop(port), "QrTunnel-Worker");
        t.setDaemon(true);
        t.start();
    }

    public String getPublicBaseUrl() {
        if (manualBaseUrl != null && !manualBaseUrl.isBlank()) {
            String trimmed = manualBaseUrl.trim();
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
        Process p = this.tunnelProcess;
        if (activeTunnelUrl != null && p != null && p.isAlive()) {
            return activeTunnelUrl;
        }
        return null;
    }

    private void runTunnelLoop(int port) {
        String exePath = resolveOrInstallCloudflared();
        if (exePath == null) {
            log.warn("[QR-TUNNEL] Không tìm thấy cloudflared; hệ thống dùng IP Wi-Fi LAN cho mã QR.");
            return;
        }
        while (!shuttingDown) {
            try {
                log.info("[QR-TUNNEL] Đang khởi tạo tunnel HTTPS toàn cầu cho cổng {}...", port);
                ProcessBuilder pb = new ProcessBuilder(
                        exePath, "tunnel", "--no-autoupdate", "--url", "http://127.0.0.1:" + port
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                this.tunnelProcess = proc;

                String foundUrl = null;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && !shuttingDown) {
                        Matcher m = TRY_CLOUDFLARE_PATTERN.matcher(line);
                        if (m.find() && foundUrl == null) {
                            foundUrl = m.group();
                        }
                        if (foundUrl != null && activeTunnelUrl == null
                                && (line.contains("Registered tunnel connection") || line.contains("connIndex="))) {
                            if (waitUntilHttpsReady(foundUrl)) {
                                this.activeTunnelUrl = foundUrl;
                                log.info("[QR-TUNNEL] Đã sẵn sàng quét QR HTTPS toàn cầu (4G/5G/Wi-Fi): {}", foundUrl);
                            }
                        }
                    }
                }
                proc.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.warn("[QR-TUNNEL] Lỗi kết nối tunnel: {}", ex.getMessage());
            }
            this.activeTunnelUrl = null;
            this.tunnelProcess = null;
            if (!shuttingDown) {
                try { Thread.sleep(4000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private boolean waitUntilHttpsReady(String baseUrl) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        URI probeUri = URI.create(baseUrl + "/robots.txt");
        for (int attempt = 1; attempt <= 10 && !shuttingDown; attempt++) {
            try {
                Thread.sleep(attempt == 1 ? 800L : 1000L);
                HttpRequest req = HttpRequest.newBuilder(probeUri)
                        .timeout(Duration.ofSeconds(4))
                        .GET()
                        .build();
                HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
                if (res.statusCode() >= 200 && res.statusCode() < 400) {
                    return true;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception ignored) {
                // DNS / TLS edge cho trycloudflare.com có thể mất vài giây để lan truyền
            }
        }
        log.warn("[QR-TUNNEL] Tunnel {} chưa sẵn sàng HTTPS kịp thời; tạm dùng địa chỉ dự phòng.", baseUrl);
        return false;
    }

    private String resolveOrInstallCloudflared() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean isWin = os.contains("win");
        boolean isMac = os.contains("mac");
        String fileName = isWin ? "cloudflared.exe" : "cloudflared";

        // 1. Check PATH / common locations
        String onPath = findExecutableOnPath(fileName);
        if (onPath != null) return onPath;
        if (isMac) {
            for (String candidate : new String[]{"/opt/homebrew/bin/cloudflared", "/usr/local/bin/cloudflared"}) {
                if (new File(candidate).canExecute()) return candidate;
            }
        }

        // 2. Check cached binary in ~/.laptopshop/
        Path cacheDir = Path.of(System.getProperty("user.home", "."), ".laptopshop");
        Path cachedExe = cacheDir.resolve(fileName);
        if (Files.isRegularFile(cachedExe) && cachedExe.toFile().length() > 1_000_000L) {
            if (!isWin) cachedExe.toFile().setExecutable(true);
            return cachedExe.toAbsolutePath().toString();
        }

        // 3. Auto-download for Windows / Linux (portable zero-config on any machine)
        String downloadUrl = resolveDownloadUrl(os);
        if (downloadUrl == null) return null;

        try {
            Files.createDirectories(cacheDir);
            log.info("[QR-TUNNEL] Đang tự động tải cloudflared lần đầu để hỗ trợ quét QR đa mạng...");
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(downloadUrl))
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() == 200) {
                Path tmp = cacheDir.resolve(fileName + ".downloading");
                try (InputStream in = res.body()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(tmp, cachedExe, StandardCopyOption.REPLACE_EXISTING);
                if (!isWin) cachedExe.toFile().setExecutable(true);
                log.info("[QR-TUNNEL] Tải cloudflared thành công: {}", cachedExe);
                return cachedExe.toAbsolutePath().toString();
            }
        } catch (Exception ex) {
            log.warn("[QR-TUNNEL] Không thể tải cloudflared ({}); sẽ dùng Wi-Fi LAN.", ex.getMessage());
        }
        return null;
    }

    private static String resolveDownloadUrl(String os) {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe";
        }
        if (os.contains("nux") || os.contains("nix")) {
            String suffix = (arch.contains("aarch64") || arch.contains("arm64")) ? "arm64" : "amd64";
            return "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-" + suffix;
        }
        return null;
    }

    private static String findExecutableOnPath(String cmd) {
        try {
            boolean isWin = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            Process p = new ProcessBuilder(isWin ? "where" : "which", cmd)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = br.readLine();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0 && line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        Process p = this.tunnelProcess;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
    }
}
