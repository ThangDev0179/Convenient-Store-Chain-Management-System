package com.retail.service.impl;

import com.retail.service.EmailService;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendCredentials(String email, String username, String tempPassword) {
        System.out.println("==================================================");
        System.out.println("EMAIL SENT SUCCESSFULLY:");
        System.out.println("To: " + email);
        System.out.println("Username: " + username);
        System.out.println("Temporary Password: " + tempPassword);
        System.out.println("Message: Vui lòng đăng nhập và đổi mật khẩu ngay lập tức.");
        System.out.println("==================================================");
    }
}
