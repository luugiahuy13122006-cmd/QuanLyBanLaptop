package com.laptopshop.client;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class MultiClientDemo {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("==========================================================================");
        System.out.println("   DEMO ĐA LUỒNG: KHỞI CHẠY ĐỒNG THỜI 3 CLIENT KẾT NỐI TỚI SOCKET SERVER");
        System.out.println("   Mục tiêu: Minh chứng Server phục vụ >= 3 client cùng một thời điểm");
        System.out.println("==========================================================================\n");

        int clientCount = 3;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(clientCount);

        // Client 1: Gửi lệnh PING và LIST_CATEGORIES
        Thread client1 = new Thread(() -> {
            try {
                startSignal.await(); // Đợi hiệu lệnh xuất phát cùng lúc
                runSingleClient("Client-1-KiemTraDanhMuc", new String[]{"PING", "LIST_CATEGORIES", "LIST_BRANDS", "QUIT"});
            } catch (Exception e) {
                System.err.println("[Client-1] Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        }, "Luong-Client-1");

        // Client 2: Gửi lệnh SEARCH|Dell
        Thread client2 = new Thread(() -> {
            try {
                startSignal.await();
                runSingleClient("Client-2-TimKiemDell", new String[]{"SEARCH|Dell", "QUIT"});
            } catch (Exception e) {
                System.err.println("[Client-2] Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        }, "Luong-Client-2");

        // Client 3: Gửi lệnh GET|1 (Chi tiết laptop ID = 1)
        Thread client3 = new Thread(() -> {
            try {
                startSignal.await();
                runSingleClient("Client-3-XemChiTietLap1", new String[]{"GET|1", "QUIT"});
            } catch (Exception e) {
                System.err.println("[Client-3] Lỗi: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        }, "Luong-Client-3");

        client1.start();
        client2.start();
        client3.start();

        System.out.println(">>> Đang phát tín hiệu xuất phát cho cả 3 client đồng thời...\n");
        startSignal.countDown();

        doneSignal.await();
        System.out.println("\n==========================================================================");
        System.out.println(">>> CẢ 3 CLIENT ĐÃ HOÀN TẤT GIAO TIẾP VỚI MÁY CHỦ THÀNH CÔNG!");
        System.out.println("==========================================================================");
    }

    private static void runSingleClient(String clientLabel, String[] commands) {
        String currentThread = Thread.currentThread().getName();
        System.out.printf("[%s | %s] Bắt đầu kết nối tới server %s:%d...%n", currentThread, clientLabel, HOST, PORT);

        try (
            Socket socket = new Socket(HOST, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)
        ) {
            socket.setSoTimeout(10000);
            System.out.printf("[%s | %s] Kết nối thành công! Bắt đầu gửi chuỗi %d lệnh.%n",
                    currentThread, clientLabel, commands.length);

            for (String cmd : commands) {
                System.out.printf("[%s | %s] >> GỬI LỆNH: %s%n", currentThread, clientLabel, cmd);
                out.println(cmd);

                String status = in.readLine();
                System.out.printf("[%s | %s] << TRẠNG THÁI TỪ SERVER: %s%n", currentThread, clientLabel, status);

                String line;
                int count = 0;
                while ((line = in.readLine()) != null) {
                    if ("END".equals(line)) {
                        break;
                    }
                    count++;
                    if (count <= 3) {
                        System.out.printf("[%s | %s]    Dòng %d: %s%n", currentThread, clientLabel, count, line);
                    }
                }
                if (count > 3) {
                    System.out.printf("[%s | %s]    ... (và %d bản ghi khác)%n", currentThread, clientLabel, (count - 3));
                }
            }

        } catch (Exception e) {
            System.err.printf("[%s | %s] Xảy ra lỗi ngoại lệ: %s%n", currentThread, clientLabel, e.getMessage());
        }
    }
}
