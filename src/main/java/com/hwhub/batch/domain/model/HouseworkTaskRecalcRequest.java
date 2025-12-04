package com.hwhub.batch.domain.model;

public record HouseworkTaskRecalcRequest(
        long requestId,
        long houseworkId
) {
}
