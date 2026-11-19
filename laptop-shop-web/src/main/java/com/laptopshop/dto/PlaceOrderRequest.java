package com.laptopshop.dto;

// Thành viên phụ trách: TV4
// DTO chứa toàn bộ thông tin khi tạo đơn hàng mới (Orders, OrderDetails, Payments) trong CSDL QL_Laptop.

import com.laptopshop.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PlaceOrderRequest {

    @NotNull(message = "Mã khách hàng (CustomerID) không được để trống")
    private Integer customerId;

    @NotNull(message = "Phương thức thanh toán (PaymentMethod) không được để trống")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    // Nếu không truyền recipientName / recipientPhone thì tự động lấy từ thông tin Customer
    private String recipientName;

    private String recipientPhone;

    @NotBlank(message = "Địa chỉ giao hàng (DeliveryAddress) không được để trống")
    private String shippingAddress;

    private BigDecimal discount = BigDecimal.ZERO;

    private BigDecimal shippingFee = new BigDecimal("30000");

    private String note;

    @NotEmpty(message = "Đơn hàng phải chứa ít nhất 1 sản phẩm laptop")
    @Valid
    private List<OrderItemRequest> items = new ArrayList<>();

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(Integer customerId, PaymentMethod paymentMethod, String shippingAddress, String note, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.items = items;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getDeliveryAddress() {
        return shippingAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.shippingAddress = deliveryAddress;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
