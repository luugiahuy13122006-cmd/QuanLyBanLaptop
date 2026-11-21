package com.laptopshop.client;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu
// Chương trình Client TCP Socket tách biệt với ứng dụng Web, giao tiếp qua cổng 9090.

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LaptopClientApp {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;
    private static final int TIMEOUT_MS = 10000; // 10 giây timeout

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        System.out.println("===============================================================");
        System.out.println("   CHƯƠNG TRÌNH LAPTOP CLIENT TÁCH BIỆT (THÀNH VIÊN 2)");
        System.out.println("   Kết nối tới Server: " + host + ":" + port);
        System.out.println("===============================================================");

        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)
        ) {
            socket.setSoTimeout(TIMEOUT_MS);
            System.out.println("[THÔNG BÁO] Kết nối máy chủ thành công!");

            boolean running = true;
            while (running) {
                printMenu();
                System.out.print("Vui lòng chọn chức năng (0-6): ");
                if (!scanner.hasNextLine()) break;
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        executeCommand(out, in, "PING", "PING");
                        break;
                    case "2":
                        executeCommand(out, in, "LIST_CATEGORIES", "DANH MỤC");
                        break;
                    case "3":
                        executeCommand(out, in, "LIST_LAPTOPS", "DANH SÁCH LAPTOP");
                        break;
                    case "4":
                        System.out.print("Nhập từ khóa cần tìm (tên hoặc hãng): ");
                        String keyword = scanner.nextLine().trim();
                        if (keyword.isEmpty()) {
                            System.out.println("[CẢNH BÁO] Từ khóa không được rỗng!");
                        } else {
                            executeCommand(out, in, "SEARCH|" + keyword, "KẾT QUẢ TÌM KIẾM CHO '" + keyword + "'");
                        }
                        break;
                    case "5":
                        System.out.print("Nhập ID của laptop cần xem chi tiết: ");
                        String id = scanner.nextLine().trim();
                        if (id.isEmpty()) {
                            System.out.println("[CẢNH BÁO] ID không được để trống!");
                        } else {
                            executeCommand(out, in, "GET|" + id, "CHI TIẾT LAPTOP ID " + id);
                        }
                        break;
                    case "6":
                        executeCommand(out, in, "LIST_BRANDS", "DANH SÁCH THƯƠNG HIỆU");
                        break;
                    case "0":
                        executeCommand(out, in, "QUIT", "THOÁT");
                        running = false;
                        System.out.println("Đã ngắt kết nối an toàn. Hẹn gặp lại!");
                        break;
                    default:
                        System.out.println("[LỖI] Lựa chọn không hợp lệ, vui lòng chọn từ 0 đến 6.");
                        break;
                }
                System.out.println();
            }

        } catch (ConnectException ce) {
            System.err.println("[LỖI KẾT NỐI] Không thể kết nối tới Server tại " + host + ":" + port);
            System.err.println(">> Hãy đảm bảo ứng dụng Web 'laptop-shop-web' đã được khởi chạy và cổng " + port + " đang mở.");
        } catch (SocketTimeoutException ste) {
            System.err.println("[LỖI TIMEOUT] Quá thời gian chờ phản hồi từ Server (10 giây). Vui lòng thử lại sau.");
        } catch (IOException e) {
            System.err.println("[LỖI MẠNG] Xảy ra sự cố I/O trong quá trình truyền nhận: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("------------------------- MENU LỆNH -------------------------");
        System.out.println(" 1. Kiểm tra kết nối máy chủ (PING)");
        System.out.println(" 2. Xem toàn bộ danh mục sản phẩm (LIST_CATEGORIES)");
        System.out.println(" 3. Xem danh sách toàn bộ laptop (LIST_LAPTOPS)");
        System.out.println(" 4. Tìm kiếm laptop theo từ khóa (SEARCH|<từ khóa>)");
        System.out.println(" 5. Xem thông tin chi tiết một laptop (GET|<id>)");
        System.out.println(" 6. Xem danh sách thương hiệu (LIST_BRANDS)");
        System.out.println(" 0. Đóng kết nối và thoát (QUIT)");
        System.out.println("-------------------------------------------------------------");
    }

    private static void executeCommand(PrintWriter out, BufferedReader in, String command, String title) throws IOException {
        out.println(command);

        String statusLine = in.readLine();
        if (statusLine == null) {
            System.out.println("[LỖI] Máy chủ đã ngắt kết nối bất ngờ.");
            return;
        }

        List<String> records = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if ("END".equals(line)) {
                break;
            }
            records.add(line);
        }

        if (statusLine.startsWith("ERROR")) {
            System.out.println("\n[MÁY CHỦ BÁO LỖI]: " + statusLine);
            for (String r : records) {
                System.out.println("  " + r);
            }
            return;
        }

        System.out.println("\n========== " + title + " ==========");
        if (command.equalsIgnoreCase("PING") || command.equalsIgnoreCase("QUIT")) {
            for (String r : records) {
                System.out.println("  Phản hồi: " + r);
            }
        } else if (command.equals("LIST_CATEGORIES")) {
            printCategoryTable(records);
        } else if (command.startsWith("LIST_LAPTOPS") || command.startsWith("SEARCH")) {
            printLaptopBriefTable(records);
        } else if (command.equals("LIST_BRANDS")) {
            printBrandTable(records);
        } else if (command.startsWith("GET")) {
            printLaptopDetail(records);
        }
    }

    private static void printBrandTable(List<String> records) {
        if (records.isEmpty()) { System.out.println("Không có thương hiệu nào."); return; }
        System.out.printf("%-6s | %-24s | %-20s | %-40s%n", "ID", "THƯƠNG HIỆU", "QUỐC GIA", "MÔ TẢ");
        System.out.println("------------------------------------------------------------------------------------------------");
        for (String record : records) {
            String[] p = record.split("\\t", -1);
            System.out.printf("%-6s | %-24s | %-20s | %-40s%n",
                    p.length > 0 ? p[0] : "",
                    p.length > 1 ? p[1] : "",
                    p.length > 2 ? p[2] : "",
                    p.length > 3 ? p[3] : "");
        }
    }

    private static void printCategoryTable(List<String> records) {
        if (records.isEmpty()) {
            System.out.println("Không có danh mục nào.");
            return;
        }
        System.out.printf("%-6s | %-30s | %-50s%n", "ID", "TÊN DANH MỤC", "MÔ TẢ");
        System.out.println("---------------------------------------------------------------------------------------------");
        for (String record : records) {
            String[] parts = record.split("\t", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : "";
            String desc = parts.length > 2 ? parts[2] : "";
            System.out.printf("%-6s | %-30s | %-50s%n", id, name, desc);
        }
    }

    private static void printLaptopBriefTable(List<String> records) {
        if (records.isEmpty()) {
            System.out.println("Không tìm thấy laptop nào.");
            return;
        }
        System.out.printf("%-5s | %-32s | %-10s | %-16s | %-6s | %-24s%n",
                "ID", "TÊN SẢN PHẨM", "HÃNG", "ĐƠN GIÁ (VNĐ)", "KHO", "DANH MỤC");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (String record : records) {
            String[] parts = record.split("\t", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : "";
            String brand = parts.length > 2 ? parts[2] : "";
            String price = parts.length > 3 ? parts[3] : "";
            String stock = parts.length > 4 ? parts[4] : "";
            String cat = parts.length > 5 ? parts[5] : "";
            System.out.printf("%-5s | %-32s | %-10s | %-16s | %-6s | %-24s%n", id, name, brand, price, stock, cat);
        }
    }

    private static void printLaptopDetail(List<String> records) {
        if (records.isEmpty()) { System.out.println("Không có thông tin chi tiết."); return; }
        String[] p = records.get(0).split("\\t", -1);
        String[] labels = {"ID", "SKU", "Tên laptop", "Thương hiệu", "Danh mục", "CPU", "RAM", "Lưu trữ", "GPU", "Giá bán", "Tồn kho", "Mô tả"};
        for (int i = 0; i < labels.length; i++) {
            System.out.println(labels[i] + " : " + (i < p.length ? p[i] : ""));
        }
    }
}
