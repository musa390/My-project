package com.luxeperfume.backend.contact;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/contact-messages")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ContactAdminController {
    private final JdbcTemplate jdbc;
    public ContactAdminController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public ApiResponse<Map<String,Object>> list(@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) @Pattern(regexp="NEW|IN_PROGRESS|RESOLVED|SPAM") String status){
        String where=status==null?"":" WHERE status=?";
        List<Map<String,Object>> rows=status==null
                ?jdbc.queryForList("SELECT id,full_name,email,subject,message,status,created_at FROM contact_messages ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",size,(long)page*size)
                :jdbc.queryForList("SELECT id,full_name,email,subject,message,status,created_at FROM contact_messages"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",status,size,(long)page*size);
        return ApiResponse.success("Contact messages",Map.of("items",rows,"page",page,"size",size));
    }
    @PatchMapping("/{id}/status")
    public ApiResponse<Map<String,Object>> updateStatus(@PathVariable @Min(1) long id,
            @Valid @RequestBody StatusRequest request){
        int changed=jdbc.update("UPDATE contact_messages SET status=? WHERE id=?",request.status(),id);
        if(changed!=1) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Contact message not found");
        return ApiResponse.success("Contact message updated",Map.of("id",id,"status",request.status()));
    }
    public record StatusRequest(@NotBlank @Pattern(regexp="NEW|IN_PROGRESS|RESOLVED|SPAM") String status){}
}
