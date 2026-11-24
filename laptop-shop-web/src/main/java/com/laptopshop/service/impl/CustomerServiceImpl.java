package com.laptopshop.service.impl;

import com.laptopshop.dto.CustomerDTO;
import com.laptopshop.entity.Customer;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CustomerRepository;
import com.laptopshop.repository.OrderRepository;
import com.laptopshop.service.CustomerService;
import com.laptopshop.util.VietnameseTextNormalizer;
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
        String asciiKw = kw == null ? "" : VietnameseTextNormalizer.normalize(kw);
        return customerRepository.searchCustomers(kw, asciiKw, pageable);
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
        String phone = dto.getPhone() != null ? dto.getPhone().trim() : "";
        String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : null;
        if (dto.getId() == null) {
            if (customerRepository.existsByPhone(phone)) {
                throw new BusinessException("Số điện thoại '" + phone + "' đã được sử dụng");
            }
            if (email != null && customerRepository.existsByEmailIgnoreCase(email)) {
                throw new BusinessException("Email '" + email + "' đã được sử dụng");
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
        if (dto.getId() == null) {
            customer.setAccountId(dto.getAccountId());
        }
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
        if (accountId == null) {
            throw new BusinessException("Bạn cần đăng nhập để đặt hàng.");
        }
        // Mỗi tài khoản chỉ dùng hồ sơ khách hàng của chính nó. KHÔNG gắn theo số điện thoại.
        Optional<Customer> byAccount = customerRepository.findByAccountId(accountId);
        if (byAccount.isPresent()) {
            return byAccount.get();
        }
        Customer c = new Customer();
        c.setAccountId(accountId);
        c.setCustomerName(name.trim());
        c.setEmail((email != null && !email.isBlank()) ? email.trim() : null);
        c.setPhone(phone != null ? phone.trim() : "");
        c.setAddress(address != null ? address.trim() : "");
        return customerRepository.save(c);
    }
}
