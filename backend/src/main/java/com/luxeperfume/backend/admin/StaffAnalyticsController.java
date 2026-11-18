package com.luxeperfume.backend.admin;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/analytics")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_dashboard.read')")
public class StaffAnalyticsController {
    private final JdbcTemplate jdbc;
    public StaffAnalyticsController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping("/overview")
    public ApiResponse<Map<String,Object>> overview(){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("activeProducts",count("SELECT COUNT(*) FROM products WHERE status='ACTIVE' AND deleted_at IS NULL"));
        result.put("totalOrders",count("SELECT COUNT(*) FROM customer_orders"));
        result.put("pendingOrders",count("SELECT COUNT(*) FROM customer_orders WHERE order_status IN ('PENDING','CONFIRMED','PROCESSING')"));
        result.put("activeCustomers",count("SELECT COUNT(*) FROM users WHERE account_status='ACTIVE' AND deleted_at IS NULL"));
        result.put("paidRevenue",amount("SELECT COALESCE(SUM(grand_total),0) FROM customer_orders WHERE payment_status='PAID' AND order_status NOT IN ('CANCELLED','RETURNED')"));
        result.put("lowStockVariants",count("SELECT COUNT(*) FROM inventory WHERE GREATEST(quantity_on_hand-quantity_reserved,0)<=reorder_level"));
        result.put("newContactMessages",count("SELECT COUNT(*) FROM contact_messages WHERE status='NEW'"));
        result.put("requestedReturns",count("SELECT COUNT(*) FROM return_requests WHERE return_status='REQUESTED'"));
        result.put("subscribedNewsletter",count("SELECT COUNT(*) FROM newsletter_subscribers WHERE subscription_status='SUBSCRIBED'"));
        result.put("pendingPayments",count("SELECT COUNT(*) FROM payments WHERE payment_status IN ('INITIATED','PENDING','AUTHORIZED')"));
        return ApiResponse.success("Store analytics overview retrieved",result);
    }
    private long count(String sql){Long value=jdbc.queryForObject(sql,Long.class);return value==null?0:value;}
    private BigDecimal amount(String sql){BigDecimal value=jdbc.queryForObject(sql,BigDecimal.class);return value==null?BigDecimal.ZERO:value;}
}
