package com.laptopshop.config;

import com.laptopshop.entity.CodeSequence;
import com.laptopshop.entity.Role;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.CodeSequenceRepository;
import com.laptopshop.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeSequenceRepository codeSequenceRepository;

    public DataInitializer(RoleRepository roleRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           CodeSequenceRepository codeSequenceRepository) {
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeSequenceRepository = codeSequenceRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole("ADMIN", "Quản trị hệ thống");
        ensureRole("STAFF", "Nhân viên cửa hàng");
        ensureRole("CUSTOMER", "Khách hàng");

        ensureSequence("DON_HANG");
        ensureSequence("PHIEU_NHAP");

        String legacyPlaceholderHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        accountRepository.findAll().forEach(acc -> {
            if (legacyPlaceholderHash.equals(acc.getPasswordHash())) {
                acc.setPasswordHash(passwordEncoder.encode("123456"));
                accountRepository.save(acc);
                log.info("[INIT] Đã chuẩn hóa mật khẩu BCrypt (123456) cho tài khoản '{}'", acc.getUsername());
            }
        });
    }

    private void ensureRole(String roleName, String description) {
        if (roleRepository.findByRoleNameIgnoreCase(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName, description));
        }
    }

    private void ensureSequence(String name) {
        if (codeSequenceRepository.findById(name).isEmpty()) {
            codeSequenceRepository.save(new CodeSequence(name, 0));
            log.info("[INIT] Khởi tạo bộ đếm mã '{}' = 0", name);
        }
    }
}
