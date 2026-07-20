package com.retail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RetailApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public com.retail.mapper.InvoiceMapper invoiceMapper() {
        return new com.retail.mapper.InvoiceMapper();
    }
}