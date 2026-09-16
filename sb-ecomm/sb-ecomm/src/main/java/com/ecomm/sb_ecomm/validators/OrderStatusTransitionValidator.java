package com.ecomm.sb_ecomm.validators;

import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,          Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.EXPIRED, OrderStatus.CONFIRMED),
            OrderStatus.PROCESSING,       Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,          Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.FAILED_ATTEMPT),
            OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED, OrderStatus.FAILED_ATTEMPT),
            OrderStatus.DELIVERED,        Set.of(OrderStatus.RETURNED),
            OrderStatus.FAILED_ATTEMPT,   Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED)
    );

    public Boolean validateTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ApiException("Cannot transition order from " + current + " to " + next);
        }

        return true;
    }
}
