package com.laptopshop.client;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LaptopClientApp {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 9090;
    private static final String DEFAULT_TOKEN = System.getenv().getOrDefault(
            "SOCKET_AUTH_TOKEN", "NEXORA_SOCKET_DEV_TOKEN_2026");

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        String token = args.length > 2 ? args[2] : DEFAULT_TOKEN;

        if (args.length > 0 && "--gui".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(() -> new StockMonitorFrame(DEFAULT_HOST, DEFAULT_PORT, token).setVisible(true));
            return;
        }

        System.out.println("=== NEXORA LAPTOP SOCKET CLIENT ===");
        System.out.printf("Kết nối tới %s:%d ...%n", host, port);

        try (Socket socket = connectWithRetry(host, port, 3);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
             Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {

            System.out.println("Kết nối thành công! Đang xác thực AUTH...");
            List<String> authResp = sendCommand(out, in, "AUTH|" + token);
            authResp.forEach(System.out::println);

            printHelp();
            while (true) {
                System.out.print("\nLệnh> ");
                if (!scanner.hasNextLine()) break;
                String cmd = scanner.nextLine().trim();
                if (cmd.isEmpty()) continue;
                if ("GUI".equalsIgnoreCase(cmd)) {
                    SwingUtilities.invokeLater(() -> new StockMonitorFrame(host, port, token).setVisible(true));
                    continue;
                }
                if ("HELP".equalsIgnoreCase(cmd)) {
                    printHelp();
                    continue;
                }

                List<String> lines = sendCommand(out, in, cmd);
                for (String line : lines) {
                    System.out.println("  " + line);
                }
                if ("QUIT".equalsIgnoreCase(cmd)) break;
            }
        } catch (IOException e) {
            System.err.println("Không thể kết nối Socket Server (" + host + ":" + port + "): " + e.getMessage());
        }
    }

    public static Socket connectWithRetry(String host, int port, int maxRetries) throws IOException {
        IOException last = null;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                Socket s = new Socket(host, port);
                s.setSoTimeout(15_000);
                return s;
            } catch (IOException ex) {
                last = ex;
                try { Thread.sleep(500L * i); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last != null ? last : new IOException("Kết nối thất bại");
    }

    public static List<String> sendCommand(PrintWriter out, BufferedReader in, String cmd) throws IOException {
        out.println(cmd);
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if ("END".equals(line)) break;
            lines.add(line);
        }
        return lines;
    }

    private static void printHelp() {
        System.out.println("Lệnh hỗ trợ:");
        System.out.println("  PING | LIST_CATEGORIES | LIST_BRANDS | LIST_LAPTOPS | LOW_STOCK");
        System.out.println("  SEARCH|<tu_khoa> | GET|<id> | CHECK_STOCK|<sku> | ORDER_STATUS|<ma_don>");
        System.out.println("  STOCK_ADJUST|<sku>|<delta>|<ly_do> | GUI | QUIT");
    }
}
