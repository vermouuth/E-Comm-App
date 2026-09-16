package com.ecomm.sb_ecomm.validators;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PaymentStatusTransitionValidator {

    private static final Map<PaymentStatus,Set<PaymentStatus>> PAYMENT_STATUS_TRANSITIONS
            = Map.of(
                    PaymentStatus.INITIATED , Set.of(PaymentStatus.PENDING, PaymentStatus.EXPIRED)
            ,       PaymentStatus.PENDING , Set.of(PaymentStatus.AUTHORIZED, PaymentStatus.EXPIRED , PaymentStatus.CAPTURED)
            ,       PaymentStatus.AUTHORIZED , Set.of(PaymentStatus.CAPTURED , PaymentStatus.EXPIRED, PaymentStatus.VOIDED)
            ,       PaymentStatus.CAPTURED , Set.of(PaymentStatus.REFUNDED)

    );

    public void validatePaymentStatus(PaymentStatus current, PaymentStatus next) {
        Set<PaymentStatus> transitions =  PAYMENT_STATUS_TRANSITIONS.getOrDefault(current,Set.of());
         if(!transitions.contains(next))
             throw new ApiException("Invalid payment status transition!.");
    }


}
