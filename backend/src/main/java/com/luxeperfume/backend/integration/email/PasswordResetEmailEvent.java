package com.luxeperfume.backend.integration.email;
public record PasswordResetEmailEvent(String recipientEmail,String resetLink) { }
