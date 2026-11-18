package com.luxeperfume.backend.catalog;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/banners")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_promotions.write')")
public class StaffBannerController {
    private final JdbcTemplate jdbc;
    public StaffBannerController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public ApiResponse<Map<String,Object>> list(@RequestParam(required=false) String q,
            @RequestParam(required=false) Boolean active,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1 ");List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where.append(" AND (title LIKE ? OR subtitle LIKE ?) ");String term="%"+q.trim()+"%";args.add(term);args.add(term);}
        if(active!=null){where.append(" AND is_active=? ");args.add(active);}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM banners"+where,Integer.class,args.toArray());
        List<Object> pageArgs=new ArrayList<>(args);pageArgs.add(safeSize);pageArgs.add((long)safePage*safeSize);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT * FROM banners"+where+" ORDER BY sort_order,id DESC LIMIT ? OFFSET ?",pageArgs.toArray());
        long total=count==null?0:count;Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",items);result.put("page",safePage);result.put("size",safeSize);result.put("totalElements",total);
        result.put("totalPages",total==0?0:(total+safeSize-1)/safeSize);
        return ApiResponse.success("Banners retrieved",result);
    }

    @PostMapping
    public ApiResponse<Map<String,Object>> create(@Valid @RequestBody BannerRequest request){
        validateDates(request);
        jdbc.update("""
                INSERT INTO banners(title,subtitle,image_url,target_url,placement,starts_at,ends_at,sort_order,is_active)
                VALUES(?,?,?,?,?,?,?,?,?)
                """,request.title().trim(),blank(request.subtitle()),request.imageUrl().trim(),blank(request.targetUrl()),
                request.placement().toUpperCase(),request.startsAt(),request.endsAt(),request.sortOrder(),request.active());
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        return ApiResponse.success("Banner created",find(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String,Object>> update(@PathVariable long id,@Valid @RequestBody BannerRequest request){
        validateDates(request);find(id);
        jdbc.update("""
                UPDATE banners SET title=?,subtitle=?,image_url=?,target_url=?,placement=?,starts_at=?,ends_at=?,
                    sort_order=?,is_active=? WHERE id=?
                """,request.title().trim(),blank(request.subtitle()),request.imageUrl().trim(),blank(request.targetUrl()),
                request.placement().toUpperCase(),request.startsAt(),request.endsAt(),request.sortOrder(),request.active(),id);
        return ApiResponse.success("Banner updated",find(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String,Object>> deactivate(@PathVariable long id){
        find(id);jdbc.update("UPDATE banners SET is_active=FALSE WHERE id=?",id);
        return ApiResponse.success("Banner deactivated",Map.of("id",id,"active",false));
    }

    private void validateDates(BannerRequest request){
        if(request.endsAt()!=null&&request.startsAt()!=null&&request.endsAt().isBefore(request.startsAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Banner end time cannot be before start time");
    }
    private Map<String,Object> find(long id){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM banners WHERE id=?",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Banner not found");
        return rows.getFirst();
    }
    private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    public record BannerRequest(
            @NotBlank @Size(max=180) String title,@Size(max=255) String subtitle,
            @NotBlank @Size(max=1000) @Pattern(regexp="https://.+") String imageUrl,
            @Size(max=1000) @Pattern(regexp="^(https://[^\\s]+|/[^/].*)$") String targetUrl,@NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z_]+") String placement,
            LocalDateTime startsAt,LocalDateTime endsAt,int sortOrder,boolean active
    ){}
}
