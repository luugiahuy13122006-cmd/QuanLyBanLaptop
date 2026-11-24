package com.laptopshop.service;

import com.laptopshop.entity.*;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.StaffPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StaffPermissionService {

    private final StaffPermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public StaffPermissionService(StaffPermissionRepository permissionRepository,
                                  AccountRepository accountRepository,
                                  AuditLogService auditLogService) {
        this.permissionRepository = permissionRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
    }

    /** Mặc định ban đầu cho tài khoản STAFF mới hoặc chưa cấu hình phân quyền chi tiết. */
    public static PermissionLevel defaultLevelFor(AdminModule module) {
        return switch (module) {
            case STATISTICS, CATEGORIES, BRANDS -> PermissionLevel.VIEW;
            case LAPTOPS, IMPORT_RECEIPTS, ORDERS, CUSTOMERS, WARRANTIES, REVIEWS -> PermissionLevel.EDIT;
            case SUPPORT_CHAT -> PermissionLevel.FULL;
        };
    }

    @Transactional(readOnly = true)
    public Map<AdminModule, PermissionLevel> getEffectivePermissions(Account account) {
        Map<AdminModule, PermissionLevel> map = new EnumMap<>(AdminModule.class);
        if (account == null || account.getRole() == null) {
            for (AdminModule m : AdminModule.values()) map.put(m, PermissionLevel.NONE);
            return map;
        }
        String role = account.getRole().getRoleName();
        if ("ADMIN".equalsIgnoreCase(role)) {
            for (AdminModule m : AdminModule.values()) map.put(m, PermissionLevel.FULL);
            return map;
        }
        if (!"STAFF".equalsIgnoreCase(role)) {
            for (AdminModule m : AdminModule.values()) map.put(m, PermissionLevel.NONE);
            return map;
        }
        List<StaffPermission> saved = permissionRepository.findByAccount_Id(account.getId());
        if (saved.isEmpty()) {
            for (AdminModule m : AdminModule.values()) map.put(m, defaultLevelFor(m));
            return map;
        }
        for (AdminModule m : AdminModule.values()) map.put(m, PermissionLevel.NONE);
        for (StaffPermission sp : saved) {
            map.put(sp.getModule(), sp.getLevel());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, PermissionLevel> getPermissionMapByKey(Integer accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);
        Map<AdminModule, PermissionLevel> raw = getEffectivePermissions(account);
        Map<String, PermissionLevel> result = new LinkedHashMap<>();
        for (AdminModule m : AdminModule.values()) {
            result.put(m.name(), raw.getOrDefault(m, PermissionLevel.NONE));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Map<AdminModule, PermissionLevel>> getPermissionsForAccounts(Collection<Account> accounts) {
        Map<Integer, Map<AdminModule, PermissionLevel>> result = new LinkedHashMap<>();
        for (Account a : accounts) {
            if (a != null && a.getRole() != null && "STAFF".equalsIgnoreCase(a.getRole().getRoleName())) {
                result.put(a.getId(), getEffectivePermissions(a));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Integer accountId, AdminModule module, PermissionLevel required) {
        if (accountId == null) return false;
        return checkAccess(accountRepository.findById(accountId).orElse(null), module, required);
    }

    @Transactional(readOnly = true)
    public boolean hasAccessByUsername(String username, AdminModule module, PermissionLevel required) {
        if (username == null || username.isBlank()) return false;
        return checkAccess(accountRepository.findByUsernameIgnoreCase(username).orElse(null), module, required);
    }

    private boolean checkAccess(Account account, AdminModule module, PermissionLevel required) {
        if (account == null || !Boolean.TRUE.equals(account.getIsActive()) || account.getRole() == null) return false;
        String role = account.getRole().getRoleName();
        if ("ADMIN".equalsIgnoreCase(role)) return true;
        if (!"STAFF".equalsIgnoreCase(role)) return false;
        PermissionLevel granted = getEffectivePermissions(account).getOrDefault(module, PermissionLevel.NONE);
        return switch (required) {
            case NONE -> true;
            case VIEW -> granted.canView();
            case EDIT -> granted.canEdit();
            case FULL -> granted.canDelete();
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePermissionsForStaff(Integer staffAccountId, Map<AdminModule, PermissionLevel> newPerms) {
        Account staff = accountRepository.findById(staffAccountId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản nhân viên ID: " + staffAccountId));
        if (staff.getRole() == null || !"STAFF".equalsIgnoreCase(staff.getRole().getRoleName())) {
            throw new BusinessException("Chỉ áp dụng phân quyền chi tiết cho tài khoản nhân viên (STAFF).");
        }
        permissionRepository.deleteByAccount_Id(staffAccountId);
        permissionRepository.flush();
        for (AdminModule module : AdminModule.values()) {
            PermissionLevel level = newPerms != null ? newPerms.getOrDefault(module, PermissionLevel.NONE) : PermissionLevel.NONE;
            permissionRepository.save(new StaffPermission(staff, module, level));
        }
        auditLogService.logAction("UPDATE_STAFF_PERMISSIONS", "Account", String.valueOf(staffAccountId),
                "Cập nhật phân quyền chi tiết cho nhân viên " + staff.getUsername());
    }
}
