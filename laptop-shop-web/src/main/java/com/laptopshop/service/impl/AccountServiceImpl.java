package com.laptopshop.service.impl;

import com.laptopshop.dto.AccountDTO;
import com.laptopshop.dto.ProfileUpdateDTO;
import com.laptopshop.dto.RegisterDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Customer;
import com.laptopshop.entity.Role;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.AccountService;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ImportReceiptRepository importReceiptRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository,
                              RoleRepository roleRepository,
                              CustomerRepository customerRepository,
                              OrderRepository orderRepository,
                              ImportReceiptRepository importReceiptRepository,
                              PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.importReceiptRepository = importReceiptRepository;
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

        Customer customer = customerRepository.findByAccountId(savedAccount.getId()).orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setAccountId(savedAccount.getId());
            customer.setCustomerName(savedAccount.getFullName());
            customer.setEmail(savedAccount.getEmail());
            customer.setPhone(savedAccount.getPhone());
            customer.setAddress(dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress().trim() : "Chưa cập nhật");
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
        if ("CUSTOMER".equalsIgnoreCase(role.getRoleName())) {
            throw new BusinessException("Không được tạo tài khoản khách hàng trong khu vực quản trị. "
                    + "Khách hàng tự đăng ký tài khoản trên website; quản trị chỉ tạo tài khoản nhân viên (STAFF) hoặc quản trị viên (ADMIN).");
        }

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
        boolean wasCustomer = "CUSTOMER".equalsIgnoreCase(account.getRole().getRoleName());
        boolean toCustomer = "CUSTOMER".equalsIgnoreCase(role.getRoleName());
        if (wasCustomer != toCustomer) {
            throw new BusinessException("Không thể chuyển tài khoản khách hàng thành nhân viên/quản trị (hoặc ngược lại). "
                    + "Hãy khóa tài khoản và tạo tài khoản nhân viên mới nếu cần.");
        }

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
        if (importReceiptRepository.existsByCreatedById(id) || importReceiptRepository.existsByApprovedById(id)) {
            throw new BusinessException("Không thể xóa tài khoản đã tạo hoặc duyệt phiếu nhập. Hãy khóa tài khoản thay thế.");
        }
        try {
            accountRepository.delete(account);
            accountRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Không thể xóa tài khoản đang được tham chiếu bởi dữ liệu khác. Hãy khóa tài khoản thay thế.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileUpdateDTO getProfileForm(String username) {
        Account account = findByUsername(username);
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFullName(account.getFullName());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        if ("CUSTOMER".equalsIgnoreCase(account.getRole().getRoleName())) {
            customerRepository.findByAccountId(account.getId())
                    .ifPresent(c -> dto.setAddress(c.getAddress()));
        }
        return dto;
    }

    @Override
    @Transactional
    public Account updateProfile(String username, ProfileUpdateDTO dto) {
        Account account = findByUsername(username);
        if (dto.getCurrentPassword() == null
                || !passwordEncoder.matches(dto.getCurrentPassword(), account.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng. Không thể lưu thay đổi.");
        }
        String cleanEmail = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : null;
        if (cleanEmail != null && accountRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, account.getId())) {
            throw new BusinessException("Email '" + cleanEmail + "' đã được tài khoản khác sử dụng!");
        }
        String cleanPhone = dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone().trim() : null;
        account.setFullName(dto.getFullName().trim());
        account.setEmail(cleanEmail);
        account.setPhone(cleanPhone);

        // Đồng bộ hồ sơ khách hàng (tên, email, SĐT, địa chỉ giao hàng) để đơn hàng & quản trị luôn thấy thông tin mới nhất
        if ("CUSTOMER".equalsIgnoreCase(account.getRole().getRoleName())) {
            customerRepository.findByAccountId(account.getId()).ifPresent(customer -> {
                if (cleanEmail != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, customer.getId())) {
                    throw new BusinessException("Email '" + cleanEmail + "' đã được khách hàng khác sử dụng!");
                }
                if (cleanPhone != null && customerRepository.existsByPhoneAndIdNot(cleanPhone, customer.getId())) {
                    throw new BusinessException("Số điện thoại '" + cleanPhone + "' đã được khách hàng khác sử dụng!");
                }
                customer.setCustomerName(account.getFullName());
                customer.setEmail(cleanEmail);
                if (cleanPhone != null) customer.setPhone(cleanPhone);
                if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
                    customer.setAddress(dto.getAddress().trim());
                }
                customerRepository.save(customer);
            });
        }

        String newPassword = dto.getNewPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự.");
            }
            if (!newPassword.equals(dto.getConfirmNewPassword())) {
                throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
            }
            if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
                throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        return accountRepository.save(account);
    }
}
