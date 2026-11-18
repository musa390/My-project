package com.luxeperfume.backend.integration.email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetEmailListener {
    private static final Logger log=LoggerFactory.getLogger(PasswordResetEmailListener.class);
    private final ResendEmailService email;
    public PasswordResetEmailListener(ResendEmailService email){this.email=email;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetEmailEvent event){
        try{
            email.sendText(event.recipientEmail(),"Reset your LUXE PERFUME password",
                    "Use this one-time link within 30 minutes: "+event.resetLink());
        }catch(RuntimeException ex){
            // Never log the reset URL or token. A provider outage cannot change the generic forgot-password response.
            log.error("Password reset email delivery failed after the reset token was committed.");
        }
    }
}
