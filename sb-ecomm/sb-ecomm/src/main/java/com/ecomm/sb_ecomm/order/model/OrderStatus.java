package com.ecomm.sb_ecomm.order.model;

public enum OrderStatus {
    PENDING, PROCESSING, READY_FOR_PICKUP,
    SHIPPED, OUT_FOR_DELIVERY, DELIVERED,
    FAILED_ATTEMPT, RETURNED,
    CANCELLED ,CONFIRMED, EXPIRED
}
