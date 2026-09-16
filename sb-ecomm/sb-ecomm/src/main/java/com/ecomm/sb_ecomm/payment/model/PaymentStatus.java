package com.ecomm.sb_ecomm.payment.model;

public enum PaymentStatus {
    INITIATED, PENDING, AUTHORIZED,
    CAPTURED, PAID, FAILED, EXPIRED,
    REFUNDED, PARTIALLY_REFUNDED, VOIDED
}
