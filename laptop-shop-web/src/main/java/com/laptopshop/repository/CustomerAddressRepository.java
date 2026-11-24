package com.laptopshop.repository;

import com.laptopshop.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {
    List<CustomerAddress> findByCustomer_IdOrderByIsDefaultDescIdDesc(Integer customerId);
}
