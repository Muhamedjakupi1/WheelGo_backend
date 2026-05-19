package com.wheelGo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class InvoiceEmailJobService {

    @Async
    public void sendInvoiceEmail(String invoiceNumber) {
        System.out.println("Email is sent for " + invoiceNumber);
    }
}
