package com.laptopshop.service;

import com.laptopshop.config.CustomUserDetails;
import com.laptopshop.entity.AuditLog;
import com.laptopshop.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(Integer actorId, String username, String action,
                                 String entityType, String entityId, String details) {
        saveLog(actorId, username, action, entityType, entityId, details);
    }

    public void logAction(String action, String entityType, String entityId, String details) {
        Integer actorId = null;
        String username = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            username = auth.getName();
            if (auth.getPrincipal() instanceof CustomUserDetails cud) {
                actorId = cud.getAccountId();
                username = cud.getUsername();
            }
        }
        saveLog(actorId, username, action, entityType, entityId, details);
    }

    public void logAction(Integer actorId, String username, String action,
                          String entityType, String entityId, String details) {
        saveLog(actorId, username != null ? username : "SYSTEM", action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            return auditLogRepository
                    .findByActorUsernameContainingIgnoreCaseOrEntityTypeContainingIgnoreCaseOrderByCreatedAtDesc(kw, kw, pageable);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private void saveLog(Integer actorId, String username, String action,
                         String entityType, String entityId, String details) {
        String ip = resolveClientIp();
        String safeDetails = details != null && details.length() > 1000 ? details.substring(0, 1000) : details;
        auditLogRepository.save(new AuditLog(actorId, username, action, entityType, entityId, safeDetails, ip));
    }

    private static String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "local";
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
