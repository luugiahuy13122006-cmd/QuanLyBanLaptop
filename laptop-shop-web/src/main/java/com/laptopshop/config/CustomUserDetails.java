package com.laptopshop.config;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import com.laptopshop.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Integer accountId;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String roleName;
    private final boolean active;

    public CustomUserDetails(Account account) {
        this.accountId = account.getId();
        this.username = account.getUsername();
        this.password = account.getPasswordHash();
        this.fullName = account.getFullName();
        this.email = account.getEmail();
        this.phone = account.getPhone();
        this.roleName = account.getRole() != null ? account.getRole().getRoleName() : "CUSTOMER";
        this.active = Boolean.TRUE.equals(account.getIsActive());
    }

    public Integer getAccountId() { return accountId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRoleName() { return roleName; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
