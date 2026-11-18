package com.luxeperfume.backend.returns.domain;

/** [ABS] Contract for permitted return-request lifecycle transitions. */
public interface ReturnStatusTransitionPolicy {
    boolean canTransition(ReturnStatus current, ReturnStatus next);
}
