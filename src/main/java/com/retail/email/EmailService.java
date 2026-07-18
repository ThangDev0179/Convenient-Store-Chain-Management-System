package com.retail.email;

public interface EmailService {
    void sendCredentials(String email, String username, String tempPassword);
}
