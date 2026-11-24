package com.laptopshop.service;

import com.laptopshop.config.ShopProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ gửi email thông báo (đơn hàng, đặt lại mật khẩu).
 * Ở môi trường DEV (mail.enabled=false) sẽ ghi log ra console; ở PROD gửi qua SMTP thật.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ShopProperties shopProperties;
    private final boolean mailEnabled;
    private final String fromAddress;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        ShopProperties shopProperties,
                        @Value("${laptop.mail.enabled:false}") boolean mailEnabled,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.shopProperties = shopProperties;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("[MAIL-DEV] To={} | Subject='{}' | Body={}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject("[" + shopProperties.getShop().getName() + "] " + subject);
            msg.setText(body);
            sender.send(msg);
            log.info("[MAIL-PROD] Đã gửi email '{}' tới {}", subject, to);
        } catch (Exception ex) {
            log.error("[MAIL-PROD] Lỗi gửi email tới {}: {}", to, ex.getMessage());
        }
    }
}
