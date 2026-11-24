package com.laptopshop.config;

import com.laptopshop.entity.Account;
import com.laptopshop.repository.AccountRepository;
import com.laptopshop.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(AccountRepository accountRepository,
                                    CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        Integer customerId = customerRepository.findByAccountId(account.getId())
                .map(c -> c.getId())
                .orElse(null);
        return new CustomUserDetails(account, customerId);
    }
}
