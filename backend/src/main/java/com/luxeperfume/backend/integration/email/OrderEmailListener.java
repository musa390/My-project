package com.luxeperfume.backend.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEmailListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);
    private final ResendEmailService email;
    private final String storefrontUrl;
    public OrderEmailListener(ResendEmailService email,
            @Value("${app.storefront-url:http://localhost:3000}") String storefrontUrl) {
        this.email=email; this.storefrontUrl=storefrontUrl.replaceAll("/+$","");
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusEmailEvent event) {
        if (!email.isConfigured()) return;
        try {
            email.sendText(event.recipientEmail(), "LUXE PERFUME order update — " + event.orderNumber(),
                    "Your order " + event.orderNumber() + " status is now " + event.status() + ".");
        } catch (RuntimeException ex) {
            log.error("Order status email failed for order {}. The committed order update is unaffected.",
                    event.orderNumber());
        }
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderEmailEvent event) {
        if (!email.isConfigured()) return;
        String subject="LUXE PERFUME order confirmation — "+event.orderNumber();
        String text="Thank you for your order.\nOrder: "+event.orderNumber()
                +"\nTotal: "+event.currency()+" "+event.grandTotal()
                +"\nView your account: "+storefrontUrl+"/account/orders";
        try { email.sendText(event.recipientEmail(),subject,text); }
        catch(RuntimeException ex) {
            log.error("Order confirmation email failed for order {}. The committed order is unaffected.",event.orderNumber());
        }
    }
}
