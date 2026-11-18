package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {
    private final JdbcTemplate jdbc;

    public CustomerAddressController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(Authentication authentication) {
        long userId = customerId(authentication);
        return ApiResponse.success("Delivery addresses retrieved", jdbc.queryForList("""
                SELECT id,label,recipient_name,phone,address_line1,address_line2,area,city,district,
                       postal_code,country_code,is_default,created_at,updated_at
                FROM addresses WHERE user_id=? ORDER BY is_default DESC,updated_at DESC,id DESC
                """, userId));
    }

    @PostMapping
    public ApiResponse<Map<String,Object>> create(Authentication authentication,
                                                    @Valid @RequestBody AddressRequest request) {
        long userId = customerId(authentication);
        if (request.makeDefault()) {
            jdbc.update("UPDATE addresses SET is_default=FALSE WHERE user_id=?", userId);
        }
        jdbc.update("""
                INSERT INTO addresses(user_id,label,recipient_name,phone,address_line1,address_line2,
                    area,city,district,postal_code,country_code,is_default)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """, userId, request.label(), request.recipientName().trim(), request.phone().trim(),
                request.addressLine1().trim(), blankToNull(request.addressLine2()), blankToNull(request.area()),
                request.city().trim(), blankToNull(request.district()), blankToNull(request.postalCode()),
                request.countryCode().toUpperCase(), request.makeDefault());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return ApiResponse.success("Delivery address created", findOwned(userId, id));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<Map<String,Object>> update(Authentication authentication,
            @PathVariable long addressId, @Valid @RequestBody AddressRequest request) {
        long userId = customerId(authentication);
        findOwned(userId, addressId);
        if (request.makeDefault()) jdbc.update("UPDATE addresses SET is_default=FALSE WHERE user_id=?", userId);
        int changed = jdbc.update("""
                UPDATE addresses SET label=?,recipient_name=?,phone=?,address_line1=?,address_line2=?,
                    area=?,city=?,district=?,postal_code=?,country_code=?,is_default=?
                WHERE id=? AND user_id=?
                """, request.label(), request.recipientName().trim(), request.phone().trim(),
                request.addressLine1().trim(), blankToNull(request.addressLine2()), blankToNull(request.area()),
                request.city().trim(), blankToNull(request.district()), blankToNull(request.postalCode()),
                request.countryCode().toUpperCase(), request.makeDefault(), addressId, userId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");
        return ApiResponse.success("Delivery address updated", findOwned(userId, addressId));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Map<String,Object>> delete(Authentication authentication, @PathVariable long addressId) {
        long userId = customerId(authentication);
        int changed = jdbc.update("DELETE FROM addresses WHERE id=? AND user_id=?", addressId, userId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");
        return ApiResponse.success("Delivery address deleted", Map.of("id", addressId));
    }

    @PatchMapping("/{addressId}/default")
    public ApiResponse<Map<String,Object>> makeDefault(Authentication authentication, @PathVariable long addressId) {
        long userId = customerId(authentication);
        findOwned(userId, addressId);
        jdbc.update("UPDATE addresses SET is_default=FALSE WHERE user_id=?", userId);
        jdbc.update("UPDATE addresses SET is_default=TRUE WHERE id=? AND user_id=?", addressId, userId);
        return ApiResponse.success("Default delivery address updated", findOwned(userId, addressId));
    }

    private Map<String,Object> findOwned(long userId, long addressId) {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT id,label,recipient_name,phone,address_line1,address_line2,area,city,district,
                       postal_code,country_code,is_default,created_at,updated_at
                FROM addresses WHERE id=? AND user_id=?
                """, addressId, userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");
        return rows.getFirst();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static long customerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return principal.longValue();
    }

    public record AddressRequest(
            @Size(max=50) String label,
            @NotBlank @Size(min=2,max=150) String recipientName,
            @NotBlank @Pattern(regexp="^[0-9+() .-]{7,30}$") String phone,
            @NotBlank @Size(min=8,max=255) String addressLine1,
            @Size(max=255) String addressLine2,
            @Size(max=120) String area,
            @NotBlank @Size(min=2,max=120) String city,
            @Size(max=120) String district,
            @Size(max=20) String postalCode,
            @NotBlank @Pattern(regexp="^[A-Za-z]{2}$") String countryCode,
            boolean makeDefault
    ) {}
}
