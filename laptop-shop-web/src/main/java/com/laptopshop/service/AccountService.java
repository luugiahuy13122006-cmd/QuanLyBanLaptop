package com.laptopshop.service;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import com.laptopshop.dto.AccountDTO;
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
    Account updateProfile(String username, String fullName, String email, String phone, String newPassword);
}
