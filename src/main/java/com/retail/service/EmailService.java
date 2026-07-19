package com.retail.service;

public interface EmailService {
    void sendCredentials(String email, String username, String tempPassword);
    void sendResetToken(String email, String token);
}