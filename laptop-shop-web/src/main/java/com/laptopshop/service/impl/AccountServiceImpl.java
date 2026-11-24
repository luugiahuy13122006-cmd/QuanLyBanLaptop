package com.laptopshop.service.impl;

import com.laptopshop.dto.AccountDTO;
import com.laptopshop.dto.ProfileUpdateDTO;
import com.laptopshop.dto.RegisterDTO;
import com.laptopshop.dto.ResetPasswordDTO;
import com.laptopshop.entity.Account;
import com.laptopshop.entity.Customer;
import com.laptopshop.entity.PasswordResetToken;
import com.laptopshop.entity.Role;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.*;
import com.laptopshop.service.AccountService;
import com.laptopshop.service.AuditLogService;
import com.laptopshop.service.EmailService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AccountServiceImpl implements AccountService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,100}$");

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public AccountServiceImpl(AccountRepository accountRepository,
                              RoleRepository roleRepository,
                              CustomerRepository customerRepository,
                              OrderRepository orderRepository,
                              ImportReceiptRepository importReceiptRepository,
                              PasswordResetTokenRepository passwordResetTokenRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService,
                              AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.importReceiptRepository = importReceiptRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    private static void validateStrongPassword(String rawPassword) {
        if (rawPassword == null || !STRONG_PASSWORD.matcher(rawPassword).matches()) {
            throw new BusinessException("Mật khẩu phải từ 8 đến 100 ký tự và gồm cả chữ cái lẫn chữ số.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account registerCustomer(RegisterDTO dto) {
        validateStrongPassword(dto.getPassword());
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
        account.setMustChangePassword(false);
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
        auditLogService.logAction(savedAccount.getId(), savedAccount.getUsername(),
                "REGISTER", "Account", String.valueOf(savedAccount.getId()), "Khách hàng tự đăng ký tài khoản");
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
        validateStrongPassword(dto.getPassword());
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
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setFullName(dto.getFullName().trim());
        account.setEmail(email);
        account.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        account.setRole(role);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        Account saved = accountRepository.save(account);
        auditLogService.logAction("CREATE_ACCOUNT", "Account", String.valueOf(saved.getId()),
                "Tạo tài khoản " + saved.getUsername() + " (" + role.getRoleName() + ")");
        return saved;
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
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            validateStrongPassword(dto.getPassword());
            account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            account.setMustChangePassword(false);
        }
        account.setFullName(dto.getFullName().trim());
        account.setEmail(email);
        account.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        account.setRole(role);
        account.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        Account saved = accountRepository.save(account);
        auditLogService.logAction("UPDATE_ACCOUNT", "Account", String.valueOf(saved.getId()),
                "Cập nhật tài khoản " + saved.getUsername());
        return saved;
    }

    @Override
    @Transactional
    public void toggleActive(Integer id, Integer currentAccountId) {
        if (id.equals(currentAccountId)) {
            throw new BusinessException("Không thể tự khóa tài khoản đang đăng nhập!");
        }
        Account account = getAccountById(id);
        boolean next = !Boolean.TRUE.equals(account.getIsActive());
        account.setIsActive(next);
        if (next) {
            account.setFailedLoginAttempts(0);
            account.setLockedUntil(null);
        }
        accountRepository.save(account);
        auditLogService.logAction("TOGGLE_ACCOUNT", "Account", String.valueOf(id),
                (next ? "Mở khóa " : "Khóa ") + account.getUsername());
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
            passwordResetTokenRepository.deleteByAccount_Id(id);
            accountRepository.delete(account);
            accountRepository.flush();
            auditLogService.logAction("DELETE_ACCOUNT", "Account", String.valueOf(id),
                    "Xóa tài khoản " + account.getUsername());
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
        if (newPassword != null && !newPassword.isEmpty()) {
            validateStrongPassword(newPassword);
            if (!newPassword.equals(dto.getConfirmNewPassword())) {
                throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
            }
            if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
                throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setMustChangePassword(false);
        }
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void recordFailedLogin(String username) {
        if (username == null || username.isBlank()) return;
        accountRepository.findByUsernameIgnoreCase(username.trim()).ifPresent(account -> {
            int attempts = (account.getFailedLoginAttempts() != null ? account.getFailedLoginAttempts() : 0) + 1;
            account.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                account.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                auditLogService.logSecurityEvent(account.getId(), account.getUsername(),
                        "ACCOUNT_LOCKED", "Account", String.valueOf(account.getId()),
                        "Khóa tạm thời " + LOCK_MINUTES + " phút sau " + attempts + " lần sai mật khẩu");
            } else {
                auditLogService.logSecurityEvent(account.getId(), account.getUsername(),
                        "LOGIN_FAILED", "Account", String.valueOf(account.getId()),
                        "Đăng nhập sai mật khẩu lần " + attempts + "/" + MAX_FAILED_ATTEMPTS);
            }
            accountRepository.save(account);
        });
    }

    @Override
    @Transactional
    public void resetFailedLogin(String username) {
        if (username == null || username.isBlank()) return;
        accountRepository.findByUsernameIgnoreCase(username.trim()).ifPresent(account -> {
            if ((account.getFailedLoginAttempts() != null && account.getFailedLoginAttempts() > 0)
                    || account.getLockedUntil() != null) {
                account.setFailedLoginAttempts(0);
                account.setLockedUntil(null);
                accountRepository.save(account);
            }
        });
    }

    @Override
    @Transactional
    public String createPasswordResetToken(String email) {
        if (email == null || email.isBlank()) return null;
        Account account = accountRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
            return null;
        }
        passwordResetTokenRepository.deleteByAccount_Id(account.getId());
        byte[] rnd = new byte[24];
        new SecureRandom().nextBytes(rnd);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        String tokenHash = sha256(rawToken);
        PasswordResetToken entity = new PasswordResetToken(account, tokenHash, LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(entity);

        emailService.sendEmail(account.getEmail(),
                "Đặt lại mật khẩu tài khoản " + account.getUsername(),
                "Mã / liên kết đặt lại mật khẩu của bạn có hiệu lực trong 30 phút: /reset-password?token=" + rawToken);
        auditLogService.logAction(account.getId(), account.getUsername(),
                "FORGOT_PASSWORD", "Account", String.valueOf(account.getId()), "Yêu cầu token đặt lại mật khẩu");
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isResetTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;
        return passwordResetTokenRepository.findByTokenHashAndUsedFalse(sha256(rawToken.trim()))
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        validateStrongPassword(dto.getNewPassword());
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
        }
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndUsedFalse(sha256(dto.getToken().trim()))
                .orElseThrow(() -> new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng."));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Liên kết đặt lại mật khẩu đã hết hạn (quá 30 phút).");
        }
        Account account = token.getAccount();
        account.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        account.setMustChangePassword(false);
        accountRepository.save(account);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        auditLogService.logAction(account.getId(), account.getUsername(),
                "RESET_PASSWORD", "Account", String.valueOf(account.getId()), "Đặt lại mật khẩu thành công");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Account forceChangePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        Account account = findByUsername(username);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        validateStrongPassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
        }
        if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới không được trùng mật khẩu mặc định.");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        Account saved = accountRepository.save(account);
        auditLogService.logAction(saved.getId(), saved.getUsername(),
                "FORCE_CHANGE_PASSWORD", "Account", String.valueOf(saved.getId()), "Đổi mật khẩu bắt buộc thành công");
        return saved;
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
