package com.laptopshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "laptop")
public class ShopProperties {

    private final ShopInfo shop = new ShopInfo();
    private final PaymentConfig payment = new PaymentConfig();
    private final UploadConfig upload = new UploadConfig();

    public ShopInfo getShop() { return shop; }
    public PaymentConfig getPayment() { return payment; }
    public UploadConfig getUpload() { return upload; }

    public static class ShopInfo {
        private String name = "LAPTOP SHOP";
        private String address = "TP. Hồ Chí Minh";
        private String phone = "0900000000";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class PaymentConfig {
        private String bankBin = "970422";
        private String bankName = "MB Bank";
        private String accountNumber = "0123456789";
        private String accountName = "CUA HANG LAPTOP SHOP";
        private int expireMinutes = 30;
        public String getBankBin() { return bankBin; }
        public void setBankBin(String bankBin) { this.bankBin = bankBin; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public int getExpireMinutes() { return expireMinutes; }
        public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
    }

    public static class UploadConfig {
        private String paymentProofDir = "uploads/payment-proofs";
        private long maxProofSizeBytes = 5L * 1024 * 1024;
        public String getPaymentProofDir() { return paymentProofDir; }
        public void setPaymentProofDir(String paymentProofDir) { this.paymentProofDir = paymentProofDir; }
        public long getMaxProofSizeBytes() { return maxProofSizeBytes; }
        public void setMaxProofSizeBytes(long maxProofSizeBytes) { this.maxProofSizeBytes = maxProofSizeBytes; }
    }
}
