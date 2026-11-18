package com.luxeperfume.backend.payment;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/payments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.read')")
public class StaffPaymentQueryController {
    private final JdbcTemplate jdbc;
    public StaffPaymentQueryController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public ApiResponse<Map<String,Object>> search(@RequestParam(required=false) String status,
            @RequestParam(required=false) String q,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1 ");
        List<Object> args=new ArrayList<>();
        if(status!=null&&!status.isBlank()){where.append(" AND p.payment_status=? ");args.add(status.trim().toUpperCase());}
        if(q!=null&&!q.isBlank()){
            where.append(" AND (o.order_number LIKE ? OR p.provider_transaction_id LIKE ? OR p.provider_reference LIKE ?) ");
            String term="%"+q.trim()+"%";args.add(term);args.add(term);args.add(term);
        }
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM payments p JOIN customer_orders o ON o.id=p.order_id"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("""
                SELECT p.id AS payment_id,p.order_id,o.order_number,o.user_id,o.customer_email,
                       p.provider,p.payment_method,p.provider_transaction_id,p.provider_reference,
                       p.amount,p.currency,p.payment_status,p.initiated_at,p.completed_at,p.created_at
                FROM payments p JOIN customer_orders o ON o.id=p.order_id
                """+where+" ORDER BY p.created_at DESC,p.id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);
        result.put("totalElements",total);result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Payments retrieved",result);
    }
}
