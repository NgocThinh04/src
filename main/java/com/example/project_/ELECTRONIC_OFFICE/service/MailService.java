package com.example.project_.ELECTRONIC_OFFICE.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email thông báo đăng ký thành công
     */
    public void sendRegistrationSuccessEmail(String toEmail, String companyCode, String companyName, String adminName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Đăng ký thành công - Electronic Office");

            String content = String.format(
                    "Kính gửi %s,\n\n" +
                            "Chúc mừng bạn đã đăng ký thành công tài khoản quản trị trên hệ thống Electronic Office!\n\n" +
                            "Thông tin công ty của bạn:\n" +
                            "• Tên công ty: %s\n" +
                            "• Mã công ty (Company Code): %s\n\n" +
                            "Vui lòng giữ mã công ty này để sử dụng khi cần hỗ trợ hoặc quản lý.\n\n" +
                            "Bạn có thể đăng nhập vào hệ thống với email: %s\n\n" +
                            "Trân trọng,\n" +
                            "Đội ngũ Electronic Office",
                    adminName, companyName, companyCode, toEmail
            );

            message.setText(content);
            mailSender.send(message);
            log.info("Registration email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send registration email to: {}", toEmail, e);
        }
    }

    /**
     * Gửi email thông báo mã công ty (có thể dùng riêng)
     */
    public void sendCompanyCodeEmail(String toEmail, String companyCode, String companyName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã công ty của bạn - Electronic Office");

            String content = String.format(
                    "Kính gửi Quý khách,\n\n" +
                            "Mã công ty của bạn trên hệ thống Electronic Office là: %s\n\n" +
                            "Tên công ty: %s\n\n" +
                            "Vui lòng lưu lại mã này để sử dụng khi cần thiết.\n\n" +
                            "Trân trọng,\n" +
                            "Đội ngũ Electronic Office",
                    companyCode, companyName
            );

            message.setText(content);
            mailSender.send(message);
            log.info("Company code email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send company code email to: {}", toEmail, e);
        }
    }
}
