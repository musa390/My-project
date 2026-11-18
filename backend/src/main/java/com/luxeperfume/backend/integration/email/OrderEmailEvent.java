package com.luxeperfume.backend.integration.email;
import java.math.BigDecimal;
public record OrderEmailEvent(long orderId, String orderNumber, String recipientEmail, String currency, BigDecimal grandTotal) { }
