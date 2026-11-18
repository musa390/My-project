package com.luxeperfume.backend.returns.domain;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** [POL] Return requests are manually reviewed; received returns do not auto-trigger refunds. */
@Component
public final class DefaultReturnStatusTransitionPolicy implements ReturnStatusTransitionPolicy {
    private static final Map<ReturnStatus, Set<ReturnStatus>> ALLOWED = Map.of(
            ReturnStatus.REQUESTED, Set.of(ReturnStatus.APPROVED, ReturnStatus.REJECTED),
            ReturnStatus.APPROVED, Set.of(ReturnStatus.RECEIVED)
    );

    @Override
    public boolean canTransition(ReturnStatus current, ReturnStatus next) {
        return current != null && next != null
                && ALLOWED.getOrDefault(current, Set.of()).contains(next);
    }
}
