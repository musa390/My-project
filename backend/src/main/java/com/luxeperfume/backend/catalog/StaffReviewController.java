package com.luxeperfume.backend.catalog;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/staff/reviews")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_reviews.moderate')")
public class StaffReviewController {
    private final JdbcTemplate jdbc;
    public StaffReviewController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public ApiResponse<Map<String,Object>> list(@RequestParam(required=false) String q,
            @RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1 ");List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where.append(" AND (p.name LIKE ? OR u.email LIKE ? OR r.title LIKE ? OR r.body LIKE ?) ");String term="%"+q.trim()+"%";args.add(term);args.add(term);args.add(term);args.add(term);}
        if(status!=null&&!status.isBlank()){where.append(" AND r.moderation_status=? ");args.add(status.trim().toUpperCase());}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM reviews r JOIN products p ON p.id=r.product_id JOIN users u ON u.id=r.user_id"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("""
                SELECT r.id,r.product_id,p.name AS product_name,p.slug AS product_slug,r.user_id,
                       u.email AS customer_email,u.first_name,u.last_name,r.rating,r.title,r.body,
                       r.moderation_status,r.moderated_by,r.moderated_at,r.created_at
                FROM reviews r JOIN products p ON p.id=r.product_id JOIN users u ON u.id=r.user_id
                """+where+" ORDER BY r.created_at DESC,r.id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);result.put("totalElements",total);
        result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Product reviews retrieved",result);
    }

    @PatchMapping("/{id}/moderation")
    public ApiResponse<Map<String,Object>> moderate(Authentication authentication,@PathVariable long id,
            @Valid @RequestBody ModerationRequest request){
        long staffId=actorId(authentication);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,moderation_status FROM reviews WHERE id=?",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Review not found");
        String target=request.status().toUpperCase();
        jdbc.update("UPDATE reviews SET moderation_status=?,moderated_by=?,moderated_at=UTC_TIMESTAMP() WHERE id=?",
                target,staffId,id);
        return ApiResponse.success("Review moderation updated",Map.of("id",id,"status",target));
    }
    private static long actorId(Authentication authentication){
        if(authentication==null||!(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return principal.longValue();
    }
    public record ModerationRequest(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String status,
                                    @Size(max=500) String note){}
}
