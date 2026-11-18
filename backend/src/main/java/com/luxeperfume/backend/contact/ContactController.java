package com.luxeperfume.backend.contact;

import com.luxeperfume.backend.common.dto.ApiResponse;
import com.luxeperfume.backend.integration.email.ResendEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    private static final Logger log=LoggerFactory.getLogger(ContactController.class);
    private final JdbcTemplate jdbc; private final ResendEmailService email; private final String adminEmail;
    public ContactController(JdbcTemplate jdbc,ResendEmailService email,@Value("${app.contact.admin-email:}") String adminEmail) {
        this.jdbc=jdbc;this.email=email;this.adminEmail=adminEmail;
    }
    @PostMapping
    public ApiResponse<Map<String,Object>> submit(@Valid @RequestBody ContactRequest body,HttpServletRequest request) {
        String ip=request.getRemoteAddr();
        Integer recent=jdbc.queryForObject("""
            SELECT COUNT(*) FROM contact_messages WHERE source_ip=? AND created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 HOUR)
            """,Integer.class,ip);
        if(recent!=null&&recent>=5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many messages. Please try again later.");
        jdbc.update("INSERT INTO contact_messages(full_name,email,subject,message,source_ip) VALUES(?,?,?,?,?)",
                body.name().trim(),body.email().trim().toLowerCase(),body.subject().trim(),body.message().trim(),ip);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        List<Long> admins=jdbc.query("SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.code IN ('ADMIN','MANAGER')",(rs,row)->rs.getLong(1));
        for(Long adminId:admins) jdbc.update("""
            INSERT IGNORE INTO user_notifications(user_id,event_key,notification_type,aggregate_type,aggregate_id,title_key,message_key)
            VALUES(?,?,'CONTACT','CONTACT',?,'notification.contact.new.title','notification.contact.new.body')
            """,adminId,"contact-submitted:"+id,id);
        if(adminEmail!=null&&!adminEmail.isBlank()) {
            try { email.sendText(adminEmail,"New LUXE PERFUME contact message #"+id,
                    "From: "+body.name()+" <"+body.email()+">\nSubject: "+body.subject()+"\n\n"+body.message()); }
            catch(RuntimeException ex) { log.error("Contact alert email could not be delivered; the message remains stored for staff."); }
        }
        return ApiResponse.success("Your message has been received.",Map.of("accepted",true,"reference",id));
    }
    public record ContactRequest(@NotBlank @Size(max=120) String name,@Email @NotBlank @Size(max=254) String email,
            @NotBlank @Size(max=180) String subject,@NotBlank @Size(min=10,max=5000) String message){}
}
