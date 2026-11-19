package com.laptopshop.service;

import com.laptopshop.dto.CustomerDTO;
import com.laptopshop.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    Page<Customer> getCustomers(String keyword, Pageable pageable);
    List<Customer> getAllCustomers();
    Customer getCustomerById(Integer id);
    Customer createCustomer(CustomerDTO dto);
    Customer updateCustomer(Integer id, CustomerDTO dto);
    Customer saveCustomer(CustomerDTO dto);
    void deleteCustomer(Integer id);
}
