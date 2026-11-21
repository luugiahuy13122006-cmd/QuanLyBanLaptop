package com.laptopshop.service.impl;

// Thành viên phụ trách: TV4 - Nguyễn Tuấn Khang
import com.laptopshop.dto.CustomerDTO;
import com.laptopshop.entity.Customer;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return customerRepository.searchCustomers(kw, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + id));
    }

    @Override
    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        dto.setId(null);
        return saveCustomer(dto);
    }

    @Override
    @Transactional
    public Customer updateCustomer(Integer id, CustomerDTO dto) {
        dto.setId(id);
        return saveCustomer(dto);
    }

    @Override
    @Transactional
    public Customer saveCustomer(CustomerDTO dto) {
        String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : null;
        String phone = dto.getPhone().trim();

        if (dto.getId() == null) {
            if (email != null && customerRepository.existsByEmailIgnoreCase(email)) {
                throw new BusinessException("Email '" + email + "' đã được đăng ký");
            }
            if (customerRepository.existsByPhone(phone)) {
                throw new BusinessException("Số điện thoại '" + phone + "' đã được đăng ký");
            }
        } else {
            if (email != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, dto.getId())) {
                throw new BusinessException("Email '" + email + "' đã được sử dụng bởi khách hàng khác");
            }
            if (customerRepository.existsByPhoneAndIdNot(phone, dto.getId())) {
                throw new BusinessException("Số điện thoại '" + phone + "' đã được sử dụng bởi khách hàng khác");
            }
        }

        Customer customer = (dto.getId() != null) ? getCustomerById(dto.getId()) : new Customer();
        customer.setCustomerName(dto.getCustomerName().trim());
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(dto.getAddress().trim());
        customer.setAccountId(dto.getAccountId());

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Integer id) {
        Customer customer = getCustomerById(id);
        if (orderRepository.countByCustomerId(id) > 0) {
            throw new BusinessException("Không thể xóa khách hàng đã có lịch sử đơn hàng");
        }
        customerRepository.delete(customer);
    }

    @Override
    @Transactional
    public Customer findOrCreateForCheckout(Integer accountId, String name, String email, String phone, String address) {
        if (accountId != null) {
            Optional<Customer> byAccount = customerRepository.findByAccountId(accountId);
            if (byAccount.isPresent()) {
                return byAccount.get();
            }
        }
        String cleanPhone = phone != null ? phone.trim() : "";
        if (!cleanPhone.isEmpty()) {
            Optional<Customer> byPhone = customerRepository.findByPhone(cleanPhone);
            if (byPhone.isPresent()) {
                Customer existing = byPhone.get();
                if (accountId != null && existing.getAccountId() == null) {
                    existing.setAccountId(accountId);
                    return customerRepository.save(existing);
                }
                return existing;
            }
        }
        Customer c = new Customer();
        c.setAccountId(accountId);
        c.setCustomerName(name.trim());
        c.setEmail((email != null && !email.isBlank()) ? email.trim() : null);
        c.setPhone(cleanPhone);
        c.setAddress(address != null ? address.trim() : "");
        return customerRepository.save(c);
    }
}
