package com.laptopshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static String safeRedirect(String target) {
        if (target == null || target.isBlank()) return null;
        String t = target.trim();
        if (!t.startsWith("/") || t.startsWith("//") || t.contains("\\") || t.contains("://")) return null;
        if (t.chars().anyMatch(c -> c < 32)) return null;
        if (t.startsWith("/login") || t.startsWith("/logout") || t.startsWith("/register")) return null;
        return t;
    }

    private static String refererPath(HttpServletRequest request) {
        String ref = request.getHeader("Referer");
        if (ref == null) return null;
        try {
            URI uri = URI.create(ref);
            if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(request.getServerName())) return null;
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            return uri.getRawQuery() != null ? path + "?" + uri.getRawQuery() : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String redirectQuery(String redirect) {
        String safe = safeRedirect(redirect);
        return safe != null ? "&redirect=" + URLEncoder.encode(safe, StandardCharsets.UTF_8) : "";
    }

    @Bean
    public AuthenticationEntryPoint loginEntryPoint() {
        return (request, response, ex) -> {
            String ctx = request.getContextPath();
            String back;
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String q = request.getQueryString();
                back = request.getRequestURI().substring(ctx.length()) + (q != null ? "?" + q : "");
            } else {
                back = refererPath(request);
            }
            response.sendRedirect(ctx + "/login?required=true" + redirectQuery(back));
        };
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdminOrStaff = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_STAFF"));
            String ctx = request.getContextPath();
            if (isAdminOrStaff) {
                response.sendRedirect(ctx + "/admin/dashboard");
                return;
            }
            String redirect = safeRedirect(request.getParameter("redirect"));
            response.sendRedirect(ctx + (redirect != null ? redirect : "/"));
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, ex) ->
                response.sendRedirect(request.getContextPath() + "/login?error=true"
                        + redirectQuery(request.getParameter("redirect")));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/san-pham/**", "/tim-kiem",
                    "/qr-pay/**",
                    "/login", "/register", "/403", "/error", "/error/**",
                    "/css/**", "/images/**", "/js/**"
                ).permitAll()
                .requestMatchers(
                    "/gio-hang/**", "/dat-hang/**", "/thanh-toan/**",
                    "/don-hang-cua-toi/**", "/tai-khoan/**"
                ).authenticated()
                .requestMatchers("/admin/accounts/**").hasRole("ADMIN")
                .requestMatchers("/admin/import-receipts/*/approve", "/admin/import-receipts/*/reject").hasRole("ADMIN")
                .requestMatchers(
                    "/admin/laptops/add/**", "/admin/laptops/edit/**", "/admin/laptops/delete/**",
                    "/admin/categories/add/**", "/admin/categories/edit/**", "/admin/categories/delete/**",
                    "/admin/brands/add/**", "/admin/brands/edit/**", "/admin/brands/delete/**",
                    "/admin/customers/delete/**"
                ).hasRole("ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(loginEntryPoint())
                .accessDeniedPage("/403")
            );

        return http.build();
    }
}
