package com.retail.service;

public interface EmailService {
    void sendCredentials(String email, String username, String tempPassword);
}