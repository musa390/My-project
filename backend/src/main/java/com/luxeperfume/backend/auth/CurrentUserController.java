package com.luxeperfume.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Returns the minimal authenticated profile needed by the customer and admin frontends. */
@RestController
public class CurrentUserController {
    private final JdbcTemplate jdbc;

    public CurrentUserController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/auth/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        long userId = principal.longValue();
        List<Map<String, Object>> users = jdbc.queryForList("""
                SELECT id, email, first_name, last_name
                FROM users
                WHERE id = ? AND account_status = 'ACTIVE'
                """, userId);
        if (users.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Map<String, Object> row = users.getFirst();
        List<String> roles = jdbc.query("""
                SELECT r.code
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                ORDER BY r.code
                """, (rs, index) -> rs.getString("code"), userId);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", ((Number) row.get("id")).longValue());
        user.put("email", row.get("email"));
        user.put("firstName", row.get("first_name"));
        user.put("lastName", row.get("last_name"));
        user.put("roles", roles);

        return Map.of("success", true, "user", user);
    }
}
