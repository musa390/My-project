package com.luxeperfume.backend.newsletter;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/newsletter")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_newsletter.manage')")
public class StaffNewsletterController {
    private final JdbcTemplate jdbc;
    public StaffNewsletterController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping("/subscribers")
    public ApiResponse<Map<String,Object>> list(@RequestParam(required=false) String q,
            @RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1 ");List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where.append(" AND email LIKE ? ");args.add("%"+q.trim()+"%");}
        if(status!=null&&!status.isBlank()){where.append(" AND subscription_status=? ");args.add(status.trim().toUpperCase());}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM newsletter_subscribers"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("""
                SELECT id,email,subscription_status,consented_at,confirmed_at,unsubscribed_at,source,created_at,updated_at
                FROM newsletter_subscribers
                """+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);result.put("totalElements",total);
        result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Newsletter subscribers retrieved",result);
    }

    @PostMapping("/subscribers/{id}/unsubscribe")
    public ApiResponse<Map<String,Object>> unsubscribe(@PathVariable long id){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,email,subscription_status FROM newsletter_subscribers WHERE id=?",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscriber not found");
        jdbc.update("""
                UPDATE newsletter_subscribers SET subscription_status='UNSUBSCRIBED',
                    unsubscribed_at=UTC_TIMESTAMP(),unsubscribe_token_hash=NULL
                WHERE id=? AND subscription_status<>'UNSUBSCRIBED'
                """,id);
        return ApiResponse.success("Subscriber unsubscribed",Map.of("id",id,"email",rows.getFirst().get("email"),"status","UNSUBSCRIBED"));
    }
}
