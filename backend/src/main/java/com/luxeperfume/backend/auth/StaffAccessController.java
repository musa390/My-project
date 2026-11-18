package com.luxeperfume.backend.auth;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/access")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('PERM_staff.manage')")
public class StaffAccessController {
    private static final List<String> MANAGED_ROLES=List.of("ADMIN","MANAGER","INVENTORY_STAFF");
    private final JdbcTemplate jdbc;
    public StaffAccessController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping("/users")
    public ApiResponse<List<Map<String,Object>>> users(){
        List<Map<String,Object>> result=jdbc.queryForList("""
                SELECT u.id,u.email,u.first_name,u.last_name,u.account_status,u.created_at,
                       GROUP_CONCAT(r.code ORDER BY r.code) AS role_codes
                FROM users u JOIN user_roles ur ON ur.user_id=u.id
                JOIN roles r ON r.id=ur.role_id
                WHERE u.deleted_at IS NULL AND r.code IN ('ADMIN','MANAGER','INVENTORY_STAFF')
                GROUP BY u.id,u.email,u.first_name,u.last_name,u.account_status,u.created_at
                ORDER BY u.created_at DESC,u.id DESC
                """);
        return ApiResponse.success("Staff accounts retrieved",result);
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String,Object>>> roles(){
        List<Map<String,Object>> result=jdbc.queryForList("""
                SELECT r.id,r.code,r.name,r.description,
                       GROUP_CONCAT(p.code ORDER BY p.code) AS permission_codes
                FROM roles r LEFT JOIN role_permissions rp ON rp.role_id=r.id
                LEFT JOIN permissions p ON p.id=rp.permission_id
                WHERE r.code IN ('ADMIN','MANAGER','INVENTORY_STAFF')
                GROUP BY r.id,r.code,r.name,r.description ORDER BY r.code
                """);
        return ApiResponse.success("Staff roles retrieved",result);
    }

    @PatchMapping("/users/{userId}/roles")
    @Transactional
    public ApiResponse<Map<String,Object>> replaceRoles(Authentication authentication,@PathVariable long userId,
            @Valid @RequestBody RoleAssignmentRequest request){
        long actorId=actorId(authentication);
        if(actorId==userId)throw new ResponseStatusException(HttpStatus.CONFLICT,"You cannot change your own staff roles");
        Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=? AND deleted_at IS NULL AND account_status='ACTIVE'",Integer.class,userId);
        if(active==null||active==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active staff account not found");
        List<String> codes=request.roleCodes().stream().map(String::toUpperCase).distinct().toList();
        if(codes.stream().anyMatch(code->!MANAGED_ROLES.contains(code)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only ADMIN, MANAGER and INVENTORY_STAFF roles can be assigned here");
        List<Long> roleIds=jdbc.query("SELECT id FROM roles WHERE code IN ("+String.join(",",codes.stream().map(code->"?").toList())+")",
                (rs,row)->rs.getLong(1),codes.toArray());
        if(roleIds.size()!=codes.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"One or more roles do not exist");
        jdbc.update("DELETE ur FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=? AND r.code IN ('ADMIN','MANAGER','INVENTORY_STAFF')",userId);
        for(Long roleId:roleIds)jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)",userId,roleId);
        List<String> assigned=jdbc.query("""
                SELECT r.code FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                WHERE ur.user_id=? ORDER BY r.code
                """,(rs,row)->rs.getString(1),userId);
        Map<String,Object> result=new LinkedHashMap<>();result.put("userId",userId);result.put("roles",assigned);
        return ApiResponse.success("Staff roles updated",result);
    }

    private static long actorId(Authentication authentication){
        if(authentication==null||!(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return principal.longValue();
    }
    public record RoleAssignmentRequest(@NotEmpty @Size(max=3) List<@Pattern(regexp="ADMIN|MANAGER|INVENTORY_STAFF") String> roleCodes){}
}
