package com.luxeperfume.backend.returns;

import com.luxeperfume.backend.common.dto.ApiResponse;
import com.luxeperfume.backend.returns.dto.ReturnDecisionPayload;
import com.luxeperfume.backend.returns.dto.ReturnRequestPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Customer-owned return requests and permission-protected staff review. */
@RestController
@Validated
@RequestMapping("/api/v1")
public class ReturnRequestController {
    private final ReturnRequestService returns;

    public ReturnRequestController(ReturnRequestService returns) {
        this.returns = returns;
    }

    @PostMapping("/orders/{orderNumber}/returns")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Map<String, Object>> request(Authentication authentication,
            @PathVariable String orderNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReturnRequestPayload payload) {
        return ApiResponse.success("Return request submitted",
                returns.requestReturn(userId(authentication), orderNumber, payload.reason(), idempotencyKey));
    }

    @GetMapping("/orders/{orderNumber}/returns")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<Map<String, Object>>> customerHistory(Authentication authentication,
            @PathVariable String orderNumber) {
        return ApiResponse.success("Return requests retrieved",
                returns.customerRequests(userId(authentication), orderNumber));
    }

    @GetMapping("/staff/returns")
    @PreAuthorize("hasAuthority('PERM_orders.read')")
    public ApiResponse<List<Map<String, Object>>> staffQueue(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Return review queue retrieved", returns.staffQueue(status, page, size));
    }

    @PatchMapping("/staff/returns/{returnId}/decision")
    @PreAuthorize("hasAuthority('PERM_orders.write')")
    public ApiResponse<Map<String, Object>> decide(Authentication authentication,
            @PathVariable @Min(1) long returnId,
            @Valid @RequestBody ReturnDecisionPayload payload) {
        return ApiResponse.success("Return request reviewed",
                returns.decide(returnId, userId(authentication), payload.decision(), payload.note()));
    }

    @PostMapping("/staff/returns/{returnId}/receive")
    @PreAuthorize("hasAuthority('PERM_orders.write')")
    public ApiResponse<Map<String, Object>> receive(Authentication authentication,
            @PathVariable @Min(1) long returnId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return ApiResponse.success("Return receipt recorded",
                returns.markReceived(returnId, userId(authentication), note));
    }

    private static long userId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.longValue();
    }
}
