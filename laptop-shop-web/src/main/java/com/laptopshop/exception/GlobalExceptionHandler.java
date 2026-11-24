package com.laptopshop.exception;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.ShopProperties;
import com.laptopshop.entity.AdminModule;
import com.laptopshop.entity.PermissionLevel;
import com.laptopshop.service.CartService;
import com.laptopshop.service.ContentAndChatService;
import com.laptopshop.service.StaffPermissionService;
import com.laptopshop.service.WishlistAndCompareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ShopProperties shopProperties;
    private final Environment environment;
    private final ObjectProvider<CartService> cartServiceProvider;
    private final ObjectProvider<WishlistAndCompareService> compareServiceProvider;
    private final ObjectProvider<StaffPermissionService> staffPermissionServiceProvider;
    private final ObjectProvider<ContentAndChatService> contentAndChatServiceProvider;

    public GlobalExceptionHandler(ShopProperties shopProperties,
                                  Environment environment,
                                  ObjectProvider<CartService> cartServiceProvider,
                                  ObjectProvider<WishlistAndCompareService> compareServiceProvider,
                                  ObjectProvider<StaffPermissionService> staffPermissionServiceProvider,
                                  ObjectProvider<ContentAndChatService> contentAndChatServiceProvider) {
        this.shopProperties = shopProperties;
        this.environment = environment;
        this.cartServiceProvider = cartServiceProvider;
        this.compareServiceProvider = compareServiceProvider;
        this.staffPermissionServiceProvider = staffPermissionServiceProvider;
        this.contentAndChatServiceProvider = contentAndChatServiceProvider;
    }

    @ModelAttribute("shop")
    public ShopProperties.ShopInfo shop() {
        return shopProperties.getShop();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        if (request != null) {
            Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
            if (csrfAttr instanceof CsrfToken csrfToken) {
                csrfToken.getToken();
            }
            return request.getRequestURI();
        }
        return "";
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "";
        }
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getFullName();
        }
        return auth.getName();
    }

    @ModelAttribute("currentUserRole")
    public String currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "";
        }
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getRoleName();
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
    }

    @ModelAttribute("cartCount")
    @SuppressWarnings("unchecked")
    public int cartCount(HttpServletRequest request) {
        if (request == null) return 0;
        HttpSession session = request.getSession(false);
        Integer accountId = currentAccountId();
        CartService cartService = cartServiceProvider.getIfAvailable();
        if (cartService != null) {
            return cartService.getCartCount(accountId, session);
        }
        if (session == null) return 0;
        Object cartObj = session.getAttribute("cart");
        if (cartObj instanceof Map<?, ?> cart) {
            return cart.values().stream()
                    .filter(Integer.class::isInstance)
                    .mapToInt(Integer.class::cast)
                    .sum();
        }
        return 0;
    }

    @ModelAttribute("compareCount")
    public int compareCount(HttpServletRequest request) {
        if (request == null) return 0;
        WishlistAndCompareService svc = compareServiceProvider.getIfAvailable();
        return svc != null ? svc.getCompareIds(request.getSession(false)).size() : 0;
    }

    @ModelAttribute("staffPerms")
    public Map<String, PermissionLevel> staffPerms() {
        CustomUserDetails cud = currentUserDetails();
        if (cud == null) return Map.of();
        if ("ADMIN".equalsIgnoreCase(cud.getRoleName())) {
            Map<String, PermissionLevel> full = new LinkedHashMap<>();
            for (AdminModule m : AdminModule.values()) {
                full.put(m.name(), PermissionLevel.FULL);
            }
            return full;
        }
        if ("STAFF".equalsIgnoreCase(cud.getRoleName())) {
            StaffPermissionService svc = staffPermissionServiceProvider.getIfAvailable();
            if (svc != null) {
                return svc.getPermissionMapByKey(cud.getAccountId());
            }
        }
        return Map.of();
    }

    @ModelAttribute("unreadChatCount")
    public long unreadChatCount() {
        CustomUserDetails cud = currentUserDetails();
        if (cud == null) return 0L;
        if ("ADMIN".equalsIgnoreCase(cud.getRoleName()) || "STAFF".equalsIgnoreCase(cud.getRoleName())) {
            ContentAndChatService svc = contentAndChatServiceProvider.getIfAvailable();
            return svc != null ? svc.getUnreadCustomerMessageCount() : 0L;
        }
        return 0L;
    }

    private CustomUserDetails currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud;
        }
        return null;
    }

    private Integer currentAccountId() {
        CustomUserDetails cud = currentUserDetails();
        return cud != null ? cud.getAccountId() : null;
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorTitle", "Không tìm thấy trang hoặc dữ liệu");
        model.addAttribute("errorMessage", ex instanceof NoResourceFoundException
                ? "Đường dẫn bạn truy cập không tồn tại hoặc đã bị thay đổi."
                : ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("errorTitle", "Không có quyền truy cập");
        model.addAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này.");
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("errorTitle", "Lỗi nghiệp vụ");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(ObjectOptimisticLockingFailureException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("errorTitle", "Xung đột cập nhật đồng thời");
        model.addAttribute("errorMessage", "Dữ liệu vừa được người dùng khác cập nhật. Vui lòng tải lại trang và thử lại.");
        return "error/error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("errorTitle", "Vi phạm ràng buộc dữ liệu");
        model.addAttribute("errorMessage", "Dữ liệu đang được tham chiếu hoặc bị trùng khóa duy nhất, không thể thực hiện thao tác.");
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model, HttpServletResponse response) {
        log.error("[UNHANDLED] {}", ex.getMessage(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("errorTitle", "Lỗi hệ thống");
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        model.addAttribute("errorMessage", isProd
                ? "Hệ thống đang gặp sự cố tạm thời. Vui lòng thử lại sau hoặc liên hệ quản trị viên."
                : (ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định"));
        return "error/error";
    }
}
