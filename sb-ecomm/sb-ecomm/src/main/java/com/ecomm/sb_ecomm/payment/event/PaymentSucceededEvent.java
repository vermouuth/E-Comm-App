package com.ecomm.sb_ecomm.payment.event;

public record PaymentSucceededEvent(
        Long paymentId,
        Long orderId
) {
}
