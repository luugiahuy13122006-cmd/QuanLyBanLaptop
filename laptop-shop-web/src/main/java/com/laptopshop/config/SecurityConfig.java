package com.example.laptopshop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ai cũng xem được: trang chủ, chi tiết, tìm kiếm, giỏ hàng, đăng ký/đăng nhập, file tĩnh
                .requestMatchers("/", "/san-pham/**", "/tim-kiem", "/gio-hang/**",
                                 "/dang-ky", "/dang-nhap", "/error",
                                 "/css/**", "/js/**", "/images/**").permitAll()
                // Chỉ ADMIN
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                // Còn lại (vd: /dat-hang) phải đăng nhập
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/dang-nhap")
                .loginProcessingUrl("/dang-nhap")
                .defaultSuccessUrl("/", false)   // false: quay lại trang đang xem dở nếu có
                .failureUrl("/dang-nhap?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/dang-xuat")
                .logoutSuccessUrl("/dang-nhap?logout")
                .permitAll()
            );
        return http.build();
    }
}
