package com.retail.service.impl;
import com.retail.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendCredentials(String email, String username, String tempPassword) {
        System.out.println("==================================================");
        System.out.println("EMAIL SENT SUCCESSFULLY (CONSOLE SIMULATED):");
        System.out.println("To: " + email);
        System.out.println("Username: " + username);
        System.out.println("Temporary Password: " + tempPassword);
        System.out.println("Message: Vui lòng đăng nhập và đổi mật khẩu ngay lập tức.");
        System.out.println("==================================================");

        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Thông tin tài khoản nhân viên mới");
                message.setText("Chào bạn,\n\nTài khoản của bạn đã được khởi tạo thành công trên hệ thống.\n\n"
                        + "Tên đăng nhập: " + username + "\n"
                        + "Mật khẩu tạm thời: " + tempPassword + "\n\n"
                        + "Vui lòng đăng nhập và đổi mật khẩu ngay lập tức.");
                mailSender.send(message);
            }
        } catch (Exception e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
        }
    }

    @Override
    public void sendResetToken(String email, String token) {
        System.out.println("==================================================");
        System.out.println("PASSWORD RESET EMAIL SENT SUCCESSFULLY (CONSOLE SIMULATED):");
        System.out.println("To: " + email);
        System.out.println("Token: " + token);
        System.out.println("Link: http://localhost:8080/reset-password?token=" + token);
        System.out.println("==================================================");

        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Yêu cầu đặt lại mật khẩu");
                message.setText("Chào bạn,\n\nBạn đã gửi yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này.\n"
                        + "Mã token đặt lại mật khẩu: " + token + "\n"
                        + "Vui lòng truy cập đường dẫn sau để đặt lại mật khẩu:\n"
                        + "http://localhost:8080/reset-password?token=" + token + "\n\n"
                        + "Liên kết này sẽ hết hạn trong vòng 15 phút.");
                mailSender.send(message);
            }
        } catch (Exception e) {
            System.err.println("Gửi email đặt lại mật khẩu thất bại: " + e.getMessage());
        }
    }
}