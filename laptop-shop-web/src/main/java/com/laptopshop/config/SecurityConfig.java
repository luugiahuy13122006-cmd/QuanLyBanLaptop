package com.laptopshop.config;

import com.laptopshop.service.AccountService;
import com.laptopshop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
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
    public AuthenticationSuccessHandler roleBasedSuccessHandler(ObjectProvider<AccountService> accountServiceProvider,
                                                              ObjectProvider<CartService> cartServiceProvider) {
        return (request, response, authentication) -> {
            AccountService svc = accountServiceProvider.getIfAvailable();
            if (svc != null && authentication != null) {
                try {
                    svc.resetFailedLogin(authentication.getName());
                } catch (Exception ignored) {}
            }
            CartService cartSvc = cartServiceProvider.getIfAvailable();
            if (cartSvc != null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
                try {
                    cartSvc.mergeSessionCartToDb(cud.getAccountId(), request.getSession(true));
                } catch (Exception ignored) {}
            }
            String ctx = request.getContextPath();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud
                    && cud.isMustChangePassword()) {
                response.sendRedirect(ctx + "/tai-khoan/doi-mat-khau-bat-buoc");
                return;
            }
            boolean isAdminOrStaff = authentication != null && authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_STAFF"));
            if (isAdminOrStaff) {
                response.sendRedirect(ctx + "/admin/dashboard");
                return;
            }
            String redirect = safeRedirect(request.getParameter("redirect"));
            response.sendRedirect(ctx + (redirect != null ? redirect : "/"));
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler(ObjectProvider<AccountService> accountServiceProvider) {
        return (request, response, ex) -> {
            AccountService svc = accountServiceProvider.getIfAvailable();
            String username = request.getParameter("username");
            if (svc != null && username != null) {
                try {
                    svc.recordFailedLogin(username);
                } catch (Exception ignored) {}
            }
            response.sendRedirect(request.getContextPath() + "/login?error=true"
                    + redirectQuery(request.getParameter("redirect")));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<AccountService> accountServiceProvider,
                                                   ObjectProvider<CartService> cartServiceProvider) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/webhooks/payment", "/qr-pay/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(ct -> {})
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                    "font-src 'self' data: https://cdn.jsdelivr.net https://fonts.gstatic.com; " +
                    "img-src 'self' data: blob: https://img.vietqr.io; " +
                    "connect-src 'self';"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/san-pham/**", "/tim-kiem", "/thong-tin/**",
                    "/qr-pay/**", "/bao-hanh/**", "/ho-tro/**",
                    "/api/webhooks/payment", "/api/store/**", "/api/coupons/check", "/api/chat/**",
                    "/login", "/register",
                    "/403", "/error", "/error/**",
                    "/css/**", "/images/**", "/js/**", "/vendor/**",
                    "/manifest.json", "/sw.js", "/robots.txt", "/sitemap.xml"
                ).permitAll()
                .requestMatchers(
                    "/gio-hang/**", "/dat-hang/**", "/thanh-toan/**",
                    "/don-hang-cua-toi/**", "/tai-khoan/**",
                    "/yeu-thich/**", "/so-sanh/**"
                ).authenticated()
                .requestMatchers("/orders/place").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers(
                    "/admin/accounts/**", "/admin/audit-logs/**",
                    "/admin/coupons/**", "/admin/suppliers/**",
                    "/admin/stock/**"
                ).hasRole("ADMIN")
                .requestMatchers("/admin/import-receipts/*/approve", "/admin/import-receipts/*/reject").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedSuccessHandler(accountServiceProvider, cartServiceProvider))
                .failureHandler(loginFailureHandler(accountServiceProvider))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionConcurrency(concurrency -> concurrency
                    .maximumSessions(5)
                    .sessionRegistry(sessionRegistry())
                )
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(loginEntryPoint())
                .accessDeniedPage("/403")
            );

        return http.build();
    }
}
