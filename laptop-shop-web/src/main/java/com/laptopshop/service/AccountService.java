package com.laptopshop.service;

import com.laptopshop.dto.AccountDTO;
import com.laptopshop.dto.ProfileUpdateDTO;
import com.laptopshop.dto.RegisterDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {
    Account registerCustomer(RegisterDTO dto);
    Page<Account> getAccounts(String keyword, Integer roleId, Pageable pageable);
    List<Role> getAllRoles();
    Account getAccountById(Integer id);
    Account findByUsername(String username);
    Account createAccount(AccountDTO dto);
    Account updateAccount(Integer id, AccountDTO dto);
    void toggleActive(Integer id, Integer currentAccountId);
    void deleteAccount(Integer id, Integer currentAccountId);
    Account updateProfile(String username, ProfileUpdateDTO dto);
    /** Dựng sẵn dữ liệu form hồ sơ cá nhân (họ tên, email, SĐT, địa chỉ giao hàng nếu là khách hàng). */
    ProfileUpdateDTO getProfileForm(String username);
}
