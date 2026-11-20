package com.example.laptopshop.security;

import com.example.laptopshop.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** Tự tạo 2 tài khoản mẫu khi chạy ứng dụng lần đầu. */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserService userService;

    public DataInitializer(UserService userService) { this.userService = userService; }

    @Override
    public void run(String... args) {
        try {
            userService.createIfMissing("admin", "Admin@123", "Quản trị viên", "admin@laptopshop.local", UserService.ROLE_ADMIN);
            userService.createIfMissing("user", "User@123", "Khách hàng mẫu", "user@laptopshop.local", UserService.ROLE_USER);
            log.info("Tài khoản mẫu: admin / Admin@123  và  user / User@123");
        } catch (DataAccessException e) {
            log.error("Không tạo được tài khoản mẫu. Bạn đã chạy file database/laptop_shop.sql (bảng users) chưa?", e);
        }
    }
}
