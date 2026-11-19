package com.laptopshop.service.impl;

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
        return customerRepository.searchCustomers(keyword, pageable);
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
}
