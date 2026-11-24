package com.laptopshop.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Giao diện desktop Swing giám sát tồn kho theo thời gian thực qua TCP Socket (auto-reconnect).
 */
public class StockMonitorFrame extends JFrame {

    private final String host;
    private final int port;
    private final String token;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public StockMonitorFrame(String host, int port, String token) {
        super("NEXORA Stock Monitor (TCP Socket)");
        this.host = host;
        this.port = port;
        this.token = token;
        setSize(820, 500);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        statusLabel = new JLabel("Đang khởi tạo...");
        tableModel = new DefaultTableModel(new String[]{"ID", "SKU", "Tên Laptop", "Hãng", "Giá bán", "Tồn kho", "Danh mục"}, 0);
        JTable table = new JTable(tableModel);

        JButton refreshBtn = new JButton("Làm mới tồn kho");
        refreshBtn.addActionListener(e -> loadLaptopsAsync());

        JButton lowStockBtn = new JButton("Chỉ hiện tồn thấp (<=5)");
        lowStockBtn.addActionListener(e -> loadLowStockAsync());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refreshBtn);
        top.add(lowStockBtn);
        top.add(statusLabel);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        loadLaptopsAsync();
    }

    private void loadLaptopsAsync() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return executeSocketCommand("LIST_LAPTOPS");
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    tableModel.setRowCount(0);
                    for (int i = 1; i < lines.size(); i++) {
                        tableModel.addRow(lines.get(i).split("\t", -1));
                    }
                    statusLabel.setText("Đã đồng bộ " + Math.max(0, lines.size() - 1) + " sản phẩm");
                } catch (Exception ex) {
                    statusLabel.setText("Mất kết nối (" + ex.getMessage() + ")");
                }
            }
        };
        worker.execute();
    }

    private void loadLowStockAsync() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return executeSocketCommand("LOW_STOCK");
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    tableModel.setRowCount(0);
                    for (int i = 1; i < lines.size(); i++) {
                        String[] p = lines.get(i).split("\t", -1);
                        tableModel.addRow(new Object[]{"-", p.length > 0 ? p[0] : "", p.length > 1 ? p[1] : "",
                                "-", "-", p.length > 2 ? p[2] : "0", "LOW_STOCK"});
                    }
                    statusLabel.setText("Cảnh báo tồn thấp: " + Math.max(0, lines.size() - 1) + " mã");
                } catch (Exception ex) {
                    statusLabel.setText("Lỗi tải tồn thấp: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private List<String> executeSocketCommand(String command) throws IOException {
        try (Socket socket = LaptopClientApp.connectWithRetry(host, port, 3);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)) {
            LaptopClientApp.sendCommand(out, in, "AUTH|" + token);
            List<String> res = LaptopClientApp.sendCommand(out, in, command);
            LaptopClientApp.sendCommand(out, in, "QUIT");
            return res;
        }
    }
}
