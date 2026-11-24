package com.laptopshop.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

public class MultiClientDemo {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9090;
    private static final String TOKEN = System.getenv().getOrDefault(
            "SOCKET_AUTH_TOKEN", "NEXORA_SOCKET_DEV_TOKEN_2026");

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DEMO 5 SOCKET CLIENT ĐỒNG THỜI (NEXORA TCP SERVER) ===");

        String[] commands = {
                "LIST_CATEGORIES",
                "LIST_LAPTOPS",
                "SEARCH|asus",
                "CHECK_STOCK|LT001",
                "LOW_STOCK"
        };

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch readyLatch = new CountDownLatch(5);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int id = i + 1;
            final String cmd = commands[i];
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startGate.await();
                    try (Socket socket = LaptopClientApp.connectWithRetry(HOST, PORT, 3);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)) {

                        LaptopClientApp.sendCommand(out, in, "AUTH|" + TOKEN);
                        List<String> lines = LaptopClientApp.sendCommand(out, in, cmd);
                        System.out.printf("[CLIENT-%d] CMD=%s => %d dòng phản hồi%n", id, cmd, lines.size());
                        LaptopClientApp.sendCommand(out, in, "QUIT");
                    }
                } catch (Exception e) {
                    System.err.printf("[CLIENT-%d] LỖI: %s%n", id, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startGate.countDown();
        doneLatch.await(15, TimeUnit.SECONDS);
        pool.shutdown();
        System.out.println("=== HOÀN TẤT DEMO ĐA CLIENT ===");
    }
}
