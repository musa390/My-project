package com.luxeperfume.backend.auth;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/customers")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_customers.read')")
public class StaffCustomerController {
    private final JdbcTemplate jdbc;
    public StaffCustomerController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public ApiResponse<Map<String,Object>> search(@RequestParam(required=false) String q,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        int safePage=Math.max(0,page), safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE u.deleted_at IS NULL ");
        List<Object> args=new ArrayList<>();
        if(status!=null&&!status.isBlank()){where.append(" AND u.account_status=? ");args.add(status.trim().toUpperCase());}
        if(q!=null&&!q.isBlank()){
            where.append(" AND (u.email LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ? OR u.phone LIKE ?) ");
            String term="%"+q.trim()+"%";args.add(term);args.add(term);args.add(term);args.add(term);
        }
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM users u"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("""
                SELECT u.id,u.email,u.first_name,u.last_name,u.phone,u.account_status,u.auth_provider,
                       u.email_verified_at,u.last_login_at,u.created_at,
                       (SELECT COUNT(*) FROM customer_orders o WHERE o.user_id=u.id) AS order_count,
                       (SELECT COALESCE(SUM(o.grand_total),0) FROM customer_orders o
                         WHERE o.user_id=u.id AND o.order_status NOT IN ('CANCELLED','RETURNED')) AS lifetime_value
                FROM users u
                """+where+" ORDER BY u.created_at DESC,u.id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);
        result.put("totalElements",total);result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Customers retrieved",result);
    }

    @GetMapping("/{customerId}")
    public ApiResponse<Map<String,Object>> detail(@PathVariable long customerId) {
        List<Map<String,Object>> users=jdbc.queryForList("""
                SELECT id,email,first_name,last_name,phone,account_status,auth_provider,
                       email_verified_at,last_login_at,created_at,updated_at
                FROM users WHERE id=? AND deleted_at IS NULL
                """,customerId);
        if(users.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found");
        Map<String,Object> result=new LinkedHashMap<>(users.getFirst());
        result.put("addresses",jdbc.queryForList("""
                SELECT id,label,recipient_name,phone,address_line1,address_line2,area,city,district,
                       postal_code,country_code,is_default FROM addresses WHERE user_id=?
                """,customerId));
        result.put("recentOrders",jdbc.queryForList("""
                SELECT order_number,order_status,payment_status,grand_total,placed_at
                FROM customer_orders WHERE user_id=? ORDER BY created_at DESC LIMIT 10
                """,customerId));
        return ApiResponse.success("Customer details retrieved",result);
    }

    @PatchMapping("/{customerId}/status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERM_staff.manage')")
    public ApiResponse<Map<String,Object>> updateStatus(Authentication authentication,@PathVariable long customerId,
            @RequestBody StatusRequest request) {
        long actorId=actorId(authentication);
        if(actorId==customerId)throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot change your own account status here");
        List<Map<String,Object>> existing=jdbc.queryForList(
                "SELECT account_status FROM users WHERE id=? AND deleted_at IS NULL",customerId);
        if(existing.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Customer not found");
        String target=request.status().toUpperCase();
        String current=String.valueOf(existing.getFirst().get("account_status"));
        if(!current.equals(target))jdbc.update("UPDATE users SET account_status=? WHERE id=? AND deleted_at IS NULL",target,customerId);
        return ApiResponse.success("Customer status updated",Map.of("id",customerId,"status",target));
    }

    private static long actorId(Authentication authentication){
        if(authentication==null||!(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return principal.longValue();
    }
    public record StatusRequest(@NotBlank @Pattern(regexp="ACTIVE|SUSPENDED") String status){}
}
