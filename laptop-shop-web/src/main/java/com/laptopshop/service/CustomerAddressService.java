package com.laptopshop.service;

import com.laptopshop.entity.Customer;
import com.laptopshop.entity.CustomerAddress;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import com.laptopshop.repository.CustomerAddressRepository;
import com.laptopshop.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    public CustomerAddressService(CustomerAddressRepository addressRepository, CustomerRepository customerRepository) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddress> getAddressesForCustomer(Integer customerId) {
        if (customerId == null) return List.of();
        return addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(customerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomerAddress addAddress(Integer customerId, String recipientName, String phone,
                                      String province, String district, String ward, String addressLine, boolean makeDefault) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng."));
        List<CustomerAddress> existing = addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(customerId);
        if (makeDefault || existing.isEmpty()) {
            existing.forEach(a -> { a.setIsDefault(false); addressRepository.save(a); });
        }
        CustomerAddress addr = new CustomerAddress();
        addr.setCustomer(customer);
        addr.setRecipientName(recipientName.trim());
        addr.setPhone(phone.trim());
        addr.setProvince(province != null ? province.trim() : "");
        addr.setDistrict(district != null ? district.trim() : "");
        addr.setWard(ward != null ? ward.trim() : "");
        addr.setAddressLine(addressLine.trim());
        addr.setIsDefault(makeDefault || existing.isEmpty());
        return addressRepository.save(addr);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Integer customerId, Integer addressId) {
        List<CustomerAddress> list = addressRepository.findByCustomer_IdOrderByIsDefaultDescIdDesc(customerId);
        boolean found = false;
        for (CustomerAddress a : list) {
            boolean isTarget = a.getId().equals(addressId);
            if (isTarget) found = true;
            a.setIsDefault(isTarget);
            addressRepository.save(a);
        }
        if (!found) throw new BusinessException("Không tìm thấy địa chỉ thuộc tài khoản của bạn.");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Integer customerId, Integer addressId) {
        CustomerAddress addr = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ."));
        if (!addr.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền xóa địa chỉ này.");
        }
        addressRepository.delete(addr);
    }
}
