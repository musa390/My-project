package com.luxeperfume.backend.integration.email;
public record OrderStatusEmailEvent(String recipientEmail,String orderNumber,String status) { }
