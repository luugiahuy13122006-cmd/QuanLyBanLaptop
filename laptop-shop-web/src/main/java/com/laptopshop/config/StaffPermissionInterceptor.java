package com.laptopshop.config;

import com.laptopshop.entity.AdminModule;
import com.laptopshop.entity.PermissionLevel;
import com.laptopshop.service.StaffPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StaffPermissionInterceptor implements HandlerInterceptor {

    private final StaffPermissionService permissionService;

    public StaffPermissionInterceptor(StaffPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isStaff = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()));
        if (isAdmin || !isStaff) {
            return true;
        }
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }

        AdminModule module = resolveModule(uri);
        if (module == null) {
            return true;
        }
        PermissionLevel required = resolveRequiredLevel(request.getMethod(), uri);
        boolean allowed = (auth.getPrincipal() instanceof CustomUserDetails cud)
                ? permissionService.hasAccess(cud.getAccountId(), module, required)
                : permissionService.hasAccessByUsername(auth.getName(), module, required);
        if (!allowed) {
            throw new AccessDeniedException("Tài khoản nhân viên chưa được cấp quyền " + required.getDisplayName()
                    + " tại mục " + module.getDisplayName());
        }
        return true;
    }

    static AdminModule resolveModule(String uri) {
        if (uri.startsWith("/admin/statistics")) return AdminModule.STATISTICS;
        if (uri.startsWith("/admin/laptops")) return AdminModule.LAPTOPS;
        if (uri.startsWith("/admin/categories")) return AdminModule.CATEGORIES;
        if (uri.startsWith("/admin/brands")) return AdminModule.BRANDS;
        if (uri.startsWith("/admin/import-receipts")) return AdminModule.IMPORT_RECEIPTS;
        if (uri.startsWith("/admin/orders")) return AdminModule.ORDERS;
        if (uri.startsWith("/admin/customers")) return AdminModule.CUSTOMERS;
        if (uri.startsWith("/admin/warranties")) return AdminModule.WARRANTIES;
        if (uri.startsWith("/admin/reviews")) return AdminModule.REVIEWS;
        if (uri.startsWith("/admin/content")) return AdminModule.SUPPORT_CHAT;
        return null;
    }

    static PermissionLevel resolveRequiredLevel(String method, String uri) {
        if (uri.contains("/delete")) {
            return PermissionLevel.FULL;
        }
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return PermissionLevel.EDIT;
        }
        if (uri.endsWith("/add") || uri.contains("/edit/") || uri.endsWith("/create")) {
            return PermissionLevel.EDIT;
        }
        return PermissionLevel.VIEW;
    }
}
