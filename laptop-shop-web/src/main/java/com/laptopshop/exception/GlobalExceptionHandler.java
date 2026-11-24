package com.laptopshop.exception;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
// Xử lý ngoại lệ tập trung (@ControllerAdvice) và cung cấp thuộc tính dùng chung (currentUri, cartCount) cho mọi giao diện.

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.config.ShopProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ShopProperties shopProperties;

    public GlobalExceptionHandler(ShopProperties shopProperties) {
        this.shopProperties = shopProperties;
    }

    /** Thông tin cửa hàng (tên, hotline, địa chỉ, email, giờ làm việc) dùng chung cho header, footer, báo cáo. */
    @ModelAttribute("shop")
    public ShopProperties.ShopInfo shop() {
        return shopProperties.getShop();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        if (request != null) {
            // Khởi tạo CSRF token trước khi Thymeleaf render để tránh lỗi commit response khi trang dài
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

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("errorTitle", "Không tìm thấy trang hoặc dữ liệu");
        model.addAttribute("errorMessage", ex.getMessage());
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("errorTitle", "Vi phạm ràng buộc dữ liệu");
        model.addAttribute("errorMessage", "Dữ liệu đang được tham chiếu hoặc bị trùng khóa duy nhất, không thể thực hiện thao tác.");
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("errorTitle", "Lỗi hệ thống");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định");
        return "error/error";
    }
}
