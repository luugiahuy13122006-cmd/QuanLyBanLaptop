package com.example.laptopshop.service;

import com.example.laptopshop.dto.RegisterForm;
import com.example.laptopshop.entity.User;
import com.example.laptopshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private RegisterForm form() {
        RegisterForm f = new RegisterForm();
        f.setUsername("khach01");
        f.setFullName("Nguyen Van A");
        f.setEmail("A@Mail.com");
        f.setPassword("123456");
        f.setConfirmPassword("123456");
        return f;
    }

    private User user(Long id, String username, String role, boolean enabled) {
        User u = new User();
        u.setId(id); u.setUsername(username); u.setPassword("hash");
        u.setRole(role); u.setEnabled(enabled);
        return u;
    }

    @Test
    void register_thanhCong_maHoaMatKhauVaGanQuyenUser() {
        when(userRepository.existsByUsername("khach01")).thenReturn(false);
        when(userRepository.existsByEmail("a@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.register(form());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("HASHED", saved.getPassword());
        assertEquals("USER", saved.getRole());
        assertEquals("a@mail.com", saved.getEmail());
        assertTrue(saved.isEnabled());
    }

    @Test
    void register_trungTenDangNhap_nemLoi() {
        when(userRepository.existsByUsername("khach01")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.register(form()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_trungEmail_nemLoi() {
        when(userRepository.existsByUsername("khach01")).thenReturn(false);
        when(userRepository.existsByEmail("a@mail.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.register(form()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_khongTonTai_nemUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_adminCoRoleAdmin_taiKhoanKhoaThiDisabled() {
        when(userRepository.findByUsername("boss")).thenReturn(Optional.of(user(1L, "boss", "ADMIN", false)));
        UserDetails d = userService.loadUserByUsername("boss");
        assertTrue(d.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(d.isEnabled());
    }

    @Test
    void toggleEnabled_chinhMinh_nemLoi() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", "ADMIN", true)));
        assertThrows(IllegalArgumentException.class, () -> userService.toggleEnabled(1L, "admin"));
    }

    @Test
    void toggleEnabled_nguoiKhac_daoTrangThai() {
        User u = user(2L, "khach01", "USER", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));
        userService.toggleEnabled(2L, "admin");
        assertFalse(u.isEnabled());
    }

    @Test
    void changeRole_quyenKhongHopLe_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> userService.changeRole(2L, "SUPERMAN", "admin"));
    }

    @Test
    void delete_chinhMinh_nemLoi() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", "ADMIN", true)));
        assertThrows(IllegalArgumentException.class, () -> userService.delete(1L, "admin"));
        verify(userRepository, never()).delete(any());
    }
}
