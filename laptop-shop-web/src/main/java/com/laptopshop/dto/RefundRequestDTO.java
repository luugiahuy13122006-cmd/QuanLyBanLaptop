package com.laptopshop.dto;

import jakarta.validation.constraints.*;

public class RefundRequestDTO {

    @NotBlank(message = "Vui lòng nhập lý do hủy/hoàn tiền")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;

    @NotBlank(message = "Vui lòng nhập tên ngân hàng nhận tiền")
    @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập số tài khoản")
    @Pattern(regexp = "^[0-9]{6,20}$", message = "Số tài khoản chỉ gồm 6–20 chữ số")
    private String accountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    @Size(max = 100, message = "Tên chủ tài khoản tối đa 100 ký tự")
    private String accountHolder;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
}
