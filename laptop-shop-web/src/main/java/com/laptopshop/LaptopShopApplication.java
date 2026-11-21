package com.laptopshop;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo (tích hợp khởi chạy Web Spring Boot + SocketServer TV2 + @Scheduled)
import com.laptopshop.socket.SocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaptopShopApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(LaptopShopApplication.class, args);
        boolean socketEnabled = Boolean.parseBoolean(
                ctx.getEnvironment().getProperty("laptop.socket.enabled", "true"));
        if (socketEnabled) {
            SocketServer socketServer = ctx.getBean(SocketServer.class);
            Thread socketThread = new Thread(socketServer, "SocketServer-Thread");
            socketThread.setDaemon(true);
            socketThread.start();
            System.out.println("[SOCKET] Socket Server đã khởi động trên cổng "
                    + ctx.getEnvironment().getProperty("laptop.socket.port", "9090"));
        }
    }
}
