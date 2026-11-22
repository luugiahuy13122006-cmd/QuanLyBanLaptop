package com.laptopshop.config;

// Thành viên phụ trách: TV2 - Phạm Hữu Hiệu (hỗ trợ phục vụ ảnh sản phẩm upload tức thì từ thư mục static)
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static Path resolveLaptopImagesBaseDir() {
        Path direct = Paths.get("src", "main", "resources", "static", "images", "laptops");
        if (Files.exists(direct.getParent())) {
            return direct.toAbsolutePath().normalize();
        }
        Path subModule = Paths.get("laptop-shop-web", "src", "main", "resources", "static", "images", "laptops");
        if (Files.exists(subModule.getParent())) {
            return subModule.toAbsolutePath().normalize();
        }
        return direct.toAbsolutePath().normalize();
    }

    public static Path resolveTargetLaptopImagesBaseDir() {
        Path direct = Paths.get("target", "classes", "static", "images", "laptops");
        if (Files.exists(direct.getParent())) {
            return direct.toAbsolutePath().normalize();
        }
        Path subModule = Paths.get("laptop-shop-web", "target", "classes", "static", "images", "laptops");
        if (Files.exists(subModule.getParent())) {
            return subModule.toAbsolutePath().normalize();
        }
        return null;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path baseDir = resolveLaptopImagesBaseDir();
        String location = baseDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/images/laptops/**")
                .addResourceLocations(location, "classpath:/static/images/laptops/");
    }
}
