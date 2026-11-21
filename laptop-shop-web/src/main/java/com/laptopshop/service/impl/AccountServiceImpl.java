package com.laptopshop.service.impl;

// Thành viên phụ trách: TV5 - Vũ Gia Bảo
import com.laptopshop.dto.AccountDTO;
import com.laptopshop.dto.RegisterDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Customer;
import com.laptopshop.entity.Role;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.repository.RoleRepository;
import com.laptopshop.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository,
                              RoleRepository roleRepository,
                              CustomerRepository customerRepository,
                              OrderRepository orderRepository,
                              PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account registerCustomer(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp.");
        }
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();

        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Tên đăng nhập '" + username + "' đã tồn tại!");
        }
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email '" + email + "' đã được sử dụng!");
        }

        Role customerRole = roleRepository.findByRoleNameIgnoreCase("CUSTOMER")
                .orElseThrow(() -> new BusinessException("Chưa khởi tạo vai trò CUSTOMER trong CSDL."));

        Account account = new Account();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setFullName(dto.getFullName().trim());
        account.setEmail(email);
        account.setPhone(dto.getPhone().trim());
        account.setRole(customerRole);
        account.setIsActive(true);
        Account savedAccount = accountRepository.save(account);

        // Tự động tạo hoặc gắn hồ sơ Customer cho tài khoản khách hàng mới
        Customer customer = customerRepository.findByPhone(dto.getPhone().trim()).orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setAccountId(savedAccount.getId());
            customer.setCustomerName(savedAccount.getFullName());
            customer.setEmail(savedAccount.getEmail());
            customer.setPhone(savedAccount.getPhone());
            customer.setAddress(dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress().trim() : "Chưa cập nhật");
            customerRepository.save(customer);
        } else if (customer.getAccountId() == null) {
            customer.setAccountId(savedAccount.getId());
            customerRepository.save(customer);
        }

        return savedAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Account> getAccounts(String keyword, Integer roleId, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return accountRepository.searchAccounts(kw, roleId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Account findByUsername(String username) {
        return accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + username));
    }

    @Override
    @Transactional
    public Account createAccount(AccountDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự khi tạo tài khoản mới.");
        }
        String username = dto.getUsername().trim();
        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Tên đăng nhập '" + username + "' đã tồn tại!");
        }
        String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : null;
        if (email != null && accountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email '" + email + "' đã được sử dụng!");
        }
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò ID: " + dto.getRoleId()));

        Account account = new Account();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword().trim()));
        account.setFullName(dto.getFullName().trim());
        account.setEmail(email);
        account.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        account.setRole(role);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public Account updateAccount(Integer id, AccountDTO dto) {
        Account account = getAccountById(id);
        String username = dto.getUsername().trim();
        if (accountRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new BusinessException("Tên đăng nhập '" + username + "' đã được sử dụng!");
        }
        String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : null;
        if (email != null && accountRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("Email '" + email + "' đã được sử dụng!");
        }
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò ID: " + dto.getRoleId()));

        account.setUsername(username);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().trim().length() < 6) {
                throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự.");
            }
            account.setPasswordHash(passwordEncoder.encode(dto.getPassword().trim()));
        }
        account.setFullName(dto.getFullName().trim());
        account.setEmail(email);
        account.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        account.setRole(role);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void toggleActive(Integer id, Integer currentAccountId) {
        if (id.equals(currentAccountId)) {
            throw new BusinessException("Không thể tự khóa tài khoản đang đăng nhập!");
        }
        Account account = getAccountById(id);
        account.setIsActive(!Boolean.TRUE.equals(account.getIsActive()));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteAccount(Integer id, Integer currentAccountId) {
        if (id.equals(currentAccountId)) {
            throw new BusinessException("Không thể tự xóa tài khoản đang đăng nhập!");
        }
        Account account = getAccountById(id);
        if (customerRepository.findByAccountId(id).isPresent() || orderRepository.countByCreatedBy(id) > 0) {
            throw new BusinessException("Không thể xóa tài khoản đang liên kết với khách hàng hoặc đơn hàng. Hãy khóa tài khoản thay thế.");
        }
        accountRepository.delete(account);
    }

    @Override
    @Transactional
    public Account updateProfile(String username, String fullName, String email, String phone, String newPassword) {
        Account account = findByUsername(username);
        String cleanEmail = (email != null && !email.isBlank()) ? email.trim() : null;
        if (cleanEmail != null && accountRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, account.getId())) {
            throw new BusinessException("Email '" + cleanEmail + "' đã được tài khoản khác sử dụng!");
        }
        account.setFullName(fullName.trim());
        account.setEmail(cleanEmail);
        account.setPhone(phone != null ? phone.trim() : null);
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.trim().length() < 6) {
                throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự.");
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        }
        return accountRepository.save(account);
    }
}
