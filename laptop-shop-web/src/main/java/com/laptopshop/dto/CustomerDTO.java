package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO dùng cho form thêm/sửa thông tin khách hàng (Customers) khớp với CSDL QL_Laptop của TV1.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerDTO {

    private Integer id;

    private Integer accountId;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 100, message = "Tên khách hàng tối đa 100 ký tự")
    private String fullName;

    @Email(message = "Địa chỉ email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Số điện thoại không hợp lệ (9-11 chữ số bắt đầu bằng 0 hoặc +84)")
    private String phone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String address;

    public CustomerDTO() {
    }

    public CustomerDTO(Integer id, String fullName, String email, String phone, String address) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public CustomerDTO(Integer id, String fullName, String email, String phone, String address, Integer accountId) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.accountId = accountId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return id;
    }

    public void setCustomerId(Integer customerId) {
        this.id = customerId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCustomerName() {
        return fullName;
    }

    public void setCustomerName(String customerName) {
        this.fullName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
