package com.example.laptopshop.service;

import com.example.laptopshop.dto.RegisterForm;
import com.example.laptopshop.entity.User;
import com.example.laptopshop.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Vừa là UserDetailsService (Spring Security dùng để đăng nhập)
 * vừa chứa nghiệp vụ đăng ký / quản lý tài khoản.
 */
@Service
public class UserService implements UserDetailsService {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Đăng nhập ----------
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole())          // USER -> ROLE_USER, ADMIN -> ROLE_ADMIN
                .disabled(!u.isEnabled())    // tài khoản bị khóa sẽ không đăng nhập được
                .build();
    }

    // ---------- Đăng ký ----------
    @Transactional
    public User register(RegisterForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        User u = new User();
        u.setUsername(username);
        u.setFullName(form.getFullName().trim());
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setRole(ROLE_USER);
        u.setEnabled(true);
        return userRepository.save(u);
    }

    /** Dùng cho DataInitializer để tạo tài khoản mẫu. */
    @Transactional
    public void createIfMissing(String username, String rawPassword, String fullName, String email, String role) {
        if (userRepository.existsByUsername(username)) return;
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        userRepository.save(u);
    }

    // ---------- Quản trị tài khoản ----------
    public List<User> findAll() { return userRepository.findAll(Sort.by("id")); }
    public long countUsers() { return userRepository.count(); }
    public long countAdmins() { return userRepository.countByRole(ROLE_ADMIN); }

    @Transactional
    public void toggleEnabled(Long id, String currentUsername) {
        User u = getOrThrow(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Không thể khóa chính tài khoản đang đăng nhập");
        }
        u.setEnabled(!u.isEnabled());
        userRepository.save(u);
    }

    @Transactional
    public void changeRole(Long id, String role, String currentUsername) {
        if (!ROLE_USER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("Quyền không hợp lệ");
        }
        User u = getOrThrow(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Không thể tự đổi quyền của chính mình");
        }
        u.setRole(role);
        userRepository.save(u);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User u = getOrThrow(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Không thể xóa chính tài khoản đang đăng nhập");
        }
        userRepository.delete(u);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }
}
