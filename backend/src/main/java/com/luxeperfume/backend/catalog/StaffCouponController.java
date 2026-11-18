package com.luxeperfume.backend.catalog;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/coupons")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_promotions.write')")
public class StaffCouponController {
    private final JdbcTemplate jdbc;
    public StaffCouponController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public ApiResponse<Map<String,Object>> list(@RequestParam(required=false) String q,
            @RequestParam(required=false) Boolean active,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1 ");List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where.append(" AND (code LIKE ? OR description LIKE ?) ");String term="%"+q.trim()+"%";args.add(term);args.add(term);}
        if(active!=null){where.append(" AND is_active=? ");args.add(active);}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM coupons"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT * FROM coupons"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);result.put("totalElements",total);
        result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Coupons retrieved",result);
    }

    @PostMapping
    public ApiResponse<Map<String,Object>> create(@Valid @RequestBody CouponRequest request){
        validate(request);
        String code=request.code().trim().toUpperCase();
        jdbc.update("""
                INSERT INTO coupons(code,description,discount_type,discount_value,minimum_order_amount,
                    maximum_discount_amount,usage_limit,per_user_limit,starts_at,ends_at,is_active)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """,code,blankToNull(request.description()),request.discountType().toUpperCase(),request.discountValue(),
                request.minimumOrderAmount()==null?BigDecimal.ZERO:request.minimumOrderAmount(),
                request.maximumDiscountAmount(),request.usageLimit(),request.perUserLimit(),request.startsAt(),
                request.endsAt(),request.active());
        Long id=jdbc.queryForObject("SELECT id FROM coupons WHERE code=?",Long.class,code);
        return ApiResponse.success("Coupon created",find(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String,Object>> update(@PathVariable long id,@Valid @RequestBody CouponRequest request){
        validate(request);find(id);
        String code=request.code().trim().toUpperCase();
        jdbc.update("""
                UPDATE coupons SET code=?,description=?,discount_type=?,discount_value=?,minimum_order_amount=?,
                    maximum_discount_amount=?,usage_limit=?,per_user_limit=?,starts_at=?,ends_at=?,is_active=?
                WHERE id=?
                """,code,blankToNull(request.description()),request.discountType().toUpperCase(),request.discountValue(),
                request.minimumOrderAmount()==null?BigDecimal.ZERO:request.minimumOrderAmount(),
                request.maximumDiscountAmount(),request.usageLimit(),request.perUserLimit(),request.startsAt(),
                request.endsAt(),request.active(),id);
        return ApiResponse.success("Coupon updated",find(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String,Object>> deactivate(@PathVariable long id){
        find(id);jdbc.update("UPDATE coupons SET is_active=FALSE WHERE id=?",id);
        return ApiResponse.success("Coupon deactivated",Map.of("id",id,"active",false));
    }

    private void validate(CouponRequest request){
        String type=request.discountType().toUpperCase();
        if("PERCENTAGE".equals(type)&&request.discountValue().compareTo(new BigDecimal("100"))>0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Percentage discount cannot exceed 100");
        if(request.maximumDiscountAmount()!=null&&request.maximumDiscountAmount().signum()<=0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Maximum discount must be positive");
        if(request.endsAt()!=null&&request.startsAt()!=null&&request.endsAt().isBefore(request.startsAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Coupon end time cannot be before start time");
        if(request.usageLimit()!=null&&request.perUserLimit()!=null&&request.perUserLimit()>request.usageLimit())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Per-user limit cannot exceed total usage limit");
    }
    private Map<String,Object> find(long id){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM coupons WHERE id=?",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Coupon not found");
        return rows.getFirst();
    }
    private static String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}

    public record CouponRequest(
            @NotBlank @Size(max=60) @Pattern(regexp="^[A-Za-z0-9_-]+$") String code,
            @Size(max=255) String description,
            @NotBlank @Pattern(regexp="PERCENTAGE|FIXED") String discountType,
            @DecimalMin("0.01") BigDecimal discountValue,
            @DecimalMin("0.00") BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscountAmount,
            Integer usageLimit,Integer perUserLimit,
            LocalDateTime startsAt,LocalDateTime endsAt,boolean active
    ){}
}
