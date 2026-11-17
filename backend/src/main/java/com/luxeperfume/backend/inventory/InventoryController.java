package com.luxeperfume.backend.inventory;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@Validated
public class InventoryController {
    private final JdbcTemplate jdbc;

    public InventoryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/staff/inventory/low-stock")
    @PreAuthorize("hasAuthority('PERM_inventory.read')")
    public ApiResponse<List<Map<String,Object>>> lowStock() {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT pv.id AS variant_id,pv.sku,pv.variant_name,pv.price,
                       p.id AS product_id,p.name AS product_name,p.slug,
                       COALESCE(i.quantity_on_hand,0) AS quantity_on_hand,
                       COALESCE(i.quantity_reserved,0) AS quantity_reserved,
                       GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) AS available_quantity,
                       COALESCE(i.reorder_level,0) AS reorder_level,
                       CASE WHEN i.variant_id IS NULL THEN TRUE ELSE FALSE END AS inventory_record_missing
                FROM product_variants pv
                JOIN products p ON p.id=pv.product_id
                LEFT JOIN inventory i ON i.variant_id=pv.id
                WHERE pv.is_active=TRUE AND p.status='ACTIVE' AND p.deleted_at IS NULL
                  AND GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) <= COALESCE(i.reorder_level,0)
                ORDER BY available_quantity ASC,p.name,pv.variant_name
                """);
        return ApiResponse.success("Low stock inventory", rows);
    }

    @GetMapping("/api/v1/staff/inventory/variants/{variantId}")
    @PreAuthorize("hasAuthority('PERM_inventory.read')")
    public ApiResponse<Map<String,Object>> inventory(@PathVariable long variantId) {
        return ApiResponse.success("Inventory detail", one("""
                SELECT pv.id AS variant_id,pv.sku,pv.variant_name,pv.is_active,
                       p.id AS product_id,p.name AS product_name,p.status AS product_status,
                       COALESCE(i.quantity_on_hand,0) AS quantity_on_hand,
                       COALESCE(i.quantity_reserved,0) AS quantity_reserved,
                       GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) AS available_quantity,
                       COALESCE(i.reorder_level,0) AS reorder_level
                FROM product_variants pv JOIN products p ON p.id=pv.product_id
                LEFT JOIN inventory i ON i.variant_id=pv.id WHERE pv.id=?
                """, variantId));
    }

    @GetMapping("/api/v1/staff/inventory/variants/{variantId}/movements")
    @PreAuthorize("hasAuthority('PERM_inventory.read')")
    public ApiResponse<List<Map<String,Object>>> movements(@PathVariable long variantId) {
        one("SELECT id FROM product_variants WHERE id=?", variantId);
        return ApiResponse.success("Inventory movement history", jdbc.queryForList(
                "SELECT id,variant_id,movement_type,quantity_change,reference_type,reference_id,note,created_at FROM inventory_movements WHERE variant_id=? ORDER BY created_at DESC,id DESC LIMIT 100",
                variantId));
    }

    @PostMapping("/api/v1/staff/inventory/variants/{variantId}/adjustments")
    @PreAuthorize("hasAuthority('PERM_inventory.write')")
    @Transactional
    public ApiResponse<Map<String,Object>> adjustStock(@PathVariable long variantId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        one("SELECT id FROM product_variants WHERE id=? FOR UPDATE", variantId);
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT quantity_on_hand,quantity_reserved,reorder_level FROM inventory WHERE variant_id=? FOR UPDATE",
                variantId);
        if (rows.isEmpty()) {
            jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,0,0,0)", variantId);
            rows = jdbc.queryForList(
                    "SELECT quantity_on_hand,quantity_reserved,reorder_level FROM inventory WHERE variant_id=? FOR UPDATE",
                    variantId);
        }
        Map<String,Object> current = rows.getFirst();
        long onHand = ((Number) current.get("quantity_on_hand")).longValue();
        long reserved = ((Number) current.get("quantity_reserved")).longValue();
        long next = onHand + request.quantityChange();
        if (next < reserved) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Adjustment cannot reduce stock below the reserved quantity (" + reserved + ")");
        if (next < 0 || next > 4_294_967_295L)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjusted stock is outside the supported range");

        jdbc.update("UPDATE inventory SET quantity_on_hand=? WHERE variant_id=?", next, variantId);
        jdbc.update("""
                INSERT INTO inventory_movements(variant_id,movement_type,quantity_change,reference_type,note)
                VALUES(?,'ADJUSTMENT',?,'MANUAL_ADJUSTMENT',?)
                """, variantId, request.quantityChange(), request.note().trim());
        return ApiResponse.success("Stock adjusted", one("""
                SELECT variant_id,quantity_on_hand,quantity_reserved,
                       GREATEST(quantity_on_hand-quantity_reserved,0) AS available_quantity,
                       reorder_level,updated_at
                FROM inventory WHERE variant_id=?
                """, variantId));
    }

    private Map<String,Object> one(String sql, Object... args) {
        List<Map<String,Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory resource not found");
        return rows.getFirst();
    }

    public record StockAdjustmentRequest(@NotNull Integer quantityChange, @NotBlank String note) {}
}
