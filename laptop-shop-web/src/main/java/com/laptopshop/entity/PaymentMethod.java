package com.laptopshop.entity;

// Thành viên phụ trách: TV4
// Enum định nghĩa các phương thức thanh toán khớp 100% với ràng buộc CK_Payments_Method trong QL_LAPTOP.sql.

public enum PaymentMethod {
    CASH("Tiền mặt / Khi nhận hàng (CASH)"),
    BANK_TRANSFER("Chuyển khoản ngân hàng (BANK_TRANSFER)"),
    CARD("Thẻ ngân hàng (CARD)"),
    E_WALLET("Ví điện tử (E_WALLET)");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
