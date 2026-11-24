package com.laptopshop.dto;

// Thành viên phụ trách: TV3 - Trương Xuân Đạt
// DTO hứng dữ liệu form đặt hàng trên giao diện Website khách hàng (có Bean Validation).

import com.laptopshop.entity.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutFormDTO {

    @NotBlank(message = "Vui lòng nhập họ tên người nhận")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Vui lòng nhập số điện thoại nhận hàng")
    @Pattern(regexp = "^(0|\\+84)[0-9]{8,10}$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String recipientPhone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String deliveryAddress;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
